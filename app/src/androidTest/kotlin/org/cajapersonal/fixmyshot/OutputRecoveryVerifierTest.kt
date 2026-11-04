package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class OutputRecoveryVerifierTest {
    @Test fun ownMediaStoreOutputIsVerifiedWithoutPublicationSideEffects() {
        if (Build.VERSION.SDK_INT < 29) return
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val port = MediaStore29Port(app.contentResolver)
        val name = "FixMyShot_20261022T000000Z_abcdef12.png"
        val image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        try {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream))
        } finally { image.recycle() }
        val bytes = stream.toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val uri = requireNotNull(port.create(name, "image/png"))
        try {
            requireNotNull(port.openWrite(uri)).use { target -> target.write(bytes) }
            val record = OutputRecord("a".repeat(32), "item", name, "image/png",
                hash, OutputStage.PUBLISHED, uri.toString())
            val verifier = OutputRecoveryVerifier(app.contentResolver)
            assertEquals(OutputRecoveryEvidence.PENDING, verifier.verify(record))
            assertTrue(port.publish(uri))
            assertEquals(OutputRecoveryEvidence.MATCHED, verifier.verify(record))
            assertEquals(OutputRecoveryEvidence.MODIFIED,
                verifier.verify(record.copy(digest = "b".repeat(64))))
        } finally { port.delete(uri) }
    }
}
