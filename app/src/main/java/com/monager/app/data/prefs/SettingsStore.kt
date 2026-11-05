package com.monager.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.monager.app.core.CurrencyMark
import com.monager.app.core.MoneyFormat
import com.monager.app.core.NumberStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Small key-value settings (async DataStore: never blocks the main thread). */
class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val numberStyleKey = stringPreferencesKey("number_style")
    private val currencyMarkKey = stringPreferencesKey("currency_mark")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeKey] = mode.name }
    }

    /** How amounts are displayed (separator style + currency mark). Display only: stored amounts and exports never change. */
    val moneyFormat: Flow<MoneyFormat> = context.settingsDataStore.data.map { prefs ->
        MoneyFormat(
            NumberStyle.entries.firstOrNull { it.name == prefs[numberStyleKey] } ?: NumberStyle.DOT,
            CurrencyMark.entries.firstOrNull { it.name == prefs[currencyMarkKey] } ?: CurrencyMark.DONG,
        )
    }.distinctUntilChanged()

    suspend fun setNumberStyle(style: NumberStyle) {
        context.settingsDataStore.edit { it[numberStyleKey] = style.name }
    }

    suspend fun setCurrencyMark(mark: CurrencyMark) {
        context.settingsDataStore.edit { it[currencyMarkKey] = mark.name }
    }
}
