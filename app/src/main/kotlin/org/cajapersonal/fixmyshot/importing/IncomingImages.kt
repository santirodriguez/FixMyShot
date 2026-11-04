package org.cajapersonal.fixmyshot.importing

import android.content.Intent
import android.net.Uri

/** Only bounded granted content is passed to the normal signature-checking private stager. */
internal object IncomingImages {
    data class Selection(val uris: List<Uri>, val rejected: Int = 0, val overLimit: Boolean = false)

    @Suppress("DEPRECATION")
    fun parse(intent: Intent): Selection {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) {
            return Selection(emptyList())
        }
        return try {
            val streams = if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            } else listOfNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            val clips = intent.clipData
            // Reject the entire oversized offer: never silently take the first 100 images.
            if (streams.size > 100 || (clips?.itemCount ?: 0) > 100) {
                return Selection(emptyList(), overLimit = true)
            }
            val offered = (streams + (0 until (clips?.itemCount ?: 0)).mapNotNull {
                clips?.getItemAt(it)?.uri
            }).distinctBy(Uri::toString)
            if (offered.size > 100) return Selection(emptyList(), overLimit = true)
            val safe = offered.filter { it.scheme == "content" && !it.authority.isNullOrBlank() &&
                it.toString().length <= 16_384 }
            Selection(safe, (offered.size - safe.size).coerceAtLeast(if (offered.isEmpty()) 1 else 0))
        } catch (_: RuntimeException) {
            // BadParcelableException, malformed extras or a sender using the wrong parcelable type.
            Selection(emptyList(), rejected = 1)
        }
    }
}
