package app.zdrgon.patches.webvideocaster.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.webvideocaster.shared.Constants.COMPATIBILITY_WEB_VIDEO_CASTER
import java.util.logging.Logger

/**
 * Web Video Caster — Block PairIP startup activity launch.
 *
 * Round 6 of the PairIP work: the surgical counterpart to "Disable
 * PairIP startup program". The round-5 device result proved the
 * encrypted startup program ("Oz9gdoFZqm3JanLF", run by
 * StartupLauncher.launch() from the component-factory clinit) was
 * the actor opening the Play Store block page — with launch()
 * stubbed, the page never appears. But the same test showed the
 * wholesale stub's predicted downside: the app crashes on launch,
 * because the startup program also performs initialization the
 * PairIP runtime depends on. So this patch takes the opposite cut:
 * the startup program RUNS, and only its route to starting an
 * activity is removed.
 *
 * That route is a single dex method, mapped from the stock
 * classes2.dex. Every VMRunner.invoke(...) — the startup program
 * included — enters through VMRunner.invoke, whose dex prologue
 * calls InitContextProvider.tryToCreateContextForBindingApp().
 * That method reflectively assembles an app Context from
 * ActivityThread/AppBindData/LoadedApk via
 * ContextImpl.createAppContext (the context the VM legitimately
 * works with), and its final step is
 * tryToInitializeBasicInstrumentationForStartActivity(Class, Object):
 * it reflectively writes a fresh android.app.Instrumentation into
 * ActivityThread.mInstrumentation — the component an activity
 * launch in the pre-Application window stands on. An app-wide
 * census finds exactly one "mInstrumentation" const-string in all
 * 8 dexes: this method. It is the VM's only dex-visible
 * activity-start enabler.
 *
 * Fix: make that one method return immediately. The Context is
 * still created and returned, the startup program still executes
 * end to end (including whatever initialization round 5's crash
 * proved necessary), VMRunner's native library still loads from
 * VMRunner.<clinit> (dex, untouched), and the app's own startup is
 * unaffected — PairIP's Application merely subclasses the real
 * WebVideoCasterApplication, so app creation never depended on
 * the VM. What the VM loses is the instrumentation install: with
 * mInstrumentation never replaced in that window, its attempt to
 * open the Play listing (round 5 convicted the program of exactly
 * that) has no launch path.
 *
 * Intended use: ON together with "Disable PairIP startup program"
 * OFF — the startup program runs, the block stays dead. It is a
 * separate switch so the combinations can be tested independently;
 * turning both on is equivalent to the round-5 total stub for the
 * startup path.
 */
@Suppress("unused")
val blockStartupActivityLaunchPatch = bytecodePatch(
    name = "Block PairIP startup activity launch",
    description = "Lets PairIP's startup program run (the app needs its initialization) but removes the one step that lets it launch an activity before the app starts, so it cannot open the Play Store block page. Use with 'Disable PairIP startup program' turned OFF.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WEB_VIDEO_CASTER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        InitContextInstrumentationFingerprint.method.addInstructions(0, "return-void")
        logger.info("Web Video Caster: PairIP startup activity launch blocked (InitContextProvider.tryToInitializeBasicInstrumentationForStartActivity returns immediately; the startup program runs but cannot install its activity-launch instrumentation).")
    }
}
