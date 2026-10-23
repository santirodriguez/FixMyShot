package org.cajapersonal.fixmyshot.export

import android.system.Os
import android.system.OsConstants
import android.system.ErrnoException
import android.os.StatFs
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/** Advisory only; pipes, virtual/closed descriptors and unavailable statistics stay unknown. */
internal object OutputStorage {
    private const val HEADROOM = 16L * 1024 * 1024

    /** Minimum private working reserve only, not an estimate/reservation of the encoded file size. */
    fun hasDirectoryHeadroom(directory: File): Boolean {
        val available = try { StatFs(directory.absolutePath).availableBytes }
            catch (_: Exception) { return true }
        return available < 0 || available >= HEADROOM
    }

    fun availableBytes(stream: OutputStream): Long? {
        if (stream !is FileOutputStream) return null
        return try {
            val fd = stream.fd
            if (!OsConstants.S_ISREG(Os.fstat(fd).st_mode)) return null
            val stat = Os.fstatvfs(fd)
            if (stat.f_blocks <= 0 || stat.f_frsize <= 0 || stat.f_bavail < 0 ||
                stat.f_bavail > Long.MAX_VALUE / stat.f_frsize) null
            else stat.f_bavail * stat.f_frsize
        } catch (_: Exception) { null }
    }

    fun hasHeadroom(stream: OutputStream, remainingBytes: Long): Boolean {
        if (remainingBytes < 0 || remainingBytes > Long.MAX_VALUE - HEADROOM) return false
        val available = availableBytes(stream) ?: return true
        return available >= remainingBytes + HEADROOM
    }

    /** Pipes cannot fsync; regular files retain the existing durability requirement. */
    fun syncIfRegularFile(stream: OutputStream) {
        if (stream !is FileOutputStream) return
        val fd = stream.fd
        val regular = try { OsConstants.S_ISREG(Os.fstat(fd).st_mode) }
            catch (error: ErrnoException) { throw IOException("Output descriptor unavailable", error) }
        if (regular) fd.sync()
    }
}
