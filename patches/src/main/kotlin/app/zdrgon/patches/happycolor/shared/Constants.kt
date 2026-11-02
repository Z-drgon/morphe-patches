package app.zdrgon.patches.happycolor.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    // Happy Color by Good Job Games — Flutter title, ads via AppLovin MAX.

    val COMPATIBILITY_HAPPY_COLOR = Compatibility(
        name = "Happy Color",
        packageName = "com.pixel.art.coloring.color.number",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0xFF7043,
        targets = listOf(
            AppTarget(version = "2.24.1")
        )
    )
}
