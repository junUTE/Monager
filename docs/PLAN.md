# Monager – Implementation Plan

Offline-only, Kotlin + Compose + Room. Target: Moto G35 (60fps, 120Hz where cheap). Rules: `/Agent.md`. Architecture: `ARCHITECTURE.md`. Visual reference: `mockup/index.html`. Progress: `TRACKER.md`.

> Note: the Figma Make link could not be read (HTTP 403). Mockup is derived from the feature list; reconcile with Figma when exported.

## Milestones

| M | Name | Outcome | Features |
|---|------|---------|----------|
| M0 | Foundation | Project builds, DB opens, theme + shared 3D components | repo skeleton (done), seed data, design tokens, `RaisedCard/RaisedButton/Chip`, light+dark theme |
| M1 | Data layer | Full schema, DAOs, repositories, unit tests | Entities for subcategory, savings vault, debt payments, transfer; migrations tests |
| M2 | Add Transaction (F1) | Core input flow | 3-mode switch, calculator numpad, 2-level category picker, date/time, note, wallet |
| M3 | Home (F2) | Dashboard | Hero card, quick actions, mini budget bar, recent timeline |
| M4 | Categories (F6) | Manage taxonomy and icons | Parent/child CRUD + reorder, vector icon set |
| M5 | Budgets & Savings (F4) | Limits and vaults | Total/per-category budgets, thumb slider, >=90% warning, savings vaults |
| M6 | Debts (F5) | Borrow/lend tracking | 2 tabs, detail, partial pay/collect creates transaction |
| M7 | Analytics (F3) | Reports | Period filter, Canvas donut, income/expense bars, ranked list |
| M8 | Settings & Data (F7) | Theme + backup | Dark/light, CSV/JSON export-import via SAF |
| M9 | Performance hardening | Meet budgets | Baseline Profile, Macrobenchmark, jank audit on Moto G35, R8 check |
| M10 | Release | Signed APK/AAB | QA pass, icon, versioning |

Suggested order: M0 → M1 → M4 (seed/taxonomy) → M2 → M3 → M5 → M6 → M7 → M8 → M9 → M10.

## Key design decisions

1. **Transaction types:** `EXPENSE`, `INCOME`, `SAVING` (deposit into a vault), plus `TRANSFER` between wallets (Quick Action "Chuyển khoản"). Extend `TxType`; add `vaultId?`, `toWalletId?`.
2. **Two-level categories:** add `parentId: Long?` and `sortOrder: Int` to `categories`. Transactions reference the leaf (child) or parent if it has no children.
3. **Calculator:** pure Kotlin `CalcEngine` (tokenizer + evaluator on `Long`, supports `+ - × ÷`, "k" = ×1000). Unit-tested; no UI dependency. Rounds division to whole đồng.
4. **Budgets:** `budgets(categoryId nullable = total, month, limit)`. Spent computed by SQL. State: normal <90%, warning >=90% (red), over >=100%.
5. **Savings vaults:** `vaults(id, name, target, iconKey, color, deadline?)`; saved amount = SUM of `SAVING` transactions with that `vaultId` (no denormalized counter).
6. **Debts:** `debts` + `debt_payments`. Paying/collecting inserts a payment row and (optionally) a transaction in one `@Transaction`; remaining = amount − SUM(payments).
7. **Icons:** Lucide/Phosphor exported to `ImageVector` (build-time generated Kotlin or XML VectorDrawable). Icon registry maps `iconKey` → vector. No font files, no PNG.
8. **Backup:** stream JSON (full fidelity) and CSV (transactions only) through Storage Access Framework on `Dispatchers.IO`; import is transactional with schema `version` check.
9. **Theme:** light + dark static token sets; no dynamic color; no runtime blur (frosted look = 85–92% solid surface + 1px border).
10. **Charts:** custom Canvas, geometry precomputed in ViewModel, one animated progress float read in draw phase.

## Schema delta vs current code (M1)

- `categories`: + `parentId`, `sortOrder`
- `transactions`: + `vaultId?`, `toWalletId?`, type adds `SAVING`, `TRANSFER`
- new `vaults`, `debt_payments`
- `debts`: replace `settled` with derived state; keep `iLent`, `dueDate`
- `budgets`: `categoryId` nullable (null = total budget)

## Performance budget (all milestones)

- Scroll: 0 frames >16ms on Home/Transactions at Moto G35.
- Cold start to first frame: <= 1.2s (release + Baseline Profile).
- No `blur()`, no per-item `shadow()`, no allocation in draw scope; every list has `key` + `contentType`.
- DB: queries on indexed columns; aggregates in SQL; month-windowed lists/Paging.
- Each milestone's PR must list a "Performance notes" block (see Agent.md §7).

## Definition of Done (per feature)

- Matches mockup in light + dark.
- Unit tests for logic (calculator, aggregation, debt math); DAO tests via in-memory Room.
- Macrobenchmark/JankStats check for scrolling screens.
- Tracker updated.

## Risks

| Risk | Mitigation |
|------|-----------|
| Neo-3D look costs GPU | Cached `drawBehind` gradients, hero element only gets true shadow; benchmark early (M0) |
| Schema churn after release | Finish M1 before any user data; commit `schemas/`, write migrations from v2 on |
| Calculator edge cases | Table-driven unit tests; clamp amount to Long range |
| Icon set bloat | Import only used icons into registry |
| Figma mismatch | Reconcile when Figma export is available |
