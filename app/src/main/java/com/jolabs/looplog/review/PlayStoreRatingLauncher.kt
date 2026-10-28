package com.jolabs.looplog.review

import android.app.Activity
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

/** Opens the app's Play Store listing, falling back to its web listing when needed. */
class PlayStoreRatingLauncher @Inject constructor() {

    fun open(activity: Activity): Result<Unit> {
        val appId = activity.packageName
        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$appId"),
        ).apply {
            setPackage(PLAY_STORE_PACKAGE)
        }

        return runCatching {
            activity.startActivity(playStoreIntent)
        }.recoverCatching {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appId"),
                )
            )
        }
    }

    private companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}
