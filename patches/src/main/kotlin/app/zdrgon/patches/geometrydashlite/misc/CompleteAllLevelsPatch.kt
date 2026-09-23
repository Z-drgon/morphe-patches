package app.zdrgon.patches.geometrydashlite.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.geometrydashlite.shared.Constants.COMPATIBILITY_GEOMETRY_DASH_LITE

// Extension helper that rewrites the game's save file. Must match the class
// in the extensions module.
private const val SAVE_COMPLETER =
    "Lapp/template/extension/geometrydashlite/SaveCompleter;"

@Suppress("unused")
val completeAllLevelsPatch = bytecodePatch(
    name = "Complete all levels",
    description = "Marks every official level 100% complete with all secret coins by updating the " +
        "game's save file on launch. No gameplay code is modified.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GEOMETRY_DASH_LITE)

    execute {
        val method = LauncherOnCreateFingerprint.method

        // Insert at the very start of onCreate. p0 is the Activity (a Context)
        // and is already initialized as a parameter, so calling a static
        // helper with it before anything else is safe. The game only loads
        // CCGameManager.dat later during native startup, so the edited save
        // wins the race.
        method.addInstructions(
            0,
            """
                invoke-static {p0}, $SAVE_COMPLETER->run(Landroid/content/Context;)V
            """,
        )
    }
}
