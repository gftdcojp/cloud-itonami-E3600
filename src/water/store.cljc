(ns water.store
  "SSoT for the water actor, behind a `Store` protocol so the backend
  is a swap, not a rewrite -- the same seam every prior `cloud-
  itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/water/store_contract_test.clj), which is the whole point: the
  actor, the Water Safety Governor and the audit ledger never know
  which SSoT they run on.

  Like `hospital.store`'s dual treatment/discharge history and every
  other dual-actuation sibling before it, this actor has TWO actuation
  events (publishing a report, suppressing an alert) acting on the
  SAME entity (a site), each with its OWN history collection, sequence
  counter and dedicated double-actuation-guard boolean
  (`:report-published?`/`:alert-suppressed?`, never a `:status`
  value) -- the same discipline every prior sibling governor's guards
  establish, informed by `cloud-itonami-isic-6492`'s status-lifecycle
  bug (ADR-2607071320).

  The ledger stays append-only on every backend: 'which site was
  screened for an unresolved threshold breach, which report was
  published, which alert was suppressed, on what jurisdictional basis,
  approved by whom' is always a query over an immutable log -- the
  audit trail a community trusting a water utility needs, and the
  evidence an operator needs if a report or alert-suppression decision
  is later disputed."
  (:require [water.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (site [s id])
  (all-sites [s])
  (threshold-screen-of [s site-id] "committed threshold-breach screening verdict for a site, or nil")
  (assessment-of [s site-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (report-history [s] "the append-only report-publication history (water.registry drafts)")
  (suppression-history [s] "the append-only alert-suppression history (water.registry drafts)")
  (next-report-sequence [s jurisdiction] "next report-number sequence for a jurisdiction")
  (next-suppression-sequence [s jurisdiction] "next suppression-number sequence for a jurisdiction")
  (site-already-published? [s site-id] "has this site's report already been published?")
  (site-already-suppressed? [s site-id] "has this site's alert already been suppressed?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-sites [s sites] "replace/seed the site directory (map id->site)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained site set covering both actuation lifecycles
  (publishing a report, suppressing an alert) so the actor + tests run
  offline."
  []
  {:sites
   {"site-1" {:id "site-1" :site-name "Sakura Community Well"
              :contaminant-level 2.0 :contaminant-min 0.0 :contaminant-max 4.0
              :threshold-breach-unresolved? false
              :report-published? false :alert-suppressed? false
              :jurisdiction "JPN" :status :intake}
    "site-2" {:id "site-2" :site-name "Atlantis Reservoir"
              :contaminant-level 2.0 :contaminant-min 0.0 :contaminant-max 4.0
              :threshold-breach-unresolved? false
              :report-published? false :alert-suppressed? false
              :jurisdiction "ATL" :status :intake}
    "site-3" {:id "site-3" :site-name "鈴木浄水場"
              :contaminant-level 6.0 :contaminant-min 0.0 :contaminant-max 4.0
              :threshold-breach-unresolved? false
              :report-published? false :alert-suppressed? false
              :jurisdiction "JPN" :status :intake}
    "site-4" {:id "site-4" :site-name "田中給水所"
              :contaminant-level 2.0 :contaminant-min 0.0 :contaminant-max 4.0
              :threshold-breach-unresolved? true
              :report-published? false :alert-suppressed? false
              :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- publish-report!
  "Backend-agnostic `:site/mark-published` -- looks up the site via
  the protocol and drafts the report-publication record, and returns
  {:result .. :site-patch ..} for the caller to persist."
  [s site-id]
  (let [st (site s site-id)
        seq-n (next-report-sequence s (:jurisdiction st))
        result (registry/register-report-publication site-id (:jurisdiction st) seq-n)]
    {:result result
     :site-patch {:report-published? true
                 :report-number (get result "report_number")}}))

(defn- suppress-alert!
  "Backend-agnostic `:site/mark-suppressed` -- looks up the site via
  the protocol and drafts the alert-suppression record, and returns
  {:result .. :site-patch ..} for the caller to persist."
  [s site-id]
  (let [st (site s site-id)
        seq-n (next-suppression-sequence s (:jurisdiction st))
        result (registry/register-alert-suppression site-id (:jurisdiction st) seq-n)]
    {:result result
     :site-patch {:alert-suppressed? true
                 :suppression-number (get result "suppression_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (site [_ id] (get-in @a [:sites id]))
  (all-sites [_] (sort-by :id (vals (:sites @a))))
  (threshold-screen-of [_ id] (get-in @a [:threshold-screens id]))
  (assessment-of [_ site-id] (get-in @a [:assessments site-id]))
  (ledger [_] (:ledger @a))
  (report-history [_] (:reports @a))
  (suppression-history [_] (:suppressions @a))
  (next-report-sequence [_ jurisdiction] (get-in @a [:report-sequences jurisdiction] 0))
  (next-suppression-sequence [_ jurisdiction] (get-in @a [:suppression-sequences jurisdiction] 0))
  (site-already-published? [_ site-id] (boolean (get-in @a [:sites site-id :report-published?])))
  (site-already-suppressed? [_ site-id] (boolean (get-in @a [:sites site-id :alert-suppressed?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :site/upsert
      (swap! a update-in [:sites (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :threshold-screen/set
      (swap! a assoc-in [:threshold-screens (first path)] payload)

      :site/mark-published
      (let [site-id (first path)
            {:keys [result site-patch]} (publish-report! s site-id)
            jurisdiction (:jurisdiction (site s site-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:report-sequences jurisdiction] (fnil inc 0))
                       (update-in [:sites site-id] merge site-patch)
                       (update :reports registry/append result))))
        result)

      :site/mark-suppressed
      (let [site-id (first path)
            {:keys [result site-patch]} (suppress-alert! s site-id)
            jurisdiction (:jurisdiction (site s site-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:suppression-sequences jurisdiction] (fnil inc 0))
                       (update-in [:sites site-id] merge site-patch)
                       (update :suppressions registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-sites [s sites] (when (seq sites) (swap! a assoc :sites sites)) s))

(defn seed-db
  "A MemStore seeded with the demo site set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {} :threshold-screens {} :ledger [] :report-sequences {}
                           :reports [] :suppression-sequences {} :suppressions []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (assessment/threshold-screen payloads, ledger
  facts, report/suppression records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:site/id                          {:db/unique :db.unique/identity}
   :assessment/site-id               {:db/unique :db.unique/identity}
   :threshold-screen/site-id         {:db/unique :db.unique/identity}
   :ledger/seq                       {:db/unique :db.unique/identity}
   :report/seq                       {:db/unique :db.unique/identity}
   :suppression/seq                  {:db/unique :db.unique/identity}
   :report-sequence/jurisdiction     {:db/unique :db.unique/identity}
   :suppression-sequence/jurisdiction {:db/unique :db.unique/identity}})

(defn- site->tx [{:keys [id site-name contaminant-level contaminant-min contaminant-max
                         threshold-breach-unresolved?
                         report-published? alert-suppressed?
                         jurisdiction status report-number suppression-number]}]
  (cond-> {:site/id id}
    site-name                           (assoc :site/site-name site-name)
    contaminant-level                    (assoc :site/contaminant-level contaminant-level)
    contaminant-min                      (assoc :site/contaminant-min contaminant-min)
    contaminant-max                      (assoc :site/contaminant-max contaminant-max)
    (some? threshold-breach-unresolved?) (assoc :site/threshold-breach-unresolved? threshold-breach-unresolved?)
    (some? report-published?)           (assoc :site/report-published? report-published?)
    (some? alert-suppressed?)           (assoc :site/alert-suppressed? alert-suppressed?)
    jurisdiction                        (assoc :site/jurisdiction jurisdiction)
    status                              (assoc :site/status status)
    report-number                       (assoc :site/report-number report-number)
    suppression-number                  (assoc :site/suppression-number suppression-number)))

(def ^:private site-pull
  [:site/id :site/site-name :site/contaminant-level :site/contaminant-min :site/contaminant-max
   :site/threshold-breach-unresolved? :site/report-published? :site/alert-suppressed?
   :site/jurisdiction :site/status :site/report-number :site/suppression-number])

(defn- pull->site [m]
  (when (:site/id m)
    {:id (:site/id m) :site-name (:site/site-name m)
     :contaminant-level (:site/contaminant-level m)
     :contaminant-min (:site/contaminant-min m)
     :contaminant-max (:site/contaminant-max m)
     :threshold-breach-unresolved? (boolean (:site/threshold-breach-unresolved? m))
     :report-published? (boolean (:site/report-published? m))
     :alert-suppressed? (boolean (:site/alert-suppressed? m))
     :jurisdiction (:site/jurisdiction m) :status (:site/status m)
     :report-number (:site/report-number m) :suppression-number (:site/suppression-number m)}))

(defrecord DatomicStore [conn]
  Store
  (site [_ id]
    (pull->site (d/pull (d/db conn) site-pull [:site/id id])))
  (all-sites [_]
    (->> (d/q '[:find [?id ...] :where [?e :site/id ?id]] (d/db conn))
         (map #(pull->site (d/pull (d/db conn) site-pull [:site/id %])))
         (sort-by :id)))
  (threshold-screen-of [_ id]
    (ls/dec* (d/q '[:find ?p . :in $ ?sid
                :where [?k :threshold-screen/site-id ?sid] [?k :threshold-screen/payload ?p]]
              (d/db conn) id)))
  (assessment-of [_ site-id]
    (ls/dec* (d/q '[:find ?p . :in $ ?sid
                :where [?a :assessment/site-id ?sid] [?a :assessment/payload ?p]]
              (d/db conn) site-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp ls/dec* second))))
  (report-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :report/seq ?s] [?e :report/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp ls/dec* second))))
  (suppression-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :suppression/seq ?s] [?e :suppression/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp ls/dec* second))))
  (next-report-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :report-sequence/jurisdiction ?j] [?e :report-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-suppression-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :suppression-sequence/jurisdiction ?j] [?e :suppression-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (site-already-published? [s site-id]
    (boolean (:report-published? (site s site-id))))
  (site-already-suppressed? [s site-id]
    (boolean (:alert-suppressed? (site s site-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :site/upsert
      (d/transact! conn [(site->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/site-id (first path) :assessment/payload (ls/enc payload)}])

      :threshold-screen/set
      (d/transact! conn [{:threshold-screen/site-id (first path) :threshold-screen/payload (ls/enc payload)}])

      :site/mark-published
      (let [site-id (first path)
            {:keys [result site-patch]} (publish-report! s site-id)
            jurisdiction (:jurisdiction (site s site-id))
            next-n (inc (next-report-sequence s jurisdiction))]
        (d/transact! conn
                     [(site->tx (assoc site-patch :id site-id))
                      {:report-sequence/jurisdiction jurisdiction :report-sequence/next next-n}
                      {:report/seq (count (report-history s)) :report/record (ls/enc (get result "record"))}])
        result)

      :site/mark-suppressed
      (let [site-id (first path)
            {:keys [result site-patch]} (suppress-alert! s site-id)
            jurisdiction (:jurisdiction (site s site-id))
            next-n (inc (next-suppression-sequence s jurisdiction))]
        (d/transact! conn
                     [(site->tx (assoc site-patch :id site-id))
                      {:suppression-sequence/jurisdiction jurisdiction :suppression-sequence/next next-n}
                      {:suppression/seq (count (suppression-history s)) :suppression/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (ls/enc fact)}])
    fact)
  (with-sites [s sites]
    (when (seq sites) (d/transact! conn (mapv site->tx (vals sites)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:sites ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [sites]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-sites s sites))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo site set -- the Datomic-backed
  analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
