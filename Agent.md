---
name: monager-motorola-performance-ui
description: Rules for building the Monager expense-tracking app (Kotlin + Jetpack Compose + Room) so it looks premium (3D neo-skeuomorphic, frosted-glass style, emerald/cream palette) and stays smooth on budget Motorola phones such as the Moto G35. Use for any UI spec, Compose/Kotlin snippet, Room query, or architecture decision in this project.
---

# AGENT SKILL: Premium UI + Maximum Performance for Motorola (baseline: Moto G35)

## 1. OBJECTIVE & PRIORITY ORDER
Build a beautiful, tactile personal-finance app (**3D neo-skeuomorphism + frosted-glass look**) that holds a smooth 60fps (up to 120fps on simple screens) on budget/mid-range Motorola devices.

**When goals conflict, decide in this order:**
1. **Performance** (no jank, fast start, low RAM/battery) is a hard constraint, not a nice-to-have.
2. **Visual quality**: never fall back to flat/boring design; get the same look through cheaper techniques (see section 4).
3. Feature convenience.

If an effect cannot be made cheap, replace it with a visually equivalent cheap one. Do not ship it and hope.

---

## 2. HARDWARE PROFILE (design for the weakest target)
- **Baseline device:** Motorola Moto G35 5G. About 1080x2400, 120Hz IPS LCD, Unisoc T760 (octa-core, Mali-G57 GPU), 4-8GB RAM, Android 14+.
- **Same class:** Moto G-series (G15/G24/G34/G45/G55/G75) and Moto E-series. Treat 4GB RAM and a weak GPU as the floor.
- **Frame budget:** 16.6ms at 60fps, 8.3ms at 120fps. Target <=10ms CPU+GPU per frame on scroll-heavy screens.
- **Near-stock Android (Hello UI):** do not rely on vendor-specific APIs. Use only AndroidX and standard Android APIs.
- **Always verify on a real mid-range device or a throttled profile.** Emulator and flagship results are not proof.

### Bottlenecks and fixes
| Bottleneck | Fix |
|---|---|
| **GPU overdraw / fill-rate (Mali)** | No stacked full-screen translucent layers. One opaque background. Cap alpha-composited layers at 2-3 deep. No runtime `blur()` / `RenderEffect` on scrolling content. |
| **RAM** | Vector assets only (`ImageVector` / VectorDrawable). Avoid large bitmaps. Cap image caches. No heavy DI/reflection libraries. |
| **Main-thread blocking** | All Room, CSV/JSON export/import, and aggregation work off the main thread (`Dispatchers.IO` / `Default`). Never compute totals in composables. |
| **Cold start** | Lazy-init everything not needed for the first frame. Ship a **Baseline Profile**. Prefer simple manual DI or Koin/Hilt with lazy providers. No work in `Application.onCreate` beyond essentials. |
| **Thermal / battery** | No infinite animations on idle screens. Stop animations when off-screen or when the app is backgrounded. |

---

## 3. COMPOSE PERFORMANCE RULES (MANDATORY)
**State & recomposition**
- Hoist state; pass only the data each composable needs. Prefer immutable `data class` with `val` fields and `ImmutableList`/`@Immutable`/`@Stable` where compiler stability is unclear. Keep Strong Skipping enabled.
- Use `remember` / `derivedStateOf` for computed values. Never allocate lists, formatters, `Brush`, `Path`, `Paint`, or `Modifier` chains inside frequently recomposed scopes without `remember`.
- Collect flows with `collectAsStateWithLifecycle()`. Read fast-changing state (scroll offset, animation values) **in the draw/layout phase** (`Modifier.drawBehind`, `graphicsLayer { }`, lambda-based modifiers), not in composition.
- Create `NumberFormat` / `DateTimeFormatter` once (top-level or `remember`), never per item.

**Lists (Transactions, Budgets, Debts)**
- `LazyColumn` with `key = { it.id }` **and** `contentType = { ... }` for every list.
- Keep item composables flat and cheap. No nested scrollables, no `LazyColumn` inside `verticalScroll`.
- Large histories: use **Paging 3** or windowed queries (by month). Never load all rows.
- Group headers (by date) are computed in the ViewModel, not in the item.

**Animation**
- Animate only `graphicsLayer` properties (translation, scale, alpha) and draw-phase values. Avoid animating size/layout of large trees.
- Prefer short springs/tweens (150-300ms). Avoid `AnimatedContent` on big screens; scope it to small widgets.
- Respect the system "remove animations" setting.

**Build & release**
- Release builds with **R8 full mode + resource shrinking**, **Baseline Profile + Startup Profile**, `minSdk` as high as the product allows.
- Measure with **Macrobenchmark** (startup, scroll jank) and **JankStats**; check recomposition counts with Layout Inspector. Treat regressions as bugs.
- Keep the APK small: drop unused libraries, use `ImageVector`, limit font files (subset or 1-2 weights).

---

## 4. VISUAL SYSTEM THAT LOOKS EXPENSIVE BUT RENDERS CHEAP
**Palette:** rich emerald + warm cream, with one accent. Define in a single theme token set; no ad-hoc colors.

**3D neo-skeuomorphic depth (cheap)**
- Do not use `Modifier.shadow()` / large `elevation` on every list item (software-rendered blur shadows are expensive on Mali).
- Fake depth with **a cached drawing**:
  - raised surface = 2-stop vertical gradient fill + 1px top inner highlight (white ~35% alpha) + 1px bottom darker edge,
  - soft outer shadow = one pre-built shape drawable / `drawBehind` with `remember`ed `Brush` (offset rounded rect with low-alpha dark color, no blur),
  - pressed state = swap to the inverted gradient and reduce offset (no runtime shadow recalculation).
- Reserve true `shadow()` for **at most 1-3 static hero elements** (e.g., the balance card), and wrap them in `graphicsLayer` so they are cached.

**Frosted glass (fake it, do not blur it)**
- Surface = solid color at **85-92% opacity** over a simple gradient + 1px border (`#FFFFFF` at ~30% alpha) + subtle top highlight.
- Real background blur only for **one static, non-scrolling element** (e.g., a header) and only on devices where it benchmarks well; always provide the no-blur fallback.

**Gradients**
- 2-stop linear gradients only. No mesh/multi-stop radial backgrounds. Create `Brush` once via `remember`/top-level constants.

**Icons & typography**
- Use `ImageVector` (Material Symbols, Lucide/Phosphor exported as vectors) rather than font icons or PNGs. Keep one font family, 2-3 weights.

**Charts (Donut/Bar/Line)**
- Custom `Canvas` drawing. **No allocations in draw scope**: hoist `Path`, `Brush`, `Stroke`, and text layouts into `remember`. Precompute geometry in the ViewModel or `derivedStateOf`.
- Draw once; animate only a single progress float in the draw phase. Limit data points (downsample long ranges).

---

## 5. DATA LAYER (KOTLIN + ROOM)
- **Indexes:** `@Index` on `timestamp`, `categoryId`, `walletId`, and composite indexes for common filters (e.g., `(walletId, timestamp)`).
- **Aggregate in SQL**: `SUM`, `GROUP BY category/month` in DAO queries returning small result objects. Never sum in Kotlin loops over full tables.
- **Flows:** DAO returns `Flow`; use `distinctUntilChanged()`, `map` to UI models on `Dispatchers.Default`, and `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`.
- **Writes:** wrap multi-row operations in a single `@Transaction`; batch inserts for imports. Keep WAL enabled (Room default).
- **Money:** store amounts as `Long` minor units (e.g., VND as whole dong), never `Double`.
- **Preferences:** DataStore (async). No `SharedPreferences.commit()` on main thread.
- **Export/Backup:** stream CSV/JSON to a file on `Dispatchers.IO`; never build giant strings in memory.
- **Zero-allocation drawing:** no object creation in `onDraw`/draw scope; reuse `Paint`/`Path`.

---

## 6. SCREEN SET & CONSISTENCY
Home, Analytics, Budgets, Debts, Settings (plus Add/Edit transaction). Keep one shared component library (`MonagerCard`, `MonagerButton`, `MonagerChip`, `MonagerTopBar`) so depth, radius, spacing, and colors are identical on all screens. Use skeleton/empty states that are static (no shimmer on lists).

---

## 7. AGENT OUTPUT CHECKLIST
When generating UI specs, Figma prompts, or code for this project, always:
1. **Keep the premium look**: 3D tactile buttons, raised cards, emerald/cream palette. Never "simplify" to flat design just to save performance; use the cheap techniques in section 4 instead.
2. **Include a short "Performance notes" block**: what is cached, what is read in the draw phase, list keys/contentType, which thread does the work.
3. **Self-check before answering:** no `blur()` on scrolling content, no per-item `shadow()`, no allocations in draw/composition hot paths, no heavy work on the main thread, list keys present.
4. **Keep screens synchronized** (shared components and tokens across all screens).
5. **State the measurement plan** for non-trivial features (which screen to benchmark, target: no frames >16ms during scroll on a Moto G35-class device).
