package com.monager.app.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.monager.app.ui.analytics.BarUi
import com.monager.app.ui.analytics.SliceUi
import com.monager.app.ui.components.drawBudgetRing
import com.monager.app.ui.components.drawCashFlowBars
import com.monager.app.ui.components.drawCompareBars
import com.monager.app.ui.components.drawDailyBars
import com.monager.app.ui.components.drawDonut
import com.monager.app.ui.components.drawLimitSlider
import com.monager.app.ui.components.drawProgress
import com.monager.app.ui.components.drawSplitBar
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AGENTS.md: "no allocations in draw scope". Draws every chart many times on a real Compose Canvas and measures the
 * bytes the draw thread allocates per frame (JVM thread allocation counter). Long-lived objects (Stroke, lists,
 * CornerRadius for the shared radius) are created outside, exactly like the composables do with `remember`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DrawAllocationTest {
    private val density = Density(2.75f)
    private val size = Size(1080f, 600f)
    // Robolectric cannot build Compose's ImageBitmap, so wrap a plain Android canvas.
    private val canvas = Canvas(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(1080, 600, android.graphics.Bitmap.Config.ARGB_8888)))
    private val scope = CanvasDrawScope()

    private val palette = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Gray)
    private val slices = List(5) { SliceUi("c$it", 20, 100, 72f) }
    private val bars = List(12) { BarUi("t$it", 100, 50, .8f, .4f) }
    private val daily = List(31) { (it % 7) / 7f }
    private val stroke = Stroke(40f)
    private val roundStroke = Stroke(40f, cap = StrokeCap.Round)
    private val radius = CornerRadius(11f)

    // The Android unit-test classpath hides java.lang.management, so reach the JVM's per-thread allocation counter by reflection.
    private val threadBean: Any = Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null)!!
    private val allocatedBytes = Class.forName("com.sun.management.ThreadMXBean").getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
    private fun allocated(): Long = allocatedBytes.invoke(threadBean, Thread.currentThread().id) as Long

    private fun bytesPerDraw(block: DrawScope.() -> Unit): Long {
        repeat(300) { scope.draw(density, LayoutDirection.Ltr, canvas, size, block) } // warm up the JIT and any lazy caches
        val runs = 1_000
        val before = allocated()
        repeat(runs) { scope.draw(density, LayoutDirection.Ltr, canvas, size, block) }
        return (allocated() - before) / runs
    }

    /**
     * Anything above [LIMIT] per frame beyond the cost of the bare framework calls it makes would mean our code is
     * creating objects while drawing. Robolectric's Canvas shadow records every drawLine / drawCircle call in a history
     * list (that is test-environment noise, not app allocation), so those functions are compared with a control that
     * issues the same calls directly: [extra] = bytes of that control.
     */
    private fun assertCheap(name: String, bytes: Long, extra: Long = 0) {
        println("DRAW-ALLOC $name = $bytes bytes/frame (control $extra)")
        assertTrue("$name allocates $bytes bytes per draw (control $extra)", bytes <= extra + LIMIT)
    }

    private val threeLines: DrawScope.() -> Unit = {
        for (g in 1..3) drawLine(Color.Gray, androidx.compose.ui.geometry.Offset(0f, g * 10f), androidx.compose.ui.geometry.Offset(100f, g * 10f), 1f)
    }
    private val twoCircles: DrawScope.() -> Unit = {
        drawCircle(Color.Black, 5f, androidx.compose.ui.geometry.Offset(10f, 10f))
        drawCircle(Color.White, 5f, androidx.compose.ui.geometry.Offset(10f, 10f))
    }
    private val threeCircles: DrawScope.() -> Unit = {
        twoCircles()
        drawCircle(Color.Red, 3f, androidx.compose.ui.geometry.Offset(10f, 10f))
    }

    /** Sanity check of the measurement itself: creating a Stroke per draw (the classic mistake) must be detected. */
    @Test fun detectsAnAllocatingDraw() {
        val bytes = bytesPerDraw {
            repeat(5) { drawArc(Color.Red, 0f, 90f, false, androidx.compose.ui.geometry.Offset.Zero, Size(100f, 100f), style = Stroke(10f)) }
        }
        println("DRAW-ALLOC deliberately bad draw = $bytes bytes/frame")
        assertTrue("the test should flag per-draw Stroke allocation but saw only $bytes bytes", bytes > LIMIT)
    }

    @Test fun baselineEmptyDrawIsFree() = assertCheap("empty (framework overhead)", bytesPerDraw { })

    @Test fun donut() = assertCheap("donut", bytesPerDraw { drawDonut(slices, palette, Color.LightGray, 40f, stroke) })
    @Test fun donutEmpty() = assertCheap("donut empty", bytesPerDraw { drawDonut(emptyList(), palette, Color.LightGray, 40f, stroke) })
    @Test fun cashFlowBars() = assertCheap("cash-flow bars", bytesPerDraw { drawCashFlowBars(bars, Color.Green, Color.Red, Color.Gray, 8f, radius) }, extra = bytesPerDraw(threeLines))
    @Test fun splitBar() = assertCheap("home split bar", bytesPerDraw { drawSplitBar(true, .4f, Color.Gray, Color.Green, Color.Red) })
    @Test fun dailyBars() = assertCheap("home daily bars", bytesPerDraw { drawDailyBars(daily, Color.Red, Color.Gray) })
    @Test fun compareBars() = assertCheap("home compare bars", bytesPerDraw { drawCompareBars(.5f, 1f, true, true, Color.Gray, Color.Green) })
    @Test fun budgetRing() = assertCheap("budget ring", bytesPerDraw { drawBudgetRing(.7f, 40f, stroke, roundStroke, Color.Gray, Color.Green) })
    @Test fun progressWithThumb() = assertCheap("progress + thumb", bytesPerDraw { drawProgress(.6f, Color.Gray, Color.Green, true) }, extra = bytesPerDraw(twoCircles))
    @Test fun limitSlider() = assertCheap("limit slider", bytesPerDraw { drawLimitSlider(.6f, Color.Gray, Color.Green) }, extra = bytesPerDraw(threeCircles))

    private companion object {
        const val LIMIT = 16L  // one Stroke object is ~40 bytes, so a single per-draw allocation already fails
    }
}
