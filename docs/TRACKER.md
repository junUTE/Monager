# Monager – Progress Tracker

Legend: `[x]` done · `[~]` in progress · `[ ]` todo · `[!]` blocked. Update this file with every PR.
Last updated: 2026-11-04

## Summary

| Milestone | Status | Done / Total |
|-----------|--------|--------------|
| M0 Foundation | in progress | 2 / 7 |
| M1 Data layer | not started | 0 / 6 |
| M2 Add Transaction | not started | 0 / 9 |
| M3 Home | not started | 0 / 6 |
| M4 Categories | not started | 0 / 6 |
| M5 Budgets & Savings | not started | 0 / 7 |
| M6 Debts | not started | 0 / 7 |
| M7 Analytics | not started | 0 / 7 |
| M8 Settings & Data | not started | 0 / 6 |
| M9 Performance | not started | 0 / 6 |
| M10 Release | not started | 0 / 5 |

## M0 Foundation
- [x] Repo skeleton, Gradle, version catalog (pushed to GitHub)
- [x] Room v1 draft schema + DAOs
- [ ] Verify build on Android Studio, bump dependency versions
- [ ] Design tokens (light + dark), typography
- [ ] Shared components: `RaisedCard`, `RaisedButton`, `Chip`, `TopBar`, bottom nav
- [ ] Icon registry + first 20 vector icons
- [ ] Seed default wallet + categories on first run

## M1 Data layer
- [ ] Schema v1 final (parentId, sortOrder, vaults, debt_payments, SAVING/TRANSFER)
- [ ] DAOs + repositories for all entities
- [ ] SQL aggregates: totals by period, by category, monthly series
- [ ] Debt payment transaction (`@Transaction`)
- [ ] In-memory DAO tests
- [ ] Export schema JSON committed

## M2 Add Transaction (F1)
- [ ] Mode switch Expense / Income / Saving
- [ ] `CalcEngine` (+ − × ÷, "k" suffix) with unit tests
- [ ] 3D numpad UI with Backspace and C
- [ ] Live result preview (e.g. 50k + 30k → 80.000 đ)
- [ ] 2-level category picker (parent → child)
- [ ] Date/time picker
- [ ] Note field
- [ ] Wallet/account selector
- [ ] Save + validation + haptic feedback

## M3 Home (F2)
- [ ] Hero 3D balance card (total, month income, month expense)
- [ ] Quick actions (+Expense, +Income, Transfer)
- [ ] Mini budget progress bar
- [ ] Recent transactions timeline (Today / Yesterday / date headers)
- [ ] Income green (+) / expense red (−), 40x40dp vector icons
- [ ] Jank check on Moto G35

## M4 Categories (F6)
- [ ] List parents with children
- [ ] Add / edit / delete category and subcategory
- [ ] Reorder (drag or move up/down)
- [ ] Icon + color picker
- [ ] Delete rules (reassign or block if used)
- [ ] Rich vector icon set (Lucide/Phosphor)

## M5 Budgets & Savings (F4)
- [ ] Total monthly budget
- [ ] Per-category budget
- [ ] 3D progress bar with thumb handle
- [ ] Red warning at ≥90%
- [ ] Savings vault CRUD
- [ ] Vault progress % (from SAVING transactions)
- [ ] Budget rollover to next month (optional)

## M6 Debts (F5)
- [ ] Tabs: Borrowed / Lent
- [ ] Debt detail (person, total, paid/collected, due date)
- [ ] Add / edit debt
- [ ] Partial pay / collect action
- [ ] Optional linked transaction
- [ ] Overdue highlight
- [ ] Debt math tests

## M7 Analytics (F3)
- [ ] Period filter Week / Month / Year / Custom
- [ ] Canvas donut chart (% + total in center)
- [ ] Cash-flow bar chart (income vs expense)
- [ ] Ranked category breakdown list
- [ ] Empty/loading states
- [ ] Chart geometry precomputed off main thread
- [ ] Zero-allocation draw verified

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
