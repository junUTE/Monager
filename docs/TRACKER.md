# Monager – Progress Tracker

Legend: `[x]` done · `[~]` in progress · `[ ]` todo · `[!]` blocked. Update this file with every PR.
Last updated: 2026-11-21 (v0.5.1 UI update)

**v0.5.1 (code 14):** calculator shows thousands separators inside expressions; tactile numpad (deeper dip, tint flash, key-click haptic, amount "pop"); one shared press system (`Press.kt`: `rememberPress`, `pressScale`, `tactile`) so every button sinks/springs and ripples follow rounded corners; chart entrance animations (trend, compare bars, donut, cash flow, budget ring, progress bars — draw-phase progress, saved per item, 0 B/frame); category drill-down screen (from Home top spending, Reports, Transactions filter) with sub-category split and Newest/Largest sort; Transactions: Reports shortcut + category filter chips; Reports redesign (net cash-flow hero, prominent tappable categories, diverging cash-flow chart with tap-to-inspect); Budgets redesign (status, per-day allowance, over-100% second ring lap and overflow bars); 44 new category icons, picker grouped by theme, emoji fallback to vectors on old fonts.

## Summary

| Milestone | Status | Done / Total |
|-----------|--------|--------------|
| M0 Foundation | done | 8 / 8 |
| M1 Data layer | done | 6 / 6 |
| M2 Add Transaction | done | 9 / 9 |
| M3 Home | done except the Moto G35 jank check (see docs/DEVICE_TESTING.md) | 5 / 6 |
| M4 Categories | done | 6 / 6 |
| M5 Budgets & Savings | done | 7 / 7 |
| M6 Debts | done | 7 / 7 |
| M7 Analytics | done | 7 / 7 |
| M8 Settings & Data | done | 6 / 6 |
| M9 Performance | in progress | 3 / 6 |
| M10 Release | not started | 0 / 5 |

## M0 Foundation
- [x] Repo skeleton, Gradle, version catalog (pushed to GitHub)
- [x] Room v1 draft schema + DAOs
- [x] Build verified from CLI (Gradle 8.9 + Studio JBR): `assembleDebug` OK, debug APK 10.8 MB. Not yet run on a device
- [x] Dependencies at the latest stable versions (2026-11-05): Gradle 9.8.2, AGP 9.5.0 (built-in Kotlin, so the `kotlin-android` plugin is gone), Kotlin 2.4.21, KSP 2.3.13, Compose BOM 2026.09.00, Room 2.8.5, lifecycle 2.11.0, activity 1.13.0, core-ktx 1.19.1, DataStore 1.3.0, coroutines 1.11.0, JankStats 1.0.0, Macrobenchmark / Baseline Profile plugin 1.5.0, Robolectric 4.16.1. compileSdk 37, targetSdk 36 (was 35). `navigation-compose` and `room-paging` were removed as unused. All JVM tests pass, the R8 release build works and the app was smoke-tested on an API 35 emulator; it has not run on an API 36 device, where the targetSdk 36 behaviour changes apply
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
- [x] `CalcEngine` (+ − × ÷, "k" suffix) with unit tests — 12 tests; the numpad now has a "k" key (50k -> 50.000, once per number, only after a digit)
- [x] 3D numpad UI with Backspace and C — 5 rows: C ÷ × ⌫ / 7-9 − / 4-6 + / 1-3 = / 0 000 k
- [x] Live result preview (e.g. 50k + 30k → 80.000 đ) — expression line + big result
- [x] 2-level category picker (parent → child) — from Room
- [x] Date + time pickers — Material date picker and a 24h time picker; past, today and future dates (future = planned, shown under "Tương lai", not counted in the balance until its day). The chosen date and time are saved as-is
- [x] Note field
- [x] Wallet selector — intentionally REMOVED from the UI (user does not use wallets); the first wallet is used implicitly. Wallet data stays in the DB and in backups
- [x] Save + validation + haptic feedback (LongPress haptic on save)

## M3 Home (F2)
- [x] Balance header with show/hide eye, month report card (spent / income / daily bars), week-or-month compare card — real data from Room (redesigned 2026-11-05 to match the reference screens)
- [x] Quick actions replaced by the center "+" button; Transfer has no UI (no wallets in use)
- [x] Budget progress now lives only on the Budgets tab (removed from Home in the redesign)
- [x] Top spending (week/month) and recent transactions (5 latest, one card); full list + date groups in the new Transactions tab (last month / this month / future)
- [x] Income green (+) / expense red (−), 40x40dp vector icons
- [ ] Jank check on Moto G35 — needs the real device (the emulator cannot give meaningful frame timings and its Perfetto traces fail); step-by-step in docs/DEVICE_TESTING.md

## M4 Categories (F6)
- [x] List parents with children (Room, expandable)
- [x] Add / edit / delete category and subcategory
- [x] Reorder via Lên/Xuống in the edit sheet (no drag-and-drop)
- [x] Icon + color picker
- [x] Delete rules: blocked with a message when the category or a child has transactions (no reassign flow)
- [x] Vector icon set — 48 icons selectable (24 added 2026-11-05: finance, bills, entertainment, transport, shopping), 69 total in MIcon; the picker scrolls inside a fixed-height area

## M5 Budgets & Savings (F4)
- [x] Total monthly budget (set/edit/remove from the screen; Home strip reads it)
- [x] Per-category budget (parent categories; spent includes subcategories)
- [x] Draggable thumb slider (`LimitSlider`) in the budget edit sheet: tap or drag to set the limit (snaps to 100k / 500k), typing an exact amount still works. Progress bars on the list show spent vs limit
- [x] Red warning at ≥90% (bar, percent and overview ring)
- [x] Savings vault: create, edit, delete (with confirmation; deleting also removes its deposits so the money returns to the balance, in one DB transaction)
- [x] Vault progress % (from SAVING transactions)
- [x] Budget rollover: when the new month has no budgets but last month had, a card offers "reuse last month" (copies limits only; never overwrites existing ones). Carrying unspent money over is NOT implemented

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
- [x] Zero-allocation draw: all chart drawing (donut, cash-flow bars, Home bars, budget ring, progress, limit slider) is plain `DrawScope` functions in `ui/components/ChartDraw.kt`, covered by `DrawAllocationTest`: it draws each one 1,000 times on a real Skia canvas (Robolectric native graphics) and reads the JVM per-thread allocation counter. Result: 0 bytes per frame for every chart; a deliberately bad draw (a `Stroke` per call) is caught at ~160 bytes, so the check is sensitive. Not measured with the Android Studio allocation profiler on a device

## M8 Settings & Data (F7)
- [x] Theme: Light / Dark / System (saved in DataStore; chosen in the Account tab)
- [x] Export JSON (full backup of all 7 tables, via the system file picker)
- [x] Export CSV (UTF-8 with BOM for Excel; formula-looking text is neutralised) and Excel .xlsx (hand-written writer, amounts stored as numbers)
- [x] Import: JSON restore (validated, confirm dialog, atomic replace) + CSV/Excel append (auto-detect, header-name or positional columns, creates missing categories/wallets/vaults, skips duplicates, reports bad rows)
- [x] Display format setting (Account tab): thousands separator `1.234.567` or `1,234,567` and currency mark `₫` / `đ` / `VND`, saved in DataStore and applied app-wide (`Money` holds Compose state, so every screen updates at once). Display only: stored amounts, CSV/Excel/JSON exports and imports never change. The app stays VND-only (amounts are whole dong); other currencies would need minor-unit support
- [x] About + data size info (offline note, transaction count, DB size)

## M9 Performance hardening
- [x] Baseline Profile + Startup Profile: generated on an API 35 emulator (`./gradlew :app:generateBaselineProfile`), committed in `app/src/release/generated/baselineProfiles/` (16.6k rules, ~1.3k from the app); `baseline.prof` is packaged in the release APK
- [~] Macrobenchmark: scenarios written in `:baselineprofile` (cold start, Home scroll, Transactions scroll, open Reports; each with/without profile) but NOT measured. The emulator's Perfetto trace fails with `FTRACE_STATUS_PARTIAL_PAGE_READ`, and emulator numbers would not represent a Moto G35 anyway. Run on a real device: `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`
- [x] JankStats: `PerfTools` logs janky frames (tag `MonagerJank`) in every build type except release. Demo data for benchmarks: launch with `--ez perf_seed true` (empty DB only, ~1500 tx). R8 strips PerfTools + JankStats from release (`BuildConfig.PERF_TOOLS` = false)
- [ ] Recomposition audit (Layout Inspector) — needs Android Studio; not done
- [x] R8 full mode + shrink verified: release APK 1.39 MB (debug 14.4 MB), smoke-tested on the emulator (Home, Transactions, Account, Add sheet: no crash)
- [ ] Test on real Moto G35 (record results below) — no device available

## M10 Release
- [ ] Launcher icon + splash
- [ ] Signing config (keystore kept out of git)
- [ ] QA checklist pass (light/dark, small screens)
- [ ] Versioning + changelog
- [ ] Release AAB/APK build

## Performance log (fill on device)

| Date | Build | Device | Cold start | Home scroll jank % | APK size | Notes |
|------|-------|--------|-----------|--------------------|----------|-------|
| 2026-11-05 | release (R8, baseline profile) | emulator API 35 (NOT representative) | 4.1-5.0 s first launch incl. DB creation | not measured | 1.39 MB | debug JankStats on emulator: ~6% janky frames, software GPU; ignore, re-measure on Moto G35 |

## Decisions / blockers

- 2026-11-04: Figma Make link unreadable (403). Mockup built from feature list; awaiting exported frames to reconcile.

## Review pass 2026-11-05 (M0–M8)
- [x] Month/week windows now derive from `AppContainer.today` (refreshed in `MainActivity.onStart`), so Home, Transactions and Budgets roll over at midnight / month change instead of freezing at ViewModel creation
- [x] Balance counts only transactions up to the end of today (`observeBalanceUntil`); planned/future ones appear in the "Tương lai" tab
- [x] Rules audit vs AGENTS.md: no `shadow()`/`blur()`, no `Double` money, all lists keyed with contentType, `collectAsStateWithLifecycle` everywhere (fixed Debts payment history), heavy mapping on `Dispatchers.Default`
- [x] Release build (R8) smoke-tested on an API 35 emulator: Home, Transactions, Account, Add sheet, date picker, future transaction flow
- [x] Optional items done 2026-11-05: draggable limit slider, vault delete, budget rollover (copy limits), 24 more icons, time picker, date pickers for debt due date and the custom report range
- [x] Fixed while testing on the emulator: the bottom navigation covered the buttons of bottom sheets on tab screens (it now hides while a sheet is open); system Back closed the whole screen/app instead of just a sheet (SimpleSheet and CategoryEditSheet now handle Back); date/time dialogs now use the app's green palette
- [ ] Still open by decision/need: carrying unspent budget over; due-date reminders (explicitly skipped); everything that needs a real device (Moto G35 jank / benchmarks, file picker, Excel files in real Excel/Sheets, API 36 behaviour); regenerate the Baseline Profile before release (it predates the latest refactors)

## Test status
- 113 JVM unit tests pass (`./gradlew :app:testDebugUnitTest`): CalcEngine 12, AmountInput 17, DebtMath 6, Periods 10, Money 4, Database/Repository 24, SettingsStore 1, Backup/Import/Export 14, Codecs 15, DrawAllocation 10 (Robolectric).
- No UI tests. Manual smoke tests only on an API 35 emulator (see M9 notes); never on a real Moto G35.

## Next up
- UI redesign 2026-11-05 (commit 49ee042): Home = balance + month report + week/month compare + top spending + recent; new Transactions tab (last month / this month / future); tabs are Home, Transactions, Budgets, Account; Reports and Debts are sub-screens (Home "Xem báo cáo", Account); the wallet field was removed from the Add sheet (the first wallet is used implicitly). Home search/bell icons from the reference were intentionally not added (no feature behind them).
- M9 (finish on a real device), then M10 Release. Not yet device-tested: file picker flows (export/import) and the Excel files opening in real Excel/Sheets.
- All five screens now read Room; the sample-data file is deleted.
- Debug APK grew 10.8 -> 14.4 MB across M4-M7 (debug is unminified); measure the release build in M9.
- Due-date reminders ("nhắc trước 3 ngày" in the mockup) are NOT implemented; the note was removed from the UI so it does not promise it. Needs a notification permission + WorkManager; decide in M8/M10.
- M4 is covered by repository tests (reorder, delete rules, two-level limit); the Categories screen/ViewModel itself has no UI test and has not been run on a device.
