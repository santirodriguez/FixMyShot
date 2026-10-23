package org.cajapersonal.fixmyshot.export

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/** Explicit removal of an exactly identified, still-pending app-owned image; never a saved copy. */
internal class PendingMediaStoreRemoval(
    private val resolver: ContentResolver,
    private val packageName: String,
    private val api: Int = Build.VERSION.SDK_INT,
) {
    companion object {
        fun supportedUri(uri: Uri): Boolean = uri.scheme == ContentResolver.SCHEME_CONTENT &&
            uri.authority == MediaStore.AUTHORITY && uri.query == null && uri.fragment == null &&
            uri.pathSegments.let { parts -> parts.size == 4 &&
                parts[0] == MediaStore.VOLUME_EXTERNAL_PRIMARY && parts[1] == "images" &&
                parts[2] == "media" && (parts[3].toLongOrNull() ?: 0L) > 0L }
    }

    fun remove(record: OutputRecord): Boolean {
        if (api < 29 || record.stage !in setOf(OutputStage.PENDING, OutputStage.VALIDATED,
                OutputStage.INDETERMINATE) || packageName.isBlank()) return false
        val uri = record.uri?.let(Uri::parse) ?: return false
        if (!supportedUri(uri)) return false
        val pending = MediaStore.MediaColumns.IS_PENDING
        val owner = MediaStore.MediaColumns.OWNER_PACKAGE_NAME
        val name = MediaStore.MediaColumns.DISPLAY_NAME
        val mime = MediaStore.MediaColumns.MIME_TYPE
        val path = MediaStore.MediaColumns.RELATIVE_PATH
        val paths = setOf("Pictures/FixMyShot", "Pictures/FixMyShot/")
        return try {
            val matched = resolver.query(uri, arrayOf(pending, owner, name, mime, path),
                null, null, null)?.use { row ->
                row.moveToFirst() && !row.isNull(0) && row.getInt(0) == 1 &&
                    row.getString(1) == packageName && row.getString(2) == record.filename &&
                    row.getString(3) == record.mime && row.getString(4) in paths && !row.moveToNext()
            } == true
            if (!matched) false else {
                // Repeat every ownership/publication predicate in the deletion itself.
                resolver.delete(uri, "$pending = ? AND $owner = ? AND $name = ? AND $mime = ? AND ($path = ? OR $path = ?)",
                    arrayOf("1", packageName, record.filename, record.mime,
                        "Pictures/FixMyShot", "Pictures/FixMyShot/")) == 1
            }
        } catch (_: Exception) { false }
    }
}
