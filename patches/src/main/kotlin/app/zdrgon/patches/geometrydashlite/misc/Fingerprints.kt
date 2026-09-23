package app.zdrgon.patches.geometrydashlite.misc

import app.morphe.patcher.Fingerprint

/**
 * Matches the launcher Activity's onCreate(Bundle).
 *
 * Verified against Geometry Dash Lite 2.2.147 (com.robtopx.geometryjumplite):
 * the launcher Activity is com.robtopx.geometryjumplite.GeometryDashLite and
 * its onCreate(Bundle) calls FMOD.init, super.onCreate, then setupGAM.
 */
object LauncherOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/robtopx/geometryjumplite/GeometryDashLite;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
