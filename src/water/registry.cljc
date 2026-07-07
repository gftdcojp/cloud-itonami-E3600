(ns water.registry
  "Pure-function report-publication + alert-suppression record
  construction -- an append-only water-utility book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a report-publication or
  alert-suppression reference number -- every utility/jurisdiction
  assigns its own reference format. This namespace does NOT invent
  one; it builds a jurisdiction-scoped sequence number and validates
  the record's required fields, the same honest, non-fabricating
  discipline `water.facts` uses.

  `contaminant-level-out-of-range?` is the THIRD instance of this
  fleet's two-sided range check family (`testlab.registry/within-
  tolerance?` established the first, `conservation.registry/body-
  condition-out-of-range?` the second), applying the SAME lo/hi
  bounds-comparison shape to a site's own measured contaminant level
  against the site's own recorded safe range.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real water-utility SCADA/telemetry system. It builds the
  RECORD a utility would keep, not the act of publishing the report or
  suppressing the alert itself (that is `water.operation`'s `:report/
  publish`/`:alert/suppress`, always human-gated -- see README
  `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  utility's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn contaminant-level-out-of-range?
  "Does `site`'s own `:contaminant-level` fall outside its own
  `[:contaminant-min :contaminant-max]` safe-range bounds? A pure
  ground-truth check against the site's own permanent fields -- no
  upstream comparison needed. The THIRD instance of this fleet's two-
  sided range check family (see ns docstring)."
  [{:keys [contaminant-level contaminant-min contaminant-max]}]
  (and (number? contaminant-level) (number? contaminant-min) (number? contaminant-max)
       (or (< contaminant-level contaminant-min) (> contaminant-level contaminant-max))))

(defn register-report-publication
  "Validate + construct the REPORT-PUBLICATION registration DRAFT --
  the utility's own legal act of publishing a real public water-
  quality report for a site. Pure function -- does not touch any real
  water-utility system; it builds the RECORD a utility would keep.
  `water.governor` independently re-verifies the site's own
  contaminant-level sufficiency against its own safe-range bounds, and
  blocks a double-publication for the same site, before this is ever
  allowed to commit."
  [site-id jurisdiction sequence]
  (when-not (and site-id (not= site-id ""))
    (throw (ex-info "report-publication: site_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "report-publication: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "report-publication: sequence must be >= 0" {})))
  (let [report-number (str (str/upper-case jurisdiction) "-RPT-" (zero-pad sequence 6))
        record {"record_id" report-number
                "kind" "report-publication-draft"
                "site_id" site-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "report_number" report-number
     "certificate" (unsigned-certificate "ReportPublication" report-number report-number)}))

(defn register-alert-suppression
  "Validate + construct the ALERT-SUPPRESSION registration DRAFT --
  the utility's own legal act of suppressing a real triggered safety
  alert for a site. Pure function -- does not touch any real water-
  utility system; it builds the RECORD a utility would keep. `water.
  governor` independently re-verifies the site's own threshold-breach
  resolution status, and blocks a double-suppression for the same
  site, before this is ever allowed to commit. Unlike every other
  actuation this fleet has modeled, this actuation is a NEGATIVE act
  (withholding/silencing a notification), not a positive one (issuing
  a record) -- see README `Actuation` and this actor's own ADR-0001
  Decision 1 for the honest framing this makes."
  [site-id jurisdiction sequence]
  (when-not (and site-id (not= site-id ""))
    (throw (ex-info "alert-suppression: site_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "alert-suppression: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "alert-suppression: sequence must be >= 0" {})))
  (let [suppression-number (str (str/upper-case jurisdiction) "-SUP-" (zero-pad sequence 6))
        record {"record_id" suppression-number
                "kind" "alert-suppression-draft"
                "site_id" site-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "suppression_number" suppression-number
     "certificate" (unsigned-certificate "AlertSuppression" suppression-number suppression-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
