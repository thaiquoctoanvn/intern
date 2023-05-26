package io.edenx.convenience

import android.app.Activity
import android.content.Intent
import android.net.Uri

object CommonFunc {
    fun sendMail(activity: Activity, mailto: String, mailSubject: String? = null, chooserTitle: String = "") {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$mailto")
            mailSubject?.let {
                putExtra(Intent.EXTRA_SUBJECT, it)
            }
        }.let {
            activity.startActivity(Intent.createChooser(it, chooserTitle))
        }
    }
    fun shareThisApp(activity: Activity, shareMessage: String?, chooserTitle: String = "") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(
                Intent.EXTRA_TEXT,
                shareMessage
            )
            type = "text/plain"
            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        activity.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }
    fun openStoreListing(activity: Activity, applicationId: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(
                "https://play.google.com/store/apps/details?id=$applicationId")
            setPackage("com.android.vending")
        }
        activity.startActivity(Intent.createChooser(intent, null))
    }
    fun openBrowser(url: String, activity: Activity) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            activity.startActivity(Intent.createChooser(intent, null))
        }
    }
}