# KDB AI Handoff Plan

## Goal
Turn this repository (currently `supabase-kmp`) into `kdb`: a Kotlin Multiplatform, modular, type-safe SQLite wrapper with high performance and simple CRUD/pagination APIs.

## Current State (as of 2026-05-07)
- Repo cloned to: `/Users/ranbirsingh/kdb`
- Upstream codebase: `AndroidPoet/supabase-kmp`
- Existing modules:
  - `supabase-core`
  - `supabase-client`
  - `supabase-auth`
  - `supabase-database`
  - `supabase-storage`
  - `supabase-realtime`
  - `supabase-functions`
- Root project name in `settings.gradle.kts`: `supabase-kmp`

## Target Product Shape
Keep the modular approach but repurpose modules around local SQLite:

- `kdb-core`
  - Result type, errors, IDs, value classes, SQL primitives, logging hooks.
- `kdb-driver`
  - `expect/actual` SQLite driver APIs and statement abstraction.
- `kdb-schema`
  - Table definitions, migrations, versioning.
- `kdb-query`
  - Typed CRUD/query APIs and light DSL.
- `kdb-paging`
  - Keyset pagination APIs and cursor model.
- `kdb-client`
  - Facade that composes modules into ergonomic entrypoint.

Optional later modules:
- `kdb-observe` (change streams)
- `kdb-sync` (remote sync adapters)

## Non-Goals
- Do not build a new database engine.
- Do not keep Supabase networking/auth/realtime/storage behavior.
- Do not add heavy ORM complexity.

## Design Constraints
- Simple API first.
- Type-safe inserts/updates/queries.
- High performance: prepared statements, batched transactions, keyset pagination.
- KMP-compatible (common API + platform actuals).
- Minimal runtime allocations on hot paths.

## Phase 1: Rebrand + Module Skeleton

### 1.1 Rename root identity
- Update `settings.gradle.kts`:
  - `rootProject.name = "kdb"`
- Replace module includes from `:supabase-*` to new `:kdb-*` modules.

### 1.2 Create new modules
Create directories + baseline Gradle setup for:
- `kdb-core`
- `kdb-driver`
- `kdb-schema`
- `kdb-query`
- `kdb-paging`
- `kdb-client`

### 1.3 Temporary coexistence strategy
- Keep old `supabase-*` modules compiling until new modules are stable.
- Mark old modules as deprecated in docs.

Acceptance checks:
- `./gradlew projects` lists all new `kdb-*` modules.
- Build files resolve without dependency cycles.

## Phase 2: Core Types and Error Model

Implement in `kdb-core`:

### 2.1 Result API
- `KdbResult<T>` with:
  - `map`, `flatMap`, `recover`, `getOrElse`
- Avoid checked exception leakage.

### 2.2 Error hierarchy
- `KdbError` sealed interface/classes:
  - `SqlError`
  - `ConstraintError`
  - `MigrationError`
  - `DriverError`
  - `SerializationError`

### 2.3 SQL primitives
- `SqlStatement`
- `SqlArg`
- `SqlValue`
- Lightweight logger interface

Acceptance checks:
- Unit tests for `KdbResult` transformations.
- Error mapping tests.

## Phase 3: Driver Layer (`kdb-driver`)

### 3.1 expect/actual abstractions
Common API:
- `KdbDriver`
  - `open(path/config)`
  - `close()`
  - `transaction {}`
  - `prepare(sql)`
- `KdbPreparedStatement`
  - typed binders (`bindLong`, `bindText`, etc.)
  - `execute()`, `executeQuery()`

### 3.2 Platform actuals
Start with:
- Android actual using AndroidX SQLite or SQLite driver backend.
- iOS actual using native SQLite (via Kotlin/Native interop).

### 3.3 Performance
- Statement reuse cache.
- Optional WAL and pragma configuration.
- Single-writer transaction queue strategy.

Acceptance checks:
- Insert/query benchmark smoke tests.
- Transaction correctness tests.

## Phase 4: Schema + Migration (`kdb-schema`)

### 4.1 Migration model
- `Migration(version: Int, upSql: List<String>)`
- `MigrationRunner` with atomic migration execution.

### 4.2 Schema registry
- `SchemaDefinition` holding tables + migrations.

### 4.3 Metadata table
- Internal table for current schema version and checksum.

Acceptance checks:
- Upgrade path test: v1 -> v2 -> v3.
- Failure rollback test.

## Phase 5: Typed Query + CRUD (`kdb-query`)

### 5.1 Table + row contracts
- `Table<R : Any>`
  - `name`
  - `columns`
  - `fromRow(reader)`
  - `toValues(row)`

### 5.2 Query API
- `insert(table, row)`
- `update(table, row, where)`
- `delete(table, where)`
- `select(table, where, orderBy, limit)`
- `queryOne`, `queryList`

### 5.3 Simple DSL (no heavy ORM)
- Keep very small surface:
  - predicates, order, limit
- Allow raw SQL escape hatch.

Acceptance checks:
- CRUD integration tests on Android and iOS targets.
- Verify generated SQL + bind ordering.

## Phase 6: Pagination (`kdb-paging`)

### 6.1 Cursor model
- `PageCursor` (opaque string or typed keyset values)
- `Page<T>(items, nextCursor, hasMore)`

### 6.2 Keyset-first paging
- APIs like:
  - `selectPageByIdDesc(lastId, limit)`
  - `selectPageByTimestamp(lastTimestamp, limit)`

### 6.3 Offset fallback
- Optional `LIMIT/OFFSET` helper for simple use cases.

Acceptance checks:
- Deterministic ordering tests under concurrent inserts.
- No duplicates/missing rows across pages.

## Phase 7: Client Facade (`kdb-client`)

### 7.1 Public entrypoint
- `Kdb.create(...) { ... }`
- Compose driver + schema + query + paging.

### 7.2 Configuration DSL
- DB path/name
- Pragmas
- Logging
- Migration strategy

### 7.3 DI hooks
- Optional Koin modules (if retained from current architecture patterns).

Acceptance checks:
- One-screen quick-start sample compiles in README.

## Phase 8: Docs + Samples + Cleanup

### 8.1 Docs
- Replace Supabase docs with KDB docs:
  - Purpose
  - module map
  - setup
  - CRUD
  - transactions
  - pagination
  - migration

### 8.2 Samples
- Simple TODO sample with:
  - create table
  - insert/update/delete
  - keyset pagination

### 8.3 Remove obsolete modules
- Remove `supabase-*` once `kdb-*` reaches functional parity for local DB goals.

Acceptance checks:
- `./gradlew build` passes.
- Basic sample passes on Android + iOS.

## Suggested Execution Order for Another AI

1. Rebrand root and create new module skeletons.
2. Implement `kdb-core` + tests.
3. Implement `kdb-driver` Android actual first; then iOS.
4. Add migrations in `kdb-schema`.
5. Add typed CRUD in `kdb-query`.
6. Add keyset pagination in `kdb-paging`.
7. Build `kdb-client` facade.
8. Rewrite docs and remove Supabase leftovers.

## Concrete First PR Scope (small and safe)
- `settings.gradle.kts` root rename to `kdb`.
- Add empty new modules with compilable source sets.
- Introduce `KdbResult` + `KdbError` in `kdb-core`.
- Add initial README section: “KDB vision and module map”.

## Risks and Mitigations
- Risk: Over-engineering into ORM complexity.
  - Mitigation: Keep API minimal and explicit; raw SQL escape hatch.
- Risk: KMP native driver edge cases.
  - Mitigation: Start Android-first, add integration tests before iOS expansion.
- Risk: Paging regressions with non-unique sort columns.
  - Mitigation: enforce stable sort keys and tie-breakers.

## Definition of Done (MVP)
- Type-safe CRUD works across Android + iOS.
- Migrations run atomically.
- Keyset pagination available and tested.
- Modular architecture retained (`kdb-*` modules).
- README documents end-to-end setup and usage.
