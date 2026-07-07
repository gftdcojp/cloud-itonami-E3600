(ns water.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:report/publish`/`:alert/suppress` must NEVER be a
  member of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [water.phase :as phase]))

(deftest report-publish-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real report publication"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :report/publish))
          (str "phase " n " must not auto-commit :report/publish")))))

(deftest alert-suppress-never-auto-at-any-phase
  (testing "structural invariant: no phase auto-commits a real alert suppression"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :alert/suppress))
          (str "phase " n " must not auto-commit :alert/suppress")))))

(deftest threshold-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :threshold/screen))
          (str "phase " n " must not auto-commit :threshold/screen")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":site/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:site/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :site/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :report/publish} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :alert/suppress} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :site/intake} :commit)))))
