(ns water.governor-contract-test
  "The governor contract as executable tests -- the water-utility
  analog of `cloud-itonami-isic-6512`'s `casualty.governor-contract-
  test`. The single invariant under test:

    Water Advisor never publishes a report or suppresses an alert the
    Water Safety Governor would reject, `:report/publish`/`:alert/
    suppress` NEVER auto-commit at any phase, `:site/intake` (no
    direct capital risk) MAY auto-commit when clean, and every
    decision (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [water.store :as store]
            [water.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :utility-officer :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- screen!
  "Walks `subject` through threshold-breach screening -> approve,
  leaving a screening on file. Only safe to call for a site whose
  breach status has already resolved -- an unresolved breach HARD-
  holds the screen itself (see
  `threshold-breach-is-held-and-unoverridable`)."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-screen") {:op :threshold/screen :subject subject} operator)
  (approve! actor (str tid-prefix "-screen")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :site/intake :subject "site-1"
                   :patch {:id "site-1" :site-name "Sakura Community Well"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sakura Community Well" (:site-name (store/site db "site-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "site-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "site-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "site-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "site-1")) "no assessment written"))))

(deftest report-publish-without-assessment-is-held
  (testing "report/publish before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :report/publish :subject "site-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest contaminant-level-out-of-range-is-held
  (testing "a site whose contaminant level falls outside its own safe-range bounds -> HOLD"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "site-3")
          res (exec-op actor "t5" {:op :report/publish :subject "site-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:contaminant-level-out-of-range} (-> (store/ledger db) last :basis)))
      (is (empty? (store/report-history db))))))

(deftest threshold-breach-is-held-and-unoverridable
  (testing "an unresolved threshold breach on a site -> HOLD, and never reaches request-approval -- exercised via :threshold/screen DIRECTLY, not via the actuation op against an unscreened site (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's and card's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :threshold/screen :subject "site-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:threshold-breach-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/threshold-screen-of db "site-4")) "no clearance written"))))

(deftest report-publish-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, in-range site still ALWAYS interrupts for human approval -- actuation/publish-report is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "site-1")
          r1 (exec-op actor "t7" {:op :report/publish :subject "site-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, report record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:report-published? (store/site db "site-1"))))
          (is (= 1 (count (store/report-history db))) "one draft report record"))))))

(deftest alert-suppress-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, resolved-breach site still ALWAYS interrupts for human approval -- actuation/suppress-alert is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "site-1")
          _ (screen! actor "t8pre2" "site-1")
          r1 (exec-op actor "t8" {:op :alert/suppress :subject "site-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, alert-suppression record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:alert-suppressed? (store/site db "site-1"))))
          (is (= 1 (count (store/suppression-history db))) "one draft suppression record"))))))

(deftest report-publish-double-publication-is-held
  (testing "publishing the same site's report twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "site-1")
          _ (exec-op actor "t9a" {:op :report/publish :subject "site-1"} operator)
          _ (approve! actor "t9a")
          res (exec-op actor "t9" {:op :report/publish :subject "site-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-published} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/report-history db))) "still only the one earlier report"))))

(deftest alert-suppress-double-suppression-is-held
  (testing "suppressing the same site's alert twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "site-1")
          _ (screen! actor "t10pre2" "site-1")
          _ (exec-op actor "t10a" {:op :alert/suppress :subject "site-1"} operator)
          _ (approve! actor "t10a")
          res (exec-op actor "t10" {:op :alert/suppress :subject "site-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-suppressed} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/suppression-history db))) "still only the one earlier suppression"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :site/intake :subject "site-1"
                          :patch {:id "site-1" :site-name "Sakura Community Well"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "site-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
