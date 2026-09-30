package com.example.data.brand

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.BuildConfig
import com.example.R

/**
 * Every link that points back to the Novin Web brand lives here, so the marketing team can change
 * the addresses (or add the next product) in one place.
 *
 * All outgoing links carry UTM parameters, which is how we learn which channel actually brought
 * people in (store listing, a poster shared on Instagram, a Telegram channel, ...).
 */
object BrandLinks {

    private const val WEBSITE = "https://webnovo.ir/"

    /** Appended to every outbound link so traffic sources stay measurable. */
    const val UTM_SUFFIX = "?utm_source=novo_app&utm_medium=android&utm_campaign=brand_awareness"

    fun websiteUrl(): String = WEBSITE + UTM_SUFFIX

    /**
     * Public download page for the newest build - no GitHub account needed, so it can be shared
     * with everybody. Each channel keeps one rolling release, so this link never changes.
     */
    fun latestReleaseUrl(): String {
        val tag = if (BuildConfig.APPLICATION_ID.endsWith(".canary")) "apk-canary" else "apk-latest"
        return "https://github.com/jackiuns066-lgtm/appmusic/releases/tag/$tag"
    }

    fun openLatestRelease(context: Context) {
        openUrl(context, latestReleaseUrl())
    }

    fun storeListingUrl(): String =
        "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

    /** market:// opens the store app directly, the https link is the fallback. */
    private fun marketUrl(): String = "market://details?id=${BuildConfig.APPLICATION_ID}"

    fun openWebsite(context: Context) {
        openUrl(context, websiteUrl())
    }

    /** Asks the user to rate the app — ratings above 4.5 measurably lift install conversion. */
    fun openStoreListing(context: Context) {
        val opened = try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
        if (!opened) {
            openUrl(context, storeListingUrl())
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Toast.makeText(context.applicationContext, R.string.toast_link_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
