# Monager architecture (offline-only)

Single Gradle module `:app` (fastest builds, no cross-module overhead; split later only if needed).

```
app/src/main/java/com/monager/app/
  MonagerApp.kt, MainActivity.kt
  di/               AppContainer (manual, lazy DI; no Hilt -> faster cold start)
  core/             Money, AppDispatchers, utils
  data/local/       Room: entity/, dao/, MonagerDatabase
  data/repository/  thin repositories over DAOs
  domain/model/     @Immutable UI models
  ui/               theme/, components/ (shared 3D widgets), navigation/,
                    one package per screen: home, analytics, budgets, debts, settings, transaction
app/schemas/        exported Room schemas (commit them; used for migration tests)
```

Data flow: `Screen -> ViewModel (StateFlow) -> Repository -> DAO -> Room`. UI never touches DAOs.

Rules (details in /AGENTS.md):
- money = `Long`; aggregate in SQL; lists need `key` + `contentType`
- no blur / per-item shadow; hoist Brush/Path/formatters
- commit `schemas/`; never `fallbackToDestructiveMigration`
- no INTERNET permission (offline-only); backup/export via Storage Access Framework
- TODO next: launcher icon, ViewModels, Baseline Profile module, seed default categories/wallet
