package app.zdrgon.patches.droidcam.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.droidcam.shared.Constants.COMPATIBILITY_DROIDCAM

// Extension helper carrying the stock signing-certificate data (bytes
// and hashCode) harvested from the original app. Must match the class
// in the extensions module.
private const val SIG_SPOOF =
    "Lapp/template/extension/droidcam/SigSpoof;"

/**
 * Fixes the dead camera in re-signed builds. The service Load hand-off
 * feeds the app's own signing certificate into two places: the server
 * derives the expected video-client nonce from its hashCode
 * (hashCode & 6138), and the Lk crypto helper is seeded with its
 * bytes. Stock passes the official certificate (nonce 5912, which the
 * official PC client sends); a re-signed build passes the debug
 * certificate (nonce 128), so the server closes every video request,
 * the encoder/session never start, and the preview stays black while
 * audio warnings repeat.
 *
 * This patch replaces both hand-off arguments at Load entry with the
 * stock values from the extension, so every signature-derived value in
 * the app matches the original. Proven 2026-11-05: numeric check
 * (stock cert hash & 6138 == 5912 == the client's nonce), a live
 * Frida substitution of the same two arguments (video streamed), and
 * the failure of the un-swapped build to ever start video.
 *
 * Requires "Disable PairIP protection" (it merges the extension this
 * patch calls into the app).
 */
@Suppress("unused")
val fixCameraStreamingPatch = bytecodePatch(
    name = "Fix camera streaming",
    description = "Fixes the black preview / no video in patched builds: the service is " +
        "handed the original app's signature data, so the PC client's video requests " +
        "are accepted exactly as with the stock app. Requires 'Disable PairIP " +
        "protection'. DroidCam 10.0 only.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_DROIDCAM)

    dependsOn(disablePairIpPatch)

    execute {
        // Overwrite the two parameters (p1 = certificate bytes,
        // p2 = hashCode) before the original body can use them.
        ServiceLoadFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, $SIG_SPOOF->cert()[B
                move-result-object p1
                invoke-static {}, $SIG_SPOOF->hash()I
                move-result p2
            """,
        )
    }
}
