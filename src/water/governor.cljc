(ns water.governor
  "Water Safety Governor -- the independent compliance layer that
  earns the Water Advisor the right to commit. The LLM has no notion
  of jurisdictional drinking-water-safety law, whether a site's own
  measured contaminant level actually stays within its own recorded
  safe-range bounds, whether a threshold breach against the site has
  actually stayed unresolved, or when an act stops being a draft and
  becomes a real-world report publication or alert suppression, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD -- the water-utility analog of `cloud-itonami-isic-
  6512`'s CasualtyGovernor.

  Six checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them (you don't get to approve your way past a
  fabricated jurisdiction spec-basis, incomplete evidence, an out-of-
  range contaminant level, an unresolved threshold breach, or a double
  report-publication/alert-suppression). The confidence/actuation gate
  is SOFT: it asks a human to look (low confidence / actuation), and
  the human may approve -- but see `water.phase`: for `:stake
  :actuation/publish-report`/`:actuation/suppress-alert` (a real
  public-safety act) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source (`water.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:report/publish`/`:alert/
                                       suppress`, has the jurisdiction
                                       actually been assessed with a
                                       full site-intake/sensor-
                                       calibration-provenance/lab-
                                       result-chain-of-custody/public-
                                       reporting-source evidence
                                       checklist on file?
    3. Contaminant level out of
       range                         -- for `:report/publish`,
                                       INDEPENDENTLY recompute whether
                                       the site's own measured
                                       contaminant level falls outside
                                       its own recorded safe-range
                                       bounds (`water.registry/
                                       contaminant-level-out-of-
                                       range?`) -- needs no proposal
                                       inspection or stored-verdict
                                       lookup at all. The THIRD
                                       instance of this fleet's two-
                                       sided range check family
                                       (`testlab.governor/within-
                                       tolerance-violations`/
                                       `conservation.governor/body-
                                       condition-out-of-range-
                                       violations` established the
                                       first two).
    4. Threshold breach unresolved  -- reported by THIS proposal itself
                                       (a `:threshold/screen` that just
                                       found an unresolved breach), or
                                       already on file for the site
                                       (`:threshold/screen`/`:alert/
                                       suppress`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/...
                                       (twenty-four prior siblings)...
                                       established -- the TWENTY-FIFTH
                                       distinct application of this
                                       exact discipline, and the FIRST
                                       specifically for a threshold-
                                       breach concept. Like the
                                       fourteen most recent siblings'
                                       equivalent checks, this is
                                       exercised in tests/demo via
                                       `:threshold/screen` DIRECTLY, not
                                       via an actuation op against an
                                       unscreened site -- see this ns's
                                       own test suite.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:report/publish`/
                                       `:alert/suppress` (REAL public-
                                       safety acts) -> escalate.

  Two more guards, double-publication/double-suppression prevention,
  are enforced but NOT listed as numbered HARD checks above because
  they need no upstream comparison at all -- `already-published-
  violations`/`already-suppressed-violations` refuse to publish a
  report/suppress an alert for the SAME site twice, off dedicated
  `:report-published?`/`:alert-suppressed?` facts (never a `:status`
  value) -- the SAME 'check a dedicated boolean, not status'
  discipline every prior sibling governor's guards establish, informed
  by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320)."
  (:require [water.facts :as facts]
            [water.registry :as registry]
            [water.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Publishing a real public water-quality report and suppressing a real
  triggered safety alert are the two real-world actuation events this
  actor performs -- a two-member set, matching every prior dual-
  actuation sibling's shape. Note that `:actuation/suppress-alert` is
  the FIRST NEGATIVE actuation in this fleet -- withholding/silencing
  a notification, not issuing a record -- see this actor's own
  ADR-0001 Decision 1."
  #{:actuation/publish-report :actuation/suppress-alert})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:report/publish`/`:alert/suppress`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's drinking-water-safety requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :report/publish :alert/suppress} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:report/publish`/`:alert/suppress`, the jurisdiction's required
  site-intake/sensor-calibration-provenance/lab-result-chain-of-
  custody/public-reporting-source evidence must actually be satisfied
  -- do not trust the advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:report/publish :alert/suppress} op)
    (let [s (store/site st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction s) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(施設登録記録/センサー校正証明書/検査結果連鎖記録/公開報告根拠資料等)が充足していない状態での提案"}]))))

(defn- contaminant-level-out-of-range-violations
  "For `:report/publish`, INDEPENDENTLY recompute whether the site's
  own contaminant level falls outside its own recorded safe-range
  bounds via `water.registry/contaminant-level-out-of-range?` --
  needs no proposal inspection or stored-verdict lookup at all, since
  its inputs are permanent ground-truth fields already on the site."
  [{:keys [op subject]} st]
  (when (= op :report/publish)
    (let [s (store/site st subject)]
      (when (registry/contaminant-level-out-of-range? s)
        [{:rule :contaminant-level-out-of-range
          :detail (str subject " の検出濃度(" (:contaminant-level s)
                      ")が安全範囲[" (:contaminant-min s) "," (:contaminant-max s) "]を逸脱")}]))))

(defn- threshold-breach-unresolved-violations
  "An unresolved threshold breach -- reported by THIS proposal (e.g. a
  `:threshold/screen` that itself just found one), or already on file
  in the store for the site (`:threshold/screen`/`:alert/suppress`) --
  is a HARD, un-overridable hold. Evaluated UNCONDITIONALLY (not
  scoped to a specific op) so the screening op itself can HARD-hold on
  its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        site-id (when (contains? #{:threshold/screen :alert/suppress} op) subject)
        hit-on-file? (and site-id (= :unresolved (:verdict (store/threshold-screen-of st site-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :threshold-breach-unresolved
        :detail "未解決の閾値超過がある状態でのアラート抑制提案は進められない"}])))

(defn- already-published-violations
  "For `:report/publish`, refuses to publish a report for the SAME
  site twice, off a dedicated `:report-published?` fact (never a
  `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :report/publish)
    (when (store/site-already-published? st subject)
      [{:rule :already-published
        :detail (str subject " は既に報告公開済み")}])))

(defn- already-suppressed-violations
  "For `:alert/suppress`, refuses to suppress an alert for the SAME
  site twice, off a dedicated `:alert-suppressed?` fact (never a
  `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :alert/suppress)
    (when (store/site-already-suppressed? st subject)
      [{:rule :already-suppressed
        :detail (str subject " は既にアラート抑制済み")}])))

(defn check
  "Censors a Water Advisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (contaminant-level-out-of-range-violations request st)
                           (threshold-breach-unresolved-violations request proposal st)
                           (already-published-violations request st)
                           (already-suppressed-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
