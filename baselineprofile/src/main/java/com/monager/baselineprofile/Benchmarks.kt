package com.monager.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run on the `benchmarkRelease` variant: ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * Each scenario runs with no compilation (worst case) and with the baseline profile applied.
 */
@RunWith(AndroidJUnit4::class)
class Benchmarks {
    @get:Rule val rule = MacrobenchmarkRule()

    private fun run(mode: CompilationMode, startup: StartupMode, metrics: List<androidx.benchmark.macro.Metric>, block: MacrobenchmarkScope.() -> Unit, setup: MacrobenchmarkScope.() -> Unit = {}) =
        rule.measureRepeated(
            packageName = PACKAGE, metrics = metrics, compilationMode = mode, startupMode = startup,
            iterations = 8, setupBlock = setup, measureBlock = block,
        )

    private val none = CompilationMode.None()
    private val profile = CompilationMode.Partial(BaselineProfileMode.Require)

    // Cold start: seed once in setup (first iteration), then measure a plain launch.
    @Test fun startupNoCompilation() = run(none, StartupMode.COLD, listOf(StartupTimingMetric()), { launch(false) }, { })
    @Test fun startupBaselineProfile() = run(profile, StartupMode.COLD, listOf(StartupTimingMetric()), { launch(false) }, { })

    @Test fun homeScrollNoCompilation() = run(none, StartupMode.WARM, listOf(FrameTimingMetric()), { scrollHome() }, { launch(true) })
    @Test fun homeScrollBaselineProfile() = run(profile, StartupMode.WARM, listOf(FrameTimingMetric()), { scrollHome() }, { launch(true) })

    @Test fun transactionsScrollBaselineProfile() =
        run(profile, StartupMode.WARM, listOf(FrameTimingMetric()), { openTransactionsAndScroll() }, { launch(true) })

    @Test fun openReportsBaselineProfile() =
        run(profile, StartupMode.WARM, listOf(FrameTimingMetric()), { openReports() }, { launch(true) })
}
