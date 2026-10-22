package org.cajapersonal.fixmyshot.export

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.InputStream
import java.io.OutputStream

/**
 * Explicit user-selected SAF folder, not an automatic Pictures/FixMyShot save.
 * A SAF document can be visible immediately after createDocument: there is no
 * MediaStore IS_PENDING equivalent and callers must explain that limitation.
 */
internal class SafTreeOutputPort(
    private val resolver: ContentResolver,
    private val tree: Uri,
    private val createDocument: (Uri, String, String) -> Uri? = { parent, mime, name ->
        DocumentsContract.createDocument(resolver, parent, mime, name)
    },
    private val deleteDocument: (Uri) -> Boolean = {
        DocumentsContract.deleteDocument(resolver, it)
    },
) : OwnedOutputPort {
    override val visibleBeforeCommit: Boolean = true

    init {
        require(tree.scheme == ContentResolver.SCHEME_CONTENT && DocumentsContract.isTreeUri(tree)) {
            "Expected a user-selected SAF tree URI"
        }
    }

    override fun create(name: String, mime: String): Uri? {
        require(name.matches(Regex("FixMyShot_[0-9]{8}T[0-9]{6}Z_[0-9a-f]{8}\\.(png|webp)")))
        require((mime == "image/png" && name.endsWith(".png")) ||
            (mime == "image/webp" && name.endsWith(".webp")))
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree))
        val created = createDocument(parent, mime, name) ?: return null
        // An unexpected provider URI is not evidence that we own the returned document.
        // Never attempt cleanup outside the folder explicitly selected by the user.
        if (!withinTree(created)) return null
        if (!hasExactName(created, name)) {
            runCatching { deleteDocument(created) }
            return null
        }
        return created
    }

    private fun hasExactName(uri: Uri, wanted: String): Boolean = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            it.moveToFirst() && it.getString(0) == wanted
        } == true
    } catch (_: Exception) { false }

    private fun withinTree(uri: Uri): Boolean = try {
        uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority == tree.authority &&
            DocumentsContract.isTreeUri(uri) &&
            DocumentsContract.getTreeDocumentId(uri) == DocumentsContract.getTreeDocumentId(tree)
    } catch (_: IllegalArgumentException) { false }

    override fun openWrite(uri: Uri): OutputStream? =
        if (!withinTree(uri)) null else resolver.openFileDescriptor(uri, "w")?.let {
            ParcelFileDescriptor.AutoCloseOutputStream(it)
        }

    override fun openRead(uri: Uri): InputStream? =
        if (!withinTree(uri)) null else resolver.openInputStream(uri)

    /** This acknowledges validated completion, not an atomic publication switch. */
    override fun publish(uri: Uri): Boolean = withinTree(uri)

    override fun delete(uri: Uri): Boolean =
        withinTree(uri) && deleteDocument(uri)
}
