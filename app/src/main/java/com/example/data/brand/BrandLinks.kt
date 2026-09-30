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

    /** Opens the brand website (with the UTM tags that tell us the traffic came from the app). */
    fun openWebsite(context: Context) {
        openUrl(context, websiteUrl())
    }

    /** The id the app is published under, even when the installed build uses a private channel id. */
    private const val PUBLIC_PACKAGE_ID = "ir.webnovo.novo"

    fun storeListingUrl(): String = "https://cafebazaar.ir/app/$PUBLIC_PACKAGE_ID"

    /**
     * Opens the store page for a rating: the Bazaar app first (that is where this build is
     * published), then Google Play, and finally a browser page - whichever the phone can handle.
     */
    fun openStoreListing(context: Context) {
        val candidates = listOf(
            "bazaar://details?id=$PUBLIC_PACKAGE_ID",
            "market://details?id=$PUBLIC_PACKAGE_ID",
            storeListingUrl()
        )
        for (url in candidates) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            } catch (_: ActivityNotFoundException) {
                // try the next one
            } catch (_: Exception) {
                // try the next one
            }
        }
        Toast.makeText(context.applicationContext, R.string.toast_link_failed, Toast.LENGTH_SHORT).show()
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
