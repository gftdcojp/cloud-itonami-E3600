# cloud-itonami-isic-3600

Open Business Blueprint for **ISIC Rev.5 3600**: water collection,
treatment and supply. This repository publishes a water-utility actor
-- site intake, jurisdiction assessment, threshold-breach screening,
report publication and alert suppression -- as an OSS business that
any qualified water-utility operator can fork, deploy, run, improve
and sell.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619)) --
the FIRST infrastructure/utility vertical in this fleet (every prior
actor has been a human-services, leisure or financial-services
domain). Here it is **Water Advisor ⊣ Water Safety Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a site-
> intake summary, normalizing records, and checking whether a site's
> own measured contaminant level actually stays within its own
> recorded safe-range bounds -- but it has **no notion of which
> jurisdiction's drinking-water-safety requirements are official, no
> license to publish a real public water-quality report or suppress a
> real triggered safety alert, and no way to know on its own whether a
> threshold breach against the site has actually stayed unresolved**.
> Letting it publish a report or suppress an alert directly invites
> fabricated jurisdiction citations, a report published on an out-of-
> range contaminant reading, and an unresolved threshold breach being
> quietly overlooked -- and liability, and public-health risk, for
> whoever runs it. This project seals the Water Advisor into a single
> node and wraps it with an independent **Water Safety Governor**, a
> human **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers site intake through jurisdiction assessment,
threshold-breach screening, report publication and alert suppression.
It does **not**, by itself, hold any license required to operate a
water utility in a given jurisdiction, and it does not claim to. It
also does **not** model a full SCADA/telemetry-ingestion system or
real-time sensor-network integration -- no sensor-protocol-specific
ingestion pipeline, no full hydraulic-modeling engine (see
`water.facts`'s own docstring for the honest simplification this
makes: a starting catalog of licensing requirements, not a survey of
every jurisdiction's water-quality-standard variant). Whoever deploys
and operates a live instance (a licensed water-utility operator)
supplies any jurisdiction-specific license, the real hydrological/
water-treatment expertise and the real SCADA/sensor-network
integrations, and bears that jurisdiction's liability -- the software
supplies the governed, spec-cited, audited execution scaffold so that
operator does not have to build the compliance layer from scratch for
every new market.

### Actuation

**Publishing a real public report or suppressing a real triggered
safety alert is never autonomous, at any phase, by construction.** Two
independent layers enforce this (`water.governor`'s `:actuation/
publish-report`/`:actuation/suppress-alert` high-stakes gate and
`water.phase`'s phase table, which never puts `:report/publish`/
`:alert/suppress` in any phase's `:auto` set) -- see `water.phase`'s
docstring and `test/water/phase_test.clj`'s `report-publish-never-
auto-at-any-phase`/`alert-suppress-never-auto-at-any-phase`. The actor
may draft, check and recommend; a human utility officer is always the
one who actually publishes a report or suppresses an alert. Like
`6512`/`6622`/`6520`/`6530`/`6820`/`6920`/`6611`/`8530`/`9200`/`9521`/
`8730`/`9102`/`9103`/`8890`/`8610`/`8510`/`9412`/`8720`/`8521`/`6619`,
this actor has TWO actuation events -- but unlike every prior sibling,
**`:actuation/suppress-alert` is a NEGATIVE actuation**: it withholds/
silences a notification rather than issuing a record, the first time
this fleet has modeled a high-stakes act in that direction. See this
actor's own `docs/adr/0001-architecture.md` Decision 1 for the honest
framing this makes.

## The core contract

```
site intake + jurisdiction facts (water.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────────┐
   │ Water        │ ─────────────▶ │ Water                         │  (independent system)
   │ Advisor      │  + citations    │ Safety Governor:               │
   │ (sealed)     │                 │ spec-basis · evidence-       │
   └──────────────┘         commit ◀────┼──────────▶ hold │ incomplete ·
                                 │             │           │ contaminant-level-
                           record + ledger  escalate ─▶ human   out-of-range (two-
                                             (ALWAYS for         sided range) ·
                                              :report/publish /       threshold-breach-
                                              :alert/suppress)         unresolved (unconditional) ·
                                                                       already-published/-suppressed
```

**The Water Advisor never publishes a report or suppresses an alert
the Water Safety Governor would reject, and never does so without a
human sign-off.** Hard violations (fabricated jurisdiction
requirements; unsupported evidence; a contaminant reading out of its
own safe-range bounds; an unresolved threshold breach; a double
publication or suppression) force **hold** and *cannot* be approved
past; a clean publication/suppression proposal still always routes to
a human.

## Run

```bash
clojure -M:dev:run     # walk one clean dual-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

A live sample of the operator console is rendered at build time by
actually running the real actor stack (`water.render-html`, driving
`water.operation`'s StateGraph against a seeded `water.store`) into
[docs/samples/operator-console.html](docs/samples/operator-console.html)
-- `clojure -M:dev:render-html`, regenerated nightly (and on demand) by
[`.github/workflows/regenerate.yml`](.github/workflows/regenerate.yml),
which commits only when the output actually changed.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a sampling and valve robot
performs water sampling, valve actuation and leak survey at treatment
and distribution points, under the actor, gated by the independent
**Water Safety Governor**. The governor never dispatches hardware
itself; `:high`/`:safety-critical` actions (such as operating near
water sources, treatment chemicals or public supply) require human
sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Water Safety Governor, report-publication + alert-suppression draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`3600`). Like `6920`/`7120`/`8620`/`8530`/`9200`/`7500`/`9603`/`9521`/
`9321`/`8730`/`9102`/`9103`/`9602`/`9000`/`8890`/`8610`/`9311`/`8510`/
`9412`/`8720`/`8521`, this vertical's site records are practice-
specific rather than a shared cross-operator data contract, so
`water.*` runs on the generic telemetry/forms/dmn/bpmn/audit-ledger
stack only -- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/water/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + separate report-publication/alert-suppression history. No dynamically-filed sub-record -- both actuation ops act directly on a pre-seeded site, and the double-actuation guards check dedicated `:report-published?`/`:alert-suppressed?` booleans rather than a `:status` value |
| `src/water/registry.cljc` | Report-publication + alert-suppression draft records, plus `contaminant-level-out-of-range?` -- the THIRD instance of this fleet's two-sided range check family (`testlab`/`conservation` established the first two) |
| `src/water/facts.cljc` | Per-jurisdiction drinking-water-safety catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/water/wateradvisor.cljc` | **Water Advisor** -- `mock-advisor` ‖ `llm-advisor`; intake/assessment/threshold-breach-screening/report-publication/alert-suppression proposals |
| `src/water/governor.cljc` | **Water Safety Governor** -- 4 HARD checks (spec-basis · evidence-incomplete · contaminant-level-out-of-range, pure ground-truth two-sided-range recompute · threshold-breach-unresolved, unconditional evaluation, the TWENTY-FIFTH grounding of this discipline and FIRST specifically for the threshold-breach concept) + already-published/already-suppressed guards + 1 soft (confidence/actuation gate) |
| `src/water/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted assess → supervised (both report publication and alert suppression always human; site intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/water/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/water/sim.cljc` | demo driver |
| `src/water/render_html.clj` | build-time renderer for `docs/samples/operator-console.html` -- drives the real actor, no invented numbers (`clojure -M:dev:render-html`) |
| `test/water/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers site intake through jurisdiction assessment,
threshold-breach screening, report publication and alert suppression
-- the core governed lifecycle this blueprint's own `docs/business-
model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Site intake + per-jurisdiction drinking-water-safety checklisting, HARD-gated on an official spec-basis citation (`:site/intake`/`:jurisdiction/assess`) | Real SCADA/sensor-network telemetry ingestion, full hydraulic-modeling engines (see `water.facts`'s docstring) |
| Threshold-breach screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:threshold/screen`) | Real water-utility-information-system integration |
| Report publication, HARD-gated on full evidence and contaminant-level sufficiency, plus a double-publication guard (`:report/publish`) | Ongoing maintenance/leak-response workflows themselves |
| Alert suppression, HARD-gated on full evidence and a double-suppression guard (`:alert/suppress`) | |
| Immutable audit ledger for every intake/assessment/screening/publication/suppression decision | |

Extending coverage is additive: add the next gate (e.g. a leak-
response-dispatch check) as its own governed op with its own HARD
checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world act"
pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`water.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `water.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `water.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to make
coverage look bigger.

## Maturity

`:implemented` -- `Water Advisor` + `Water Safety Governor` run as
real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the thirty-
four prior actors' architecture. See `docs/adr/0001-architecture.md`
for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
