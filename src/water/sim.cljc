(ns water.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean site through
  intake -> jurisdiction assessment -> threshold-breach screening ->
  report-publication proposal (always escalates) -> human approval ->
  commit, then through alert-suppression proposal (always escalates)
  -> human approval -> commit, then shows four HARD holds (a
  jurisdiction with no spec-basis, an out-of-range contaminant level,
  an unresolved threshold breach screened directly via `:threshold/
  screen` [never via an actuation op against an unscreened site -- see
  this actor's own governor ns docstring / the lesson `parksafety`'s
  ADR-2607071922 Decision 5, `eldercare`'s, `museum`'s,
  `conservation`'s, `salon`'s, `entertainment`'s, `casework`'s,
  `hospital`'s, `facility`'s, `school`'s, `association`'s, `leasing`'s,
  `behavioral`'s, `secondary`'s and `card`'s ADR-0001s already
  recorded], and a double report-publication/alert-suppression of an
  already-processed site) that never reach a human at all, and prints
  the audit ledger + the draft report-publication and alert-
  suppression records."
  (:require [langgraph.graph :as g]
            [water.store :as store]
            [water.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :utility-officer :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== site/intake site-1 (JPN, clean; contaminant within safe range, no threshold breach) ==")
    (println (exec! actor "t1" {:op :site/intake :subject "site-1"
                                :patch {:id "site-1" :site-name "Sakura Community Well"}} operator))

    (println "== jurisdiction/assess site-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :jurisdiction/assess :subject "site-1"} operator))
    (println (approve! actor "t2"))

    (println "== threshold/screen site-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :threshold/screen :subject "site-1"} operator))
    (println (approve! actor "t3"))

    (println "== report/publish site-1 (always escalates -- actuation/publish-report) ==")
    (let [r (exec! actor "t4" {:op :report/publish :subject "site-1"} operator)]
      (println r)
      (println "-- human utility officer approves --")
      (println (approve! actor "t4")))

    (println "== alert/suppress site-1 (always escalates -- actuation/suppress-alert) ==")
    (let [r (exec! actor "t5" {:op :alert/suppress :subject "site-1"} operator)]
      (println r)
      (println "-- human utility officer approves --")
      (println (approve! actor "t5")))

    (println "== jurisdiction/assess site-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t6" {:op :jurisdiction/assess :subject "site-2" :no-spec? true} operator))

    (println "== jurisdiction/assess site-3 (escalates -- human approves; sets up the out-of-range test) ==")
    (println (exec! actor "t7" {:op :jurisdiction/assess :subject "site-3"} operator))
    (println (approve! actor "t7"))

    (println "== report/publish site-3 (6.0 outside [0.0,4.0] range -> HARD hold) ==")
    (println (exec! actor "t8" {:op :report/publish :subject "site-3"} operator))

    (println "== threshold/screen site-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t9" {:op :threshold/screen :subject "site-4"} operator))

    (println "== report/publish site-1 AGAIN (double-publication -> HARD hold) ==")
    (println (exec! actor "t10" {:op :report/publish :subject "site-1"} operator))

    (println "== alert/suppress site-1 AGAIN (double-suppression -> HARD hold) ==")
    (println (exec! actor "t11" {:op :alert/suppress :subject "site-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft report-publication records ==")
    (doseq [r (store/report-history db)] (println r))

    (println "== draft alert-suppression records ==")
    (doseq [r (store/suppression-history db)] (println r))))
