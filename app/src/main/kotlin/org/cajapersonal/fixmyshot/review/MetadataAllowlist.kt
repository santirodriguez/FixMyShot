package org.cajapersonal.fixmyshot.review

import androidx.exifinterface.media.ExifInterface
import org.cajapersonal.fixmyshot.core.EncodeRequest
import org.cajapersonal.fixmyshot.core.MetadataPolicy
import org.cajapersonal.fixmyshot.core.OutputFormat
import java.io.File
import java.io.IOException

/** Copies individual supported fields into an already verified, freshly rendered candidate. */
internal object MetadataAllowlist {
    val sourceTags: List<String> = listOf(
        ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL, ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
        ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF, ExifInterface.TAG_GPS_ALTITUDE,
    )

    fun apply(source: File, candidate: File, request: EncodeRequest) {
        require(request.metadataPolicy == MetadataPolicy.PRESERVE_ALLOWLIST)
        require(request.format == OutputFormat.PNG || request.format == OutputFormat.WEBP_LOSSLESS)
        require(source.canonicalFile != candidate.canonicalFile)
        // Read-only source. Never attach a source EXIF blob, thumbnail or XMP packet.
        val original = ExifInterface(source)
        val target = ExifInterface(candidate)
        val copied = linkedMapOf<String, String>()
        for (tag in sourceTags) {
            val value = original.getAttribute(tag) ?: continue
            // The first supported subset is bounded printable ASCII. Unsupported values are omitted.
            if (value.isEmpty() || value.length > 512 || value.any { it.code !in 32..126 }) continue
            target.setAttribute(tag, value)
            val accepted = target.getAttribute(tag)
            if (accepted == value) copied[tag] = value else target.setAttribute(tag, null)
        }
        // AndroidX synthesizes compatibility fields while reading. Only retain technical fields.
        target.setAttribute(ExifInterface.TAG_LIGHT_SOURCE, null)
        target.setAttribute(ExifInterface.TAG_DATETIME, copied[ExifInterface.TAG_DATETIME_ORIGINAL])
        target.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        target.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, request.size.width.toString())
        target.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, request.size.height.toString())
        target.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, request.size.width.toString())
        target.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, request.size.height.toString())
        target.saveAttributes()
        val reopened = ExifInterface(candidate)
        if (copied.any { (tag, value) -> reopened.getAttribute(tag) != value } ||
            reopened.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0) != ExifInterface.ORIENTATION_NORMAL ||
            reopened.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) != request.size.width ||
            reopened.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) != request.size.height ||
            reopened.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0) != request.size.width ||
            reopened.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0) != request.size.height) {
            throw IOException("Selective metadata verification failed")
        }
    }
}
