(ns water.render-html
  "Build-time HTML renderer for docs/samples/operator-console.html.
  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300).
  Drives the REAL actor stack (water.operation -> water.governor ->
  water.store) through the SAME scenario `water.sim` already proves
  trustworthy (real seed-db ids, real op keywords, real HARD-hold
  rules) -- no invented numbers, no timestamps, byte-identical across
  reruns.

  Replaces the placeholder `docs/samples/operator-console.html` that
  was hand-typed boilerplate copied from an unrelated robotics-mission
  console (title 'cloud-itonami · robotics', rows 'M1'/'robot-1') and
  never adapted to this repo's own water-utility domain."
  (:require [clojure.string :as str]
            [water.store :as store]
            [water.operation :as op]
            [water.phase :as phase]
            [water.governor :as governor]
            [langgraph.graph :as g]))

(def ^:private operator
  {:actor-id "op-1" :actor-role :utility-officer :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- run-op!
  "Executes one request, appends {:tid :op :subject :label :result} to
  the atom `log*` and returns the run* result."
  [log* actor tid label request]
  (let [r (exec! actor tid request)]
    (swap! log* conj {:tid tid :op (:op request) :subject (:subject request)
                      :label label :result r})
    r))

(defn- approve-op!
  "Resumes an interrupted op and OVERWRITES that tid's :result with the
  final (post-approval) run* result, so the operations log shows one
  row per real-world business operation, not one row per graph
  superstep."
  [log* actor tid]
  (let [r (approve! actor tid)]
    (swap! log* (fn [v] (mapv (fn [m] (if (= tid (:tid m)) (assoc m :result r) m)) v)))
    r))

(defn run-demo!
  "Drives the real OperationActor StateGraph through the water.sim
  scenario (verified against real seed-db ids + real governor rules
  first, and found trustworthy -- every id/op below is copied
  verbatim from `water.sim`, which is itself pinned to
  `water.store/demo-data` and `water.governor`'s actual checks).
  Exercises:
    (a) a phase-3 auto-commit          -- :site/intake site-1 (the
                                          only op in phase 3's :auto
                                          set)
    (b) always-escalate + human approve -- :report/publish and
                                          :alert/suppress (governor
                                          `high-stakes`), each
                                          followed by `approve-op!`
    (c) five distinct real HARD-hold reasons -- :no-spec-basis,
                                          :contaminant-level-out-of-
                                          range, :threshold-breach-
                                          unresolved, :already-
                                          published, :already-
                                          suppressed
  Returns {:db .. :log [..]}."
  []
  (let [db (store/seed-db)
        actor (op/build db)
        log* (atom [])]

    (run-op! log* actor "t1"
             "site/intake site-1 -- phase-3 auto-eligible, governor-clean"
             {:op :site/intake :subject "site-1"
              :patch {:id "site-1" :site-name "Sakura Community Well"}})

    (run-op! log* actor "t2"
             "jurisdiction/assess site-1 -- escalates (phase-approval)"
             {:op :jurisdiction/assess :subject "site-1"})
    (approve-op! log* actor "t2")

    (run-op! log* actor "t3"
             "threshold/screen site-1 -- clean; escalates (phase-approval)"
             {:op :threshold/screen :subject "site-1"})
    (approve-op! log* actor "t3")

    (run-op! log* actor "t4"
             "report/publish site-1 -- always escalates (actuation/publish-report)"
             {:op :report/publish :subject "site-1"})
    (approve-op! log* actor "t4")

    (run-op! log* actor "t5"
             "alert/suppress site-1 -- always escalates (actuation/suppress-alert)"
             {:op :alert/suppress :subject "site-1"})
    (approve-op! log* actor "t5")

    (run-op! log* actor "t6"
             "jurisdiction/assess site-2 -- no spec-basis -> HARD hold"
             {:op :jurisdiction/assess :subject "site-2" :no-spec? true})

    (run-op! log* actor "t7"
             "jurisdiction/assess site-3 -- escalates (sets up the out-of-range case)"
             {:op :jurisdiction/assess :subject "site-3"})
    (approve-op! log* actor "t7")

    (run-op! log* actor "t8"
             "report/publish site-3 -- 6.0 outside [0.0,4.0] -> HARD hold"
             {:op :report/publish :subject "site-3"})

    (run-op! log* actor "t9"
             "threshold/screen site-4 -- unresolved breach -> HARD hold"
             {:op :threshold/screen :subject "site-4"})

    (run-op! log* actor "t10"
             "report/publish site-1 AGAIN -- double-publication -> HARD hold"
             {:op :report/publish :subject "site-1"})

    (run-op! log* actor "t11"
             "alert/suppress site-1 AGAIN -- double-suppression -> HARD hold"
             {:op :alert/suppress :subject "site-1"})

    {:db db :log @log*}))

;; ----------------------------- render helpers -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- qname
  "Fully-qualified keyword text (`site/intake`, not just `intake`) --
  ops in this domain are namespaced (`:site/intake`, `:report/publish`,
  `:actuation/publish-report`, ...) and stripping the namespace with
  plain `name` would make distinct ops read identically."
  [kw]
  (subs (str kw) 1))

(defn- final-state [entry] (:state (:result entry)))
(defn- audit-of [entry] (:audit (final-state entry)))

(defn- hold-fact-of [entry]
  (last (filter #(#{:governor-hold :approval-rejected} (:t %)) (audit-of entry))))

(defn- approval-granted? [entry]
  (boolean (some #(= :approval-granted (:t %)) (audit-of entry))))

(defn- proposal-trace [entry]
  (first (filter #(= :wateradvisor-proposal (:t %)) (audit-of entry))))

(defn- outcome-cell
  "[css-class label] for one operations-log row, derived ONLY from the
  real run* result (disposition + audit trail) -- never invented."
  [entry]
  (let [disp (:disposition (final-state entry))]
    (cond
      (= :hold disp)
      (let [hf (hold-fact-of entry)]
        ["err" (str "HARD hold: " (str/join ", " (map name (:basis hf))))])

      (and (= :commit disp) (approval-granted? entry))
      ["ok" "escalated -> human approved -> committed"]

      (= :commit disp)
      ["ok" "auto-committed (phase 3, governor-clean)"]

      :else
      ["warn" (str disp)])))

(defn- ops-log-rows [log]
  (str/join
   "\n"
   (for [entry log]
     (let [[cls label] (outcome-cell entry)
           trace (proposal-trace entry)]
       (str "<tr><td>" (esc (:tid entry)) "</td>"
            "<td><code>" (esc (qname (:op entry))) "</code></td>"
            "<td>" (esc (:subject entry)) "</td>"
            "<td>" (esc (:label entry)) "</td>"
            "<td><span class=\"" cls "\">" (esc label) "</span></td>"
            "<td class=\"muted\">" (esc (:summary trace)) "</td></tr>")))))

(defn- site-rows [sites]
  (str/join
   "\n"
   (for [s sites]
     (str "<tr><td>" (esc (:id s)) "</td>"
          "<td>" (esc (:site-name s)) "</td>"
          "<td>" (esc (:jurisdiction s)) "</td>"
          "<td class=\"amt\">" (esc (:contaminant-level s)) "</td>"
          "<td class=\"amt\">[" (esc (:contaminant-min s)) "," (esc (:contaminant-max s)) "]</td>"
          "<td>" (if (:threshold-breach-unresolved? s)
                   "<span class=\"err\">unresolved</span>"
                   "<span class=\"ok\">none on file</span>") "</td>"
          "<td>" (if (:report-published? s)
                   (str "<span class=\"ok\">" (esc (:report-number s)) "</span>")
                   "<span class=\"muted\">not published</span>") "</td>"
          "<td>" (if (:alert-suppressed? s)
                   (str "<span class=\"ok\">" (esc (:suppression-number s)) "</span>")
                   "<span class=\"muted\">not suppressed</span>") "</td></tr>"))))

(defn- record-rows [records id-key]
  (str/join
   "\n"
   (for [r records]
     (str "<tr><td><code>" (esc (get r id-key)) "</code></td>"
          "<td>" (esc (get r "site_id")) "</td>"
          "<td>" (esc (get r "jurisdiction")) "</td>"
          "<td>" (esc (get r "kind")) "</td></tr>"))))

(defn- phase-rows []
  (str/join
   "\n"
   (for [[n {:keys [label writes auto]}] (sort-by key phase/phases)]
     (str "<tr><td>" n "</td><td>" (esc label) "</td>"
          "<td class=\"muted\">" (esc (str/join ", " (map qname (sort writes)))) "</td>"
          "<td>" (if (seq auto)
                   (str "<span class=\"ok\">" (esc (str/join ", " (map qname (sort auto)))) "</span>")
                   "<span class=\"muted\">none</span>") "</td></tr>"))))

(defn- high-stakes-rows []
  (str/join
   "\n"
   (for [op (sort governor/high-stakes)]
     (str "<tr><td><code>" (esc (qname op)) "</code></td>"
          "<td><span class=\"critical\">always escalates, every phase</span></td></tr>"))))

(defn- ledger-rows [ledger]
  (str/join
   "\n"
   (for [fact ledger]
     (str "<tr><td>"
          (if (= :committed (:t fact)) "<span class=\"ok\">committed</span>"
              "<span class=\"err\">governor-hold</span>") "</td>"
          "<td><code>" (esc (qname (:op fact))) "</code></td>"
          "<td>" (esc (:subject fact)) "</td>"
          "<td class=\"muted\">" (esc (pr-str (or (:basis fact) []))) "</td></tr>"))))

(def ^:private css
  "table { width: 100%; border-collapse: collapse; font-size: 14px; }
.ok { color: #137a3f; }
body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }
header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }
th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }
h2 { margin-top: 0; font-size: 15px; }
td.amt { font-variant-numeric: tabular-nums; text-align: right; }
.warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }
main { max-width: 1180px; margin: 24px auto; padding: 0 20px; }
header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }
.muted { color: #888; font-size: 13px; }
.critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }
.card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; overflow-x: auto; }
.err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }
th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }
header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }
code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }")

(defn render [{:keys [db log]}]
  (let [sites (store/all-sites db)
        reports (store/report-history db)
        suppressions (store/suppression-history db)
        ledger (store/ledger db)]
    (str
     "<!doctype html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n"
     "<title>water.render-html -- Water Utility Governor operator console</title>\n"
     "<style>\n" css "\n</style>\n</head>\n<body>\n"
     "<header class=\"bar\"><h1>Water Utility Governor -- Operator Console</h1>"
     "<span class=\"badge\">ISIC 3600 &middot; water collection, treatment and supply &middot; phase "
     (:phase operator) "</span></header>\n<main>\n"

     "<section class=\"card\"><h2>Operations log</h2>\n"
     "<p class=\"muted\">Every row is a real `langgraph-clj` StateGraph run of "
     "<code>water.operation</code> against a seeded <code>water.store/MemStore</code> -- "
     "the Water Advisor proposes, the Water Safety Governor censors, the phase gate "
     "adjusts, a human operator approves where required. No row is hand-typed.</p>\n"
     "<table><thead><tr><th>Thread</th><th>Op</th><th>Subject</th><th>Scenario</th>"
     "<th>Outcome</th><th>Advisor rationale</th></tr></thead><tbody>\n"
     (ops-log-rows log)
     "\n</tbody></table></section>\n"

     "<section class=\"card\"><h2>Site directory</h2>\n"
     "<table><thead><tr><th>Site</th><th>Name</th><th>Jurisdiction</th>"
     "<th>Contaminant level</th><th>Safe range</th><th>Threshold breach</th>"
     "<th>Report</th><th>Alert suppression</th></tr></thead><tbody>\n"
     (site-rows sites)
     "\n</tbody></table></section>\n"

     "<section class=\"card\"><h2>Draft report-publication records</h2>\n"
     "<table><thead><tr><th>Report #</th><th>Site</th><th>Jurisdiction</th>"
     "<th>Kind</th></tr></thead><tbody>\n"
     (record-rows reports "record_id")
     "\n</tbody></table></section>\n"

     "<section class=\"card\"><h2>Draft alert-suppression records</h2>\n"
     "<table><thead><tr><th>Suppression #</th><th>Site</th><th>Jurisdiction</th>"
     "<th>Kind</th></tr></thead><tbody>\n"
     (record-rows suppressions "record_id")
     "\n</tbody></table></section>\n"

     "<section class=\"card\"><h2>Rollout phase gate</h2>\n"
     "<p class=\"muted\">`water.phase/phases` -- the write/auto-commit envelope per phase, "
     "as compiled into the actor. `:report/publish`/`:alert/suppress` never appear in any "
     "phase's auto set -- see `water.phase` docstring.</p>\n"
     "<table><thead><tr><th>Phase</th><th>Label</th><th>Writes allowed</th>"
     "<th>Auto-commit eligible</th></tr></thead><tbody>\n"
     (phase-rows)
     "\n</tbody></table>\n"
     "<h2>Always-escalate (governor high-stakes)</h2>\n"
     "<table><thead><tr><th>Op</th><th>Gate</th></tr></thead><tbody>\n"
     (high-stakes-rows)
     "\n</tbody></table></section>\n"

     "<section class=\"card\"><h2>Audit ledger</h2>\n"
     "<p class=\"muted\">Append-only. `store/ledger` verbatim -- one row per committed or "
     "governor-held operation, in commit order.</p>\n"
     "<table><thead><tr><th>Disposition</th><th>Op</th><th>Subject</th>"
     "<th>Basis</th></tr></thead><tbody>\n"
     (ledger-rows ledger)
     "\n</tbody></table></section>\n"

     "</main>\n</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        demo (run-demo!)
        html (render demo)]
    (spit out html)
    (println "wrote" out)))
