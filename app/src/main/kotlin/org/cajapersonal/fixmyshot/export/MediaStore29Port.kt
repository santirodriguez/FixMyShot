package org.cajapersonal.fixmyshot.export

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.InputStream
import java.io.OutputStream

/** Narrow ownership-only output port. A fake port exercises faults without touching a gallery. */
internal interface OwnedOutputPort {
    /** SAF may expose a newly created document before validation completes. */
    val visibleBeforeCommit: Boolean get() = false
    fun create(name: String, mime: String): Uri?
    fun openWrite(uri: Uri): OutputStream?
    fun openRead(uri: Uri): InputStream?
    fun publish(uri: Uri): Boolean
    fun delete(uri: Uri): Boolean
}

/** API29+ image collection; no broad storage permission is needed for app-owned output. */
@TargetApi(29)
internal class MediaStore29Port(private val resolver: ContentResolver) : OwnedOutputPort {
    init { require(Build.VERSION.SDK_INT >= 29) }

    override fun create(name: String, mime: String): Uri? {
        require(name.matches(Regex("FixMyShot_[0-9]{8}T[0-9]{6}Z_[0-9a-f]{8}\\.(png|webp)")))
        require((mime == "image/png" && name.endsWith(".png")) ||
            (mime == "image/webp" && name.endsWith(".webp")))
        val metadata = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/FixMyShot")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return resolver.insert(images, metadata)
    }

    override fun openWrite(uri: Uri): OutputStream? =
        resolver.openFileDescriptor(uri, "w")?.let { ParcelFileDescriptor.AutoCloseOutputStream(it) }

    override fun openRead(uri: Uri): InputStream? = resolver.openInputStream(uri)

    override fun publish(uri: Uri): Boolean {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        return resolver.update(uri, values, null, null) == 1
    }

    override fun delete(uri: Uri): Boolean = resolver.delete(uri, null, null) > 0
}
