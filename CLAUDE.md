# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Standards

Read `.claude/instructions.md` on every session start — it is the authoritative coding standards doc.

Quick rules:
- YAML: two-space indentation.
- Imports: never star imports; FQNs only on name conflicts.
- Comments: gotchas only — never explain what the code already says.
- Variable and param names: verbose, never abbreviated; param name must match its type (`saleSaveRequest: SaleSaveRequest`, not `input: SaleSaveRequest`).
- Endpoints are thin delegators — mapping/assembler logic lives in services, never in the endpoint body.

## Commands

```bash
# Compose files, run scripts and env files live in the sibling repo liberty-ops (applications/easy-records/server/)
# Start infrastructure (Postgres, Redis), on the dev infra host
../liberty-ops/applications/easy-records/server/scripts/dev.sh          # up
../liberty-ops/applications/easy-records/server/scripts/dev.sh down     # down

# Start only the app container, against the infra that dev.sh started (hosts come from env/dev.env there)
../liberty-ops/applications/easy-records/server/scripts/debug.sh        # up
../liberty-ops/applications/easy-records/server/scripts/debug.sh down   # down
```

## Architecture

The app is a Spring Boot + Kotlin monolith backed by **three independent datasources** — `platform`, `organization`, and `location` — each with its own Liquibase changelog and connection pool (configured in `application.yml`). Schemas are per-organization (`org-a`, `org-b`, …) and per-location (`loc-1`, `loc-2`, …); the location datasource sets the search path dynamically per request via `@TransactionalOnLocationSchema`.

### Layer structure

```
platform/       — cross-org metadata: users, table registry, db versioning, jurisdiction/tax setup
organizations/  — org-scoped data: products, contacts, payment methods, fiscal periods, adjustment reasons
locations/      — location-scoped operations: sales, purchases, inventory, stock
```

Each domain lives under a product folder: `*/business/core/<domain>/` and `cross_tier/core/<domain>/` for core, `*/business/retail/<domain>/` and `cross_tier/retail/<domain>/` for retail. The `platform` tier holds only core domains and has no product folders. Endpoints are split the same way under `*/rest/endpoints/core/` and `*/rest/endpoints/retail/`. Anything not under `api/` is internal to that domain. **`ArchitectureTest` enforces this** — cross-domain access to non-`api` classes will fail the test suite. When cross-domain communication is needed, import from `<domain>/api/` only.

### Products

The platform hosts products on a shared core. A domain is tagged by the product folder it sits in (`core`, `retail`; a later billing product gets its own folder the same way, and billing may split into more than one product). The `platform` tier is core throughout. A domain outside a product folder fails `ArchitectureTest`, so a new domain fails the build until it is placed. `core` depends on no product, and products never depend on each other; `ArchitectureTest` enforces the direction. Existing core-to-retail dependencies sit in an explicit allow-list in `ProductTags`; it only shrinks. Billing is planned and is defined outside this repo.

### Key locations domains

| Domain                                          | What it owns                                                           |
|-------------------------------------------------|------------------------------------------------------------------------|
| `sale/` + `sale_session/`                       | Full sale lifecycle; session is the only path to create/confirm a sale |
| `purchase/` + `delivery/` + `supplier_payment/` | Purchase orders, deliveries (stock-in), supplier payments              |
| `stock/`                                        | Stock balances, reservations, FIFO movement                            |
| `location_product/`                             | Per-location product catalog and pricing                               |
| `sale_adjustment/`                              | Discounts and surcharges (shared by sale and sale_session)             |
| `tax_entry/`                                    | Async tax finalization records (written by Kafka consumers)            |

Each domain with non-obvious rules has a `README.md` — read it before touching the package, update it after.

### Messaging

Kafka events are published via `ApplicationEventPublisher` and bind to the enclosing transaction commit (a rolled-back transaction never reaches the broker). Every processor implements `shouldProcess` backed by a unique DB constraint on the natural key so retries are idempotent.

### Utilities to always use

- `DateTimes` — all `LocalDate` / `OffsetDateTime` logic; never `LocalDate.now()` or `ZoneOffset.UTC`
- `Decimals` — monetary arithmetic; money uses `Decimals.MoneyScale` (the `Decimals.MONEY_SCALE` constant, 2, half-up), never a hardcoded scale
- `StringUtils` — blank-to-null and other string helpers

### Database migrations

Liquibase changelogs live under `src/main/resources/db/changelog/{platform,organizations,locations}/`. Within each layer the load order is: `tables/ → views/ → sequences/ → foreign_keys/ → triggers/ → indexes/ → audit/ → version-entries/ → registry-entries/` (the last two are platform-only). All new tables must be registered in `registry-entries/` and in the `TableNames` constants class.

## Agent discipline

Before starting a multi-step task: write a plan to `.claude-local/plan.txt` (personal scratch, not committed) and update it as you go. Read at most 3 files before making a first edit, then adjust. Checkpoint every 5 tool calls — state your hypothesis and decide whether to continue or back out.
