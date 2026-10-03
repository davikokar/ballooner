package com.ballooner.data.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * The language the app is read in, chosen in Settings rather than taken from the device.
 *
 * The choice is applied by wrapping every context the app builds from
 * ([android.app.Application] and the activity), so a change needs the activity recreating and
 * nothing else.
 *
 * Deliberately not [Resources.updateConfiguration]: it mutates a cached `Resources` the framework
 * is free to replace at any time, which makes the choice stop sticking.
 */
object LocaleHelper {

    /** BCP-47 tags the app ships translations for. An empty tag means "follow the device". */
    val supportedLanguageTags: List<String> = listOf(
        "en", "ar", "de", "es", "fr", "it", "ja", "ko", "pt-BR", "zh-CN",
    )

    const val SYSTEM_LANGUAGE = ""

    fun languageTag(context: Context): String =
        preferences(context).getString(KEY_LANGUAGE, SYSTEM_LANGUAGE) ?: SYSTEM_LANGUAGE

    fun setLanguageTag(context: Context, tag: String) {
        preferences(context).edit().putString(KEY_LANGUAGE, tag).apply()
    }

    /** [context] reading the chosen language, or the device's own when none has been chosen. */
    fun wrap(context: Context): Context {
        val tag = languageTag(context)
        if (tag.isEmpty()) {
            // An earlier override in this process may have moved the JVM default away from it.
            deviceLocale()?.let { Locale.setDefault(it) }
            return context
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    /** The device's own locale, which [Locale.getDefault] no longer reports once overridden. */
    private fun deviceLocale(): Locale? =
        runCatching { Resources.getSystem().configuration.locales[0] }.getOrNull()

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private const val PREFERENCES = "app_settings"
    private const val KEY_LANGUAGE = "app_language"
}
