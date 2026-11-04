# Monager – Progress Tracker

Legend: `[x]` done · `[~]` in progress · `[ ]` todo · `[!]` blocked. Update this file with every PR.
Last updated: 2026-11-04 (M0–M3 progress; see sections below)

## Summary

| Milestone | Status | Done / Total |
|-----------|--------|--------------|
| M0 Foundation | in progress | 7 / 8 |
| M1 Data layer | done | 6 / 6 |
| M2 Add Transaction | in progress | 8 / 9 |
| M3 Home | in progress | 5 / 6 |
| M4 Categories | in progress | 5 / 6 |
| M5 Budgets & Savings | in progress | 4 / 7 |
| M6 Debts | done | 7 / 7 |
| M7 Analytics | in progress | 6 / 7 |
| M8 Settings & Data | not started | 0 / 6 |
| M9 Performance | not started | 0 / 6 |
| M10 Release | not started | 0 / 5 |

## M0 Foundation
- [x] Repo skeleton, Gradle, version catalog (pushed to GitHub)
- [x] Room v1 draft schema + DAOs
- [x] Build verified from CLI (Gradle 8.9 + Studio JBR): `assembleDebug` OK, debug APK 10.8 MB. Not yet run on a device
- [ ] Bump dependency versions to latest stable (still the versions I chose from memory)
- [x] Design tokens (light + dark), typography — ui/theme/Theme.kt
- [x] Shared components: `RaisedCard`, `RaisedButton`, `Chip`, `TopBar`, bottom nav — raised()/RaisedBox, SegmentedTabs, AppProgress, ScreenHeader, bottom nav
- [x] Icon registry + first 20 vector icons — 40 icons, `MIcon.fromKey`
- [x] Seed default wallet + categories on first run — SeedCallback (wallet, vault, 8 parents + 7 subcategories)

## M1 Data layer
- [x] Schema v1 final (parentId, sortOrder, vaults, debt_payments, SAVING/TRANSFER)
- [x] DAOs + repositories for all entities
- [x] SQL aggregates: totals by period, by category, monthly series — totals, by-parent roll-up, time buckets, balance, budget spent
- [x] Debt payment transaction (`@Transaction`) — DebtRepository.pay, validated against remaining
- [x] In-memory DAO tests — 15 Robolectric tests on JVM, all pass
- [x] Export schema JSON committed — app/schemas/.../1.json

## M2 Add Transaction (F1)
- [x] Mode switch Expense / Income / Saving
- [x] `CalcEngine` (+ − × ÷, "k" suffix) with unit tests — 12 tests; "k" supported by engine, no k key on numpad yet
- [x] 3D numpad UI with Backspace and C — 5 rows: C ÷ × ⌫ / 7-9 − / 4-6 + / 1-3 = / 000 0
- [x] Live result preview (e.g. 50k + 30k → 80.000 đ) — expression line + big result
- [x] 2-level category picker (parent → child) — from Room
- [~] Date/time picker — tap cycles Hôm nay / Hôm qua / 2 ngày trước; real picker + time still todo
- [x] Note field
- [x] Wallet/account selector — tap cycles wallets (single seeded wallet for now)
- [x] Save + validation + haptic feedback — saves to Room with validation; haptic not done

## M3 Home (F2)
- [x] Hero 3D balance card (total, month income, month expense) — real data from Room
- [x] Quick actions (+Expense, +Income, Transfer) — Transfer button has no action yet
- [x] Mini budget progress bar — reads the overall monthly budget; no UI to set one yet
- [x] Recent transactions timeline (Today / Yesterday / date headers) — row-level date labels, 20 latest
- [x] Income green (+) / expense red (−), 40x40dp vector icons
- [ ] Jank check on Moto G35

## M4 Categories (F6)
- [x] List parents with children (Room, expandable)
- [x] Add / edit / delete category and subcategory
- [x] Reorder via Lên/Xuống in the edit sheet (no drag-and-drop)
- [x] Icon + color picker
- [x] Delete rules: blocked with a message when the category or a child has transactions (no reassign flow)
- [~] Vector icon set — 24 icons selectable, 40 total in MIcon; the "rich" Lucide/Phosphor set is still to import

## M5 Budgets & Savings (F4)
- [x] Total monthly budget (set/edit/remove from the screen; Home strip reads it)
- [x] Per-category budget (parent categories; spent includes subcategories)
- [~] 3D progress bar with thumb handle — drawn with thumb, but not draggable; limits are set by typing the amount
- [x] Red warning at ≥90% (bar, percent and overview ring)
- [~] Savings vault: create, read, update done; delete deferred (savings already left the wallet balance)
- [x] Vault progress % (from SAVING transactions)
- [ ] Budget rollover to next month (optional)

## M6 Debts (F5)
- [x] Tabs: Borrowed / Lent
- [x] Debt detail sheet (totals, note, payment history)
- [x] Add / edit debt (total cannot drop below amount already paid; due date typed as dd/MM/yyyy, no date picker yet)
- [x] Partial pay / collect action (amount, half / all shortcuts)
- [x] Optional linked transaction (checkbox; writes INCOME or EXPENSE into the first wallet)
- [x] Overdue highlight (red date + days overdue; settled debts sink to the bottom)
- [x] Debt math tests (DebtMath 6 + repository tests for edit, delete, expense/income direction)

## M7 Analytics (F3)
- [x] Period filter Week / Month / Year / Custom (prev/next navigation; custom range typed as dd/MM/yyyy)
- [x] Canvas donut chart (total + period label in the center; top 4 categories + "Khác")
- [x] Cash-flow bar chart (income vs expense per day / week / month bucket)
- [x] Ranked category breakdown list (percent sums to exactly 100, children roll up to parent) + change vs previous period
- [x] Empty/loading states
- [x] Chart geometry precomputed off main thread (AnalyticsViewModel on Dispatchers.Default)
- [~] Zero-allocation draw: code-reviewed (Stroke/CornerRadius remembered, sweeps and fractions precomputed) but NOT profiled — verify in M9 with the Android Studio profiler

## M8 Settings & Data (F7)
- [ ] Theme: Light / Dark / System
- [ ] Export JSON (full)
- [ ] Export CSV (transactions)
- [ ] Import JSON with validation + transaction
- [ ] Currency / locale formatting setting
- [ ] About + data size info

## M9 Performance hardening
- [ ] Baseline Profile + Startup Profile
- [ ] Macrobenchmark: cold start, Home scroll, Analytics open
- [ ] JankStats in debug builds
- [ ] Recomposition audit (Layout Inspector)
- [ ] R8 full mode + shrink verified, APK size recorded
- [ ] Test on real Moto G35 (record results below)

## M10 Release
- [ ] Launcher icon + splash
- [ ] Signing config (keystore kept out of git)
- [ ] QA checklist pass (light/dark, small screens)
- [ ] Versioning + changelog
- [ ] Release AAB/APK build

## Performance log (fill on device)

| Date | Build | Device | Cold start | Home scroll jank % | APK size | Notes |
|------|-------|--------|-----------|--------------------|----------|-------|
| – | – | – | – | – | – | – |

## Decisions / blockers

- 2026-11-04: Figma Make link unreadable (403). Mockup built from feature list; awaiting exported frames to reconcile.

## Front-end (Compose) status – added 2026-11-04
Source of truth: Figma Make export in `tmp/` (git-ignored). UI is static (sample data in `ui/sample/SampleData.kt`); **not yet compiled or run on a device**.

- [x] Theme tokens light + dark (`ui/theme/Theme.kt`)
- [x] 40 vector icons from the mockup as in-code ImageVectors (`ui/components/MIcon.kt`)
- [x] Cheap Neo-3D: `raised()` / `RaisedBox` (no blur, no shadow())
- [x] Shared components: header, segmented tabs, progress (Canvas), cards
- [x] Home, Stats (Canvas donut + bars), Budgets & Savings, Debts, Categories, Icon picker, Add Transaction sheet, bottom nav with center FAB, dark toggle
- [ ] First compile + run on Moto G35; fix any build errors
- [ ] Add Transaction: calculator operators (+ − × ÷) — mockup only has digits
- [ ] Drag-to-reorder categories, thumb dragging on budget bars (visual only for now)
- [ ] Replace SampleData with ViewModels + Room
- [ ] Persist theme in DataStore

## Test status
- 60 JVM unit tests pass (`./gradlew :app:testDebugUnitTest`): CalcEngine 12, AmountInput 12, DebtMath 6, Periods 10, Database/Repository 20 (Robolectric).
- No UI tests, no device/emulator run yet.

## Next up
- M8 Settings & Data (theme persistence, JSON/CSV backup and restore). M5 leftovers: rollover (optional), vault delete, draggable thumb.
- All five screens now read Room; the sample-data file is deleted.
- Debug APK grew 10.8 -> 14.4 MB across M4-M7 (debug is unminified); measure the release build in M9.
- Due-date reminders ("nhắc trước 3 ngày" in the mockup) are NOT implemented; the note was removed from the UI so it does not promise it. Needs a notification permission + WorkManager; decide in M8/M10.
- M4 is covered by repository tests (reorder, delete rules, two-level limit); the Categories screen/ViewModel itself has no UI test and has not been run on a device.
