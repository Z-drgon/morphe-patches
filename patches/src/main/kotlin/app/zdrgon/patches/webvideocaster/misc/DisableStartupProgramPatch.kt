package app.zdrgon.patches.webvideocaster.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.webvideocaster.shared.Constants.COMPATIBILITY_WEB_VIDEO_CASTER
import java.util.logging.Logger

/**
 * Web Video Caster — Disable PairIP startup program.
 *
 * Round 5 of the PairIP work, and the first round aimed BEFORE the
 * Application class exists. Rounds 1–3 closed every dex route to
 * the Play Store block page, verified instruction-by-instruction in
 * the patched output: the signature gate, the whole LicenseClient
 * funnel (checkLicense / initializeLicenseCheck forced to
 * FULL_CHECK_OK; processResponse, both LicenseActivity launchers,
 * the delayed shutdown and its exit action all emptied;
 * handleTrialEnd dead), LicenseActivity finishing itself in
 * onStart, and — via the companion resource patch — LicenseActivity
 * disabled in the manifest. On-device, with all of that enabled on
 * a fresh bundle, the "Get this app from Play" page still fires.
 *
 * The round-4 census of all 8 dexes explains where the remaining
 * actor has to live. Every VMRunner.invoke trampoline in the app
 * other than the one below is a third-party SDK
 * BroadcastReceiver.onReceive body; no app or SDK dex code outside
 * the stubbed license stack references the store, the license
 * classes, or the license state; the app's own Application and
 * lifecycle classes are plain dex with no trampolines. The one
 * exception is PairIP's own bootstrap:
 *
 *   StartupLauncher.launch() (classes2.dex) is invoked from the
 *   <clinit> of PairIP's shadow Landroid/app/AppComponentFactory;
 *   and of the manifest-declared
 *   Landroidx/core/app/CoreComponentFactory;. Class loading of the
 *   component factory happens during bindApplication — before
 *   Application.attachBaseContext, hence before verifyIntegrity,
 *   checkLicense, and everything else rounds 1–3 patched. The
 *   method runs VMRunner.invoke("Oz9gdoFZqm3JanLF", null): a
 *   415,036-byte encrypted VM module (the largest of PairIP's ~40),
 *   executed by libpairipcore.so, whose behavior cannot be read
 *   from the dex. Its surrounding plumbing is built for exactly
 *   this window: InitContextProvider reflectively assembles an app
 *   Context from ActivityThread/AppBindData and installs a basic
 *   Instrumentation named for starting activities, so the VM
 *   program can act — including starting an activity — before any
 *   Application code runs.
 *
 * Fix: make launch() return immediately, so the startup program
 * never executes. The method is plain dex with a real body (not
 * itself VM-virtualized), so a single return-void at entry
 * neutralizes it; its once-guard and the VMRunner machinery used
 * by the SDK receivers are untouched (those invoke VMRunner from
 * their own trampoline bodies, not through launch()).
 *
 * Honest scope: this is an elimination fix, not a traced one. The
 * startup program is the only actor left that runs early enough
 * and can start activities, but its encrypted contents cannot
 * prove it opens the store page — and if it also performs setup
 * the app silently relies on, the patched app may fail to start
 * instead of showing the block page. That is why this ships as its
 * own separate patch: if the app misbehaves with it on, untick
 * just this one switch and the build is back to exactly the
 * round-3 state. Defaults on, as the experiment Matsen approved.
 */
@Suppress("unused")
val disableStartupProgramPatch = bytecodePatch(
    name = "Disable PairIP startup program",
    description = "Experimental: PairIP runs an encrypted startup program before the app itself starts, earlier than every other check, and it is the only code left that can open the Play Store block page. Stops that program from running. If the app will not start with this on, turn just this patch off.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WEB_VIDEO_CASTER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        StartupLauncherFingerprint.method.addInstructions(0, "return-void")
        logger.info("Web Video Caster: PairIP startup program disabled (StartupLauncher.launch returns immediately; the VM startup module never runs).")
    }
}
