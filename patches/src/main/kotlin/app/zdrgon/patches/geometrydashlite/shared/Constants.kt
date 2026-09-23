package app.zdrgon.patches.geometrydashlite.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_GEOMETRY_DASH_LITE = Compatibility(
        name = "Geometry Dash Lite",
        packageName = "com.robtopx.geometryjumplite",
        // XAPK: Morphe merges the split bundle (including config.arm64_v8a.apk,
        // which holds the native libraries) into a single APK before patching.
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x2DB3FF,
        targets = listOf(
            AppTarget(version = "2.2.147"),
        ),
    )
}
