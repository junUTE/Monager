package com.monager.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.monager.app.core.CurrencyMark
import com.monager.app.core.MoneyFormat
import com.monager.app.core.NumberStyle
import com.monager.app.data.prefs.SettingsStore
import com.monager.app.data.prefs.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsStoreTest {
    @Test fun moneyFormatAndThemeArePersistedAndRestoredByAFreshStore() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(ctx)
        assertEquals(MoneyFormat(NumberStyle.DOT, CurrencyMark.DONG), store.moneyFormat.first()) // defaults
        assertEquals(ThemeMode.SYSTEM, store.themeMode.first())

        store.setNumberStyle(NumberStyle.COMMA)
        store.setCurrencyMark(CurrencyMark.VND)
        store.setThemeMode(ThemeMode.DARK)

        val reopened = SettingsStore(ctx) // what the app does after being killed and started again
        assertEquals(MoneyFormat(NumberStyle.COMMA, CurrencyMark.VND), reopened.moneyFormat.first())
        assertEquals(ThemeMode.DARK, reopened.themeMode.first())
    }
}
