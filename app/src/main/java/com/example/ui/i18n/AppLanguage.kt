package com.example.ui.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Language handling for the app.
 *
 * The app ships with two fully translated locales (English as the default and Persian) and lets
 * the user either follow the device language or force one of them from the settings screen.
 */
object AppLanguage {

    /** Stored value: follow the device language. */
    const val MODE_SYSTEM = "system"

    /** Stored value: always use Persian. */
    const val MODE_PERSIAN = "fa"

    /** Stored value: always use English. */
    const val MODE_ENGLISH = "en"

    const val PERSIAN = "fa"
    const val ENGLISH = "en"

    /** Language currently configured on the device (respects Android 13+ per-app languages). */
    fun deviceLanguage(context: Context): String {
        val configuration = context.resources.configuration
        return if (configuration.locales.isEmpty) {
            Locale.getDefault().language
        } else {
            configuration.locales[0].language
        }
    }

    /** Resolves the language code that should be applied to the UI. */
    fun resolve(mode: String, deviceLanguage: String): String {
        return when (mode) {
            MODE_PERSIAN -> PERSIAN
            MODE_ENGLISH -> ENGLISH
            else -> if (isPersianLanguageCode(deviceLanguage)) PERSIAN else ENGLISH
        }
    }

    fun isPersian(mode: String, deviceLanguage: String): Boolean =
        resolve(mode, deviceLanguage) == PERSIAN

    fun isPersianLanguageCode(languageCode: String): Boolean {
        val code = languageCode.lowercase(Locale.ROOT)
        return code == "fa" || code == "per" || code == "pes"
    }

    fun localeFor(mode: String, deviceLanguage: String): Locale =
        Locale(resolve(mode, deviceLanguage))
}

/**
 * Lightweight [ContextWrapper] that exposes resources of the selected locale while keeping every
 * other capability (startActivity, services, content resolver, ...) delegated to the wrapped
 * context, so the app keeps working exactly as before.
 */
class LocaleAwareContext(base: Context, locale: Locale) : ContextWrapper(base) {

    private val localizedResources: Resources = base.createConfigurationContext(
        Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    ).resources

    override fun getResources(): Resources = localizedResources

    override fun getAssets(): AssetManager = localizedResources.assets
}
