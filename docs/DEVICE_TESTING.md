# Device testing (Moto G35 class)

Things that cannot be verified on the emulator. Do these on a real mid-range phone with USB debugging on.
Target (docs/PLAN.md): no frame over 16 ms while scrolling Home / Transactions, cold start <= 1.2 s on a release build.

## 1. Jank on Home and Transactions (closes the M3 item)
1. Build and install a profile-compiled release-like app: `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`
   (runs `Benchmarks.kt`: cold start, Home scroll, Transactions scroll, Reports open, each with and without the Baseline Profile).
2. Read the result in `baselineprofile/build/outputs/connected_android_test_additional_output/` (JSON) or in the Studio test results.
   Look at `frameDurationCpuMs` P90/P99 and `frameOverrunMs`, and `timeToInitialDisplayMs` for start-up.
3. Copy the numbers into the "Performance log" table in docs/TRACKER.md (date, device, cold start, jank %, APK size).

## 2. Quick manual check with JankStats (debug build)
- `./gradlew :app:installDebug`, then `adb shell am start -n com.monager.app.debug/com.monager.app.MainActivity --ez perf_seed true`
  (fills an empty database with ~1,500 demo transactions).
- Scroll Home, Transactions and Reports, then `adb logcat -s MonagerJank`: it prints every janky frame and a summary every 300 frames.
- Debug builds are slower than release; use the benchmark above for the official number.

## 3. Other device-only checks
- Export / import through the system file picker (JSON, CSV, Excel) and open the .xlsx in real Excel or Google Sheets.
- Light and dark theme, small screens, system Back inside every sheet.
- Release APK: `./gradlew :app:assembleRelease`, sign it, install, check start-up and the three tabs.
