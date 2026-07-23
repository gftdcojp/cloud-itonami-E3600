# ADR-0001: cloud-itonami-isic-3600 -- Water Advisor as a contained intelligence node

- Status: Accepted (2026-07-08)
- Related: `cloud-itonami-isic-6511`/`6512`/`6621`/`6622`/`6629`/`6520`/
  `6530`/`6820`/`6612`/`6492`/`6920`/`6611`/`7120`/`8620`/`8530`/`9200`/
  `7500`/`9603`/`9521`/`9321`/`8730`/`9102`/`9103`/`9602`/`9000`/`8890`/
  `8610`/`9311`/`8510`/`9412`/`6491`/`8720`/`8521`/`6619` ADR-0001s (the
  pattern this ADR ports); ADR-2607071250/ADR-2607071320/
  ADR-2607071351/ADR-2607071618/ADR-2607071640/ADR-2607071654/
  ADR-2607071717/ADR-2607071732/ADR-2607071752/ADR-2607071819/
  ADR-2607071849/ADR-2607071922/ADR-2607072715/ADR-2607072730/
  ADR-2607072745/ADR-2607072800/ADR-2607072815/ADR-2607072830/
  ADR-2607072845/ADR-2607072900/ADR-2607072915/ADR-2607080100/
  ADR-2607080200/ADR-2607080300/ADR-2607080400/ADR-2607080500
  (`6612`/`6492`/`6920`/`6611`/`7120`/`8620`/`8530`/`9200`/`7500`/
  `9603`/`9521`/`9321`/`8730`/`9102`/`9103`/`9602`/`9000`/`8890`/
  `8610`/`9311`/`8510`/`9412`/`6491`/`8720`/`8521`/`6619`, the twenty-
  six verticals built outside ADR-2607032000's original insurance/
  real-estate batch -- this is the twenty-seventh)
- Context: Continuing the standing "pick a new ISIC blueprint
  vertical" direction past `6619`, this ADR deepens `cloud-itonami-
  isic-3600` (water collection, treatment and supply) from `:blueprint`
  to `:implemented`, the forty-first actor in this fleet -- the FIRST
  infrastructure/utility vertical built in this fleet (every prior
  actor has been a human-services, leisure or financial-services
  domain).

## Problem

A water utility's report-publication/alert-suppression workflow
bundles several distinct concerns under one governed workflow:

1. **Jurisdiction drinking-water-safety correctness** -- an official
   spec-basis citation from a real regulator (国土交通省/環境省 under
   the current post-2024 Waterworks Act reorganization/the EPA's
   SDWA/the DWI/the UBA under the TrinkwV), never fabricated.
2. **Contaminant-level sufficiency** -- does a site's own measured
   contaminant level stay within its own recorded safe-range bounds?
   The THIRD instance of this fleet's two-sided range check family
   (`testlab.registry/within-tolerance?`/`conservation.registry/body-
   condition-out-of-range?` established the first two).
3. **Threshold-breach resolution verification** -- has a threshold
   breach against the site actually stayed unresolved before an alert
   is suppressed? The water-utility-specific application of the
   unconditional-evaluation screening discipline this fleet's
   `casualty.governor/sanctions-violations` originally established --
   a TWENTY-FIFTH distinct grounding overall, and the FIRST
   specifically for a threshold-breach concept.
4. **Real, high-stakes actuation, twice, one of which is NEGATIVE** --
   publishing a real public report and suppressing a real triggered
   safety alert are two independently-gated real-world acts on the
   SAME entity (a site) -- and critically, the second of these is the
   first NEGATIVE actuation this fleet has modeled: withholding a
   notification, not issuing a record.

An LLM has no authority or grounding for any of these. The design
problem is therefore not "run a water utility with an LLM" but "seal
the LLM inside a trust boundary and layer evidence-sufficiency,
contaminant-level verification, threshold-breach-resolution
verification, audit and human-approval on top of it, while
structurally fixing both real actuation events as human-only --
including the negative one."

## Decision

### 1. `:actuation/suppress-alert` is the FIRST negative actuation this fleet has modeled

Every prior dual-actuation sibling in this fleet gates two POSITIVE
acts: issuing a certification, finalizing a discharge, releasing a
chargeback hold, and so on -- each one CREATES a real-world record.
`:actuation/suppress-alert` is structurally different: it WITHHOLDS/
SILENCES an already-triggered safety notification. This blueprint's
own published Core Contract makes the same point explicit: "No
automated recommendation can suppress a safety alert ... without
governor approval and audit evidence" -- suppression, not
notification, is the dangerous act requiring the SAME governed-actor
discipline as every positive actuation in this fleet. This build
treats it identically to a positive actuation (HARD checks, high-
stakes gate, phase-3 exclusion, dedicated double-actuation boolean) --
the discipline generalizes cleanly to the negative direction with no
special-casing required.

### 2. Water Advisor is sealed into the bottom node; it never publishes a report or suppresses an alert directly

`water.wateradvisor` returns exactly five kinds of proposal: intake
normalization, jurisdiction drinking-water-safety checklist,
threshold-breach screening, report-publication draft, and alert-
suppression draft. No proposal writes the SSoT or commits a real
report-publication/alert-suppression directly.

### 3. OperationActor = langgraph-clj StateGraph, 1 run = 1 water-utility operation

`water.operation/build` is the SAME StateGraph shape as every sibling
actor's operation namespace, copied verbatim.

### 4. `contaminant-level-out-of-range?` is the THIRD instance of the two-sided range check family

`testlab.registry/within-tolerance?` established the FIRST two-sided
range check in this fleet, `conservation.registry/body-condition-out-
of-range?` the SECOND. `contaminant-level-out-of-range?` is the THIRD
instance, reusing the identical lo/hi-bounds-comparison shape for a
site's own measured contaminant level against the site's own recorded
safe-range bounds.

### 5. Threshold-breach screening reuses the unconditional-evaluation discipline for a twenty-fifth distinct grounding, and a first for this concept

`threshold-breach-unresolved-violations` reuses `casualty.governor/
sanctions-violations`'s fix (evaluated unconditionally, not scoped to
a specific op, so the screening op itself can HARD-hold on its own
finding) for `:threshold/screen` AND `:alert/suppress` -- the TWENTY-
FIFTH distinct application of this exact discipline in this fleet
overall, and the FIRST specifically for a threshold-breach concept.

### 6. The unconditional-evaluation check is tested via the SCREENING op directly, per the lesson already recorded by `parksafety` and fourteen later siblings

`threshold-breach-is-held-and-unoverridable` calls `:threshold/screen`
directly against `site-4` (an unresolved breach), NOT `:alert/
suppress` against an unscreened site -- because a failing screen is
itself a HARD hold whose payload never persists to the store, so the
actuation op alone could never discover the bad ground-truth flag
through this check family without the screening op having actually
been run first. This build applied that lesson PROACTIVELY for a
fifteenth consecutive vertical (after `eldercare`, `museum`,
`conservation`, `salon`, `entertainment`, `casework`, `hospital`,
`facility`, `school`, `association`, `leasing`, `behavioral`,
`secondary` and `card`), further reinforcing that lessons recorded in
this fleet's ADRs transfer forward reliably.

### 7. Dual actuation, matching `6512`/`6622`/`6520`/`6530`/`6820`/`6920`/`6611`/`8530`/`9200`/`9521`/`8730`/`9102`/`9103`/`8890`/`8610`/`8510`/`9412`/`8720`/`8521`/`6619`'s shape

`water.governor`'s `high-stakes` set has exactly two members
(`:actuation/publish-report`, `:actuation/suppress-alert`), each
acting on the SAME site entity, each with its OWN history collection
(`report-history`/`suppression-history`), sequence counter and
dedicated double-actuation-guard boolean.

### 8. Double-publication/double-suppression guards check dedicated booleans, not `:status`

`already-published-violations`/`already-suppressed-violations` check
`:report-published?`/`:alert-suppressed?`, dedicated booleans set
once and never cleared, rather than a `:status` value that could
legitimately advance past a checked state (the exact trap `cloud-
itonami-isic-6492`'s ADR-0001 documents in detail, explicitly avoided
BY DESIGN in every sibling actor's equivalent guard since). This
actor's `:status` never needs to encode "has this actuation already
happened" at all -- a deliberate architectural choice applied here for
a twenty-sixth consecutive time.

### 9. No bespoke capability lib

Like `6920`/`7120`/`8620`/`8530`/`9200`/`7500`/`9603`/`9521`/`9321`/
`8730`/`9102`/`9103`/`9602`/`9000`/`8890`/`8610`/`9311`/`8510`/`9412`/
`8720`/`8521`, and unlike most other actors in this fleet, this
vertical's site records are practice-specific rather than a shared
cross-operator data contract -- `water.*` runs on the generic
telemetry/forms/dmn/bpmn/audit-ledger stack only, per the blueprint's
own explicit (now corrected -- see Decision 10) `:required-
technologies` statement.

### 10. Blueprint's own missing fields corrected to match the registry

This blueprint's `blueprint.edn` was missing `:itonami.blueprint/
required-technologies`/`:optional-technologies` entirely (unlike every
other blueprint in this fleet, which all specify them), even though
`kotoba-lang/industry`'s own registry entry for `"3600"` already
specified `[:robotics :telemetry :dmn :bpmn :audit-ledger :forms]` /
`[:cfd :cae]`. Its internal `:itonami.blueprint/id` also still carried
the pre-rename `"cloud-itonami-3600"` value. Both fixed as part of
this promotion to match the registry and the already-renamed GitHub
repo -- the SAME kind of stale-field fix `6619`'s ADR-0001 Decision 9
already established for that sibling's own ID field.

## Consequences

- (+) Water-utility operation gets the same governed, auditable-actor
  treatment as the thirty-four prior actors, and this fleet now has a
  TWENTY-SEVENTH concrete precedent for extending past ADR-2607032000's
  original scope, and its FIRST infrastructure/utility-sector
  vertical.
- (+) `:actuation/suppress-alert` is a genuine structural contribution:
  the first negative actuation this fleet has modeled, proving the
  governed-actor discipline (HARD checks, high-stakes gate, phase-3
  exclusion, dedicated boolean guard) generalizes cleanly to
  withholding a notification, not just issuing a record.
- (+) `contaminant-level-out-of-range?` is a genuine structural
  contribution: the third instance of the two-sided range check
  family.
- (+) `threshold-breach-unresolved-violations` is a genuine domain-
  modeling contribution: the first unconditional-evaluation grounding
  for a threshold-breach concept.
- (+) The actuation invariant (governor + phase, two layers) is
  regression-tested by `test/water/phase_test.clj`'s `report-publish-
  never-auto-at-any-phase`/`alert-suppress-never-auto-at-any-phase`.
- (+) `MemStore` ‖ `DatomicStore` parity is proven by `test/water/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- (+) The threshold-breach test/demo correctly applied the established
  SCREENING-op-directly pattern for a fifteenth consecutive vertical
  -- further evidence that lessons recorded in this fleet's ADRs
  continue to transfer forward reliably.
- (+) Two small pre-existing inconsistencies (missing required/
  optional-technologies fields, a stale internal ID field) were
  corrected as part of this promotion.
- (-) This R0 seeds only 6 jurisdictions (JPN, USA, GBR, DEU, FRA, NZL)
  with an official spec-basis, out of ~194 worldwide (FRA and NZL
  added post-promotion; counts here corrected accordingly);
  `water.facts/coverage` reports this honestly rather than claiming
  broader coverage.
- (-) `contaminant-level-out-of-range?` models only a single
  contaminant-level comparison, not real SCADA/sensor-network
  telemetry ingestion or a full hydraulic-modeling engine -- see
  README `Scope`/coverage table for the full honest-scope accounting.
- 36 tests / 176 assertions, lint clean.

## Alternatives considered

| Option | Verdict | Reason |
|---|---|---|
| Model `:actuation/suppress-alert` as a "positive" actuation by inverting its framing (e.g. "finalize an alert-suppression-approval record") | ❌ | The blueprint's own Core Contract explicitly frames the dangerous act as SUPPRESSION itself, not the record of it -- honestly naming the actuation for what it does (withholding a notification) is clearer than obscuring it behind a record-issuance euphemism |
| Add this as an addendum to any prior post-batch ADR | ❌ | All twenty-six of those ADRs' titles and scopes are explicitly named human-services/leisure/financial-services verticals; this is this fleet's first infrastructure/utility vertical, with no prior sibling sharing even the same broad sector |
| Keep `cloud-itonami-isic-3600` at `:blueprint` only | ❌ | The standing direction continues past `6619`; water-utility operation is a natural next domain, opening this fleet's first infrastructure/utility-sector coverage |
| Test `threshold-breach-unresolved-violations` via an actuation op against an unscreened site (the shape `parksafety`'s ORIGINAL, buggy test used) | ❌ | Already proven wrong by `parksafety`'s own ADR-2607071922 Decision 5 and reconfirmed by fourteen later siblings' ADR-0001s -- a failing screen never persists its payload to the store, so the actuation op alone cannot discover the bad ground-truth flag through this check family; this build tested the SCREENING op directly from the start |
| Leave the blueprint's missing `:required-technologies`/stale ID fields untouched | ❌ | They contradicted the industry registry's own stated required/optional technologies and the already-renamed GitHub repo -- small, low-risk, in-scope consistency fixes to make during this promotion, matching `6619`'s ADR-0001 precedent for an analogous fix |

## References

- ADR-2607071250/ADR-2607071320/ADR-2607071351/ADR-2607071618/
  ADR-2607071640/ADR-2607071654/ADR-2607071717/ADR-2607071732/
  ADR-2607071752/ADR-2607071819/ADR-2607071849/ADR-2607071922/
  ADR-2607072715/ADR-2607072730/ADR-2607072745/ADR-2607072800/
  ADR-2607072815/ADR-2607072830/ADR-2607072845/ADR-2607072900/
  ADR-2607072915/ADR-2607080100/ADR-2607080200/ADR-2607080300/
  ADR-2607080400/ADR-2607080500 (`6612`/`6492`/`6920`/`6611`/`7120`/
  `8620`/`8530`/`9200`/`7500`/`9603`/`9521`/`9321`/`8730`/`9102`/
  `9103`/`9602`/`9000`/`8890`/`8610`/`9311`/`8510`/`9412`/`6491`/
  `8720`/`8521`/`6619`, first twenty-six post-batch verticals)
- ADR-2607032000 (original insurance/real-estate batch, Addenda 1-7)
- `cloud-itonami-isic-3600/docs/adr/0001-architecture.md` (this ADR)
- `kotoba-lang/industry` `resources/kotoba/industry/registry.edn`
  (fleet-wide maturity registry)
