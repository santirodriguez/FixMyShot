package org.cajapersonal.fixmyshot.export

import org.cajapersonal.fixmyshot.core.BackgroundChoice
import org.cajapersonal.fixmyshot.core.PrivateOutputRecipe
import java.io.DataOutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.io.OutputStream

/** Deterministic identity of output semantics, independent of JSON ordering or device heap size. */
internal object FrozenPlanFingerprint {
    // Bump for semantic geometry/color/metadata renderer changes before allowing old-job retry.
    private const val RENDER_POLICY_VERSION = 2
    fun of(recipes: List<PrivateOutputRecipe>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val sink = object : OutputStream() { override fun write(value: Int) {} }
        DataOutputStream(DigestOutputStream(sink, digest)).use { out ->
            out.writeInt(RENDER_POLICY_VERSION)
            out.writeInt(recipes.size)
            recipes.forEach { recipe ->
                out.writeUTF(recipe.sourceId); out.writeUTF(recipe.sourceSha256)
                out.writeInt(recipe.orientedSource.width); out.writeInt(recipe.orientedSource.height)
                out.writeLong(recipe.itemRevision); out.writeLong(recipe.sessionRevision)
                out.writeInt(recipe.catalogVersion); out.writeUTF(recipe.destinationId)
                out.writeInt(recipe.canvas.width); out.writeInt(recipe.canvas.height)
                out.writeInt(recipe.placement.left); out.writeInt(recipe.placement.top)
                out.writeInt(recipe.placement.width); out.writeInt(recipe.placement.height)
                when (val background = recipe.background) {
                    BackgroundChoice.Automatic -> out.writeUTF("automatic")
                    BackgroundChoice.White -> out.writeUTF("white")
                    BackgroundChoice.Black -> out.writeUTF("black")
                    BackgroundChoice.Transparent -> out.writeUTF("transparent")
                    is BackgroundChoice.CustomRgb -> { out.writeUTF("rgb"); out.writeInt(background.rgb) }
                }
                out.writeUTF(recipe.codecRoute.name); out.writeUTF(recipe.request.format.name)
                out.writeInt(recipe.request.size.width); out.writeInt(recipe.request.size.height)
                out.writeBoolean(recipe.request.hasAlpha); out.writeUTF(recipe.request.metadataPolicy.name)
                out.writeBoolean(recipe.crop != null)
                recipe.crop?.let { crop ->
                    out.writeDouble(crop.left); out.writeDouble(crop.top)
                    out.writeDouble(crop.width); out.writeDouble(crop.height)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
