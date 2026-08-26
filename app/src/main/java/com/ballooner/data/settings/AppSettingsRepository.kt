package com.ballooner.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.TextSizeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** [SettingsRepository] backed by [SharedPreferences]. */
@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ballooner_settings", Context.MODE_PRIVATE)

    override fun observeSettings(): Flow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(readSettings()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readSettings())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setDefaultFont(font: BalloonFont) {
        prefs.edit().putString(KEY_FONT, font.name).apply()
    }

    override suspend fun setHideFontSelector(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_FONT, hide).apply()
    }

    override suspend fun setTextSizeMode(mode: TextSizeMode) {
        prefs.edit().putString(KEY_TEXT_MODE, mode.name).apply()
    }

    private fun readSettings() = AppSettings(
        defaultFont = prefs.getString(KEY_FONT, null).toEnum(BalloonFont::valueOf) ?: BalloonFont.DEFAULT,
        hideFontSelector = prefs.getBoolean(KEY_HIDE_FONT, false),
        textSizeMode = prefs.getString(KEY_TEXT_MODE, null).toEnum(TextSizeMode::valueOf) ?: TextSizeMode.MANUAL,
    )

    private fun <T> String?.toEnum(parse: (String) -> T): T? =
        this?.let { runCatching { parse(it) }.getOrNull() }

    private companion object {
        const val KEY_FONT = "default_font"
        const val KEY_HIDE_FONT = "hide_font_selector"
        const val KEY_TEXT_MODE = "text_size_mode"
    }
}
