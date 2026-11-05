package com.monager.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until

const val PACKAGE = "com.monager.app"
private const val TIMEOUT = 8_000L

/** Cold-launches the app; with [seed] the app fills an empty DB with demo data first (see PerfTools). */
fun MacrobenchmarkScope.launch(seed: Boolean) {
    pressHome()
    startActivityAndWait { intent -> if (seed) intent.putExtra("perf_seed", true) }
    // Home is ready once the first section title is on screen.
    device.wait(Until.hasObject(By.textContains("Báo cáo tháng này")), TIMEOUT)
}

/** UiAutomator objects go stale when Compose recomposes; look the element up again on every attempt. */
private inline fun retry(times: Int = 4, block: () -> Unit) {
    repeat(times) {
        try { block(); return } catch (_: StaleObjectException) { Thread.sleep(250) }
    }
}

private fun MacrobenchmarkScope.clickText(text: String) {
    device.wait(Until.hasObject(By.text(text)), TIMEOUT)
    retry { device.findObject(By.text(text))?.click() }
    device.waitForIdle()
}

private fun MacrobenchmarkScope.flingList(times: Int = 3) {
    for (dir in listOf(Direction.DOWN, Direction.UP)) {
        repeat(times) {
            retry {
                val list = device.findObject(By.scrollable(true)) ?: return@retry
                list.setGestureMargin(device.displayWidth / 10)
                list.fling(dir)
            }
        }
        device.waitForIdle()
    }
}

fun MacrobenchmarkScope.scrollHome() = flingList()

fun MacrobenchmarkScope.openTransactionsAndScroll() {
    clickText("Giao dịch")
    device.wait(Until.hasObject(By.text("Tháng này")), TIMEOUT)
    flingList(4)
}

fun MacrobenchmarkScope.openReports() {
    clickText("Xem báo cáo")
    device.wait(Until.hasObject(By.text("Theo dõi dòng tiền của bạn")), TIMEOUT)
    device.waitForIdle()
}

fun MacrobenchmarkScope.visitTabs() {
    for (label in listOf("Giao dịch", "Hạn mức", "Tài khoản", "Trang chủ")) clickText(label)
}
