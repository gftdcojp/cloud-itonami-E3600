# cloud-itonami-3600

Open Business Blueprint for **ISIC Rev.5 3600**: water collection, treatment
and supply.

This repository designs a forkable OSS business for water-quality monitoring,
small-utility operations, leak response, and public water-safety reporting.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a sampling and valve robot performs water sampling, valve actuation and leak survey at treatment and distribution points under an actor that proposes
actions and an independent **Water Safety Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near water sources, treatment chemicals or public supply) require human sign-off.

## Core Contract

```text
sensor samples + lab tests + asset records
        |
        v
Water Advisor -> Safety Governor -> notify, hold, or human approval
        |
        v
incident log + public report + maintenance queue
```

No automated recommendation can suppress a safety alert or publish a water
quality claim without governor approval and audit evidence.

## Runbook

- Start with read-only water-quality and asset logs.
- Add threshold alerts and maintenance queues.
- Add public reporting only after provenance checks.
- Keep emergency notifications human-visible.

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

Code and implementation templates are AGPL-3.0-or-later.
