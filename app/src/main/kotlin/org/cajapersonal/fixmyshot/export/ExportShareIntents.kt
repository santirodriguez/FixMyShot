package org.cajapersonal.fixmyshot.export

import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

/** Only app-verified, committed output URIs may be passed to this intent builder. */
internal data class ShareImage(val uri: Uri, val mime: String)

internal object ExportShareIntents {
    fun build(items: List<ShareImage>): Intent? {
        if (items.isEmpty() || items.size > 100) return null
        if (items.any { it.uri.scheme != ContentResolver.SCHEME_CONTENT ||
                it.uri.authority.isNullOrBlank() ||
                it.mime !in setOf("image/png", "image/webp") }) return null
        if (items.map { it.uri.toString() }.toSet().size != items.size) return null

        val uris = ArrayList(items.map { it.uri })
        val mimeTypes = items.map { it.mime }.distinct()
        val kind = if (items.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
        return Intent(kind).apply {
            type = if (mimeTypes.size == 1) mimeTypes.first() else "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Use already verified MIME types: constructing the chooser must not query a provider again.
            clipData = ClipData("FixMyShot output", mimeTypes.toTypedArray(), ClipData.Item(uris.first())).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            if (items.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }
}
