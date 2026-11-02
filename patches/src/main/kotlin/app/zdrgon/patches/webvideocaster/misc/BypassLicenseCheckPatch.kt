package app.zdrgon.patches.webvideocaster.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.webvideocaster.shared.Constants.COMPATIBILITY_WEB_VIDEO_CASTER
import java.util.logging.Logger

/**
 * Web Video Caster — Bypass PairIP license check.
 *
 * Round 2 of the PairIP work. v1.35.3 (signature bypass) got the app
 * past the startup crash, but on-device it then landed on the Play
 * Store's "Get this app from Play" block page. That page is the
 * second PairIP layer, mapped here from the 6.1.0 dex:
 *
 * LicenseClient (classes2.dex) is PairIP's Play-licensing client.
 * The static LicenseClient.checkLicense(Context) is its only entry:
 * the PairIP Application's attachBaseContext calls it right after
 * SignatureCheck.verifyIntegrity, and LicenseContentProvider.onCreate
 * calls it again. A scan of all 7 dex files shows no other app class
 * even references LicenseClient / LicenseActivity / TrialClient, so
 * every launch path funnels through checkLicense. (PairIP's VM
 * bootstrap, StartupLauncher from the AppComponentFactory clinit,
 * never touches it in dex — and any VM-to-Java call would still land
 * on the patched method bodies below.)
 *
 * checkLicense posts getInstance(ctx).initializeLicenseCheck() to
 * the main thread. That method switches on the static
 * licenseCheckState (seeded CHECK_REQUIRED): the shipped dex never
 * enables the local installer branch (localCheckEnabled is read once
 * and written nowhere), so CHECK_REQUIRED binds straight to Play's
 * ILicensingService in com.android.vending and sends a
 * LicenseCheckV2 request. The answer lands in
 * processResponse(code, bundle):
 *   0 LICENSED     -> state becomes FULL_CHECK_OK (or
 *                     REPEATED_CHECK_REQUIRED with re-check metadata)
 *   2 NOT_LICENSED -> startPaywallActivity(): LicenseActivity opens
 *                     with the service-supplied PAYWALL_INTENT
 *                     PendingIntent, i.e. the Play Store listing,
 *                     which renders the "Get this app from Play"
 *                     block page for a copy Play didn't install.
 *                     This is the on-device v1.35.3 behaviour: Play's
 *                     licensing service deterministically answers
 *                     NOT_LICENSED for a re-signed, non-Play install.
 *   other          -> LicenseCheckException -> handleError -> the
 *                     same LicenseActivity as an error dialog.
 * A third trigger, handleTrialEnd, resets the state to
 * CHECK_REQUIRED and binds afresh, bypassing initializeLicenseCheck;
 * its only caller chain is the caller-less TrialClient facade.
 *
 * Fix: reproduce the genuine success steady state at the funnel.
 * checkLicense and initializeLicenseCheck are replaced at entry with
 * "licenseCheckState = FULL_CHECK_OK; return" — FULL_CHECK_OK is
 * exactly the terminal value the real flow stores after a LICENSED
 * response with no repeated check due, and it is the value every
 * later reader keys on (checkTrialStatus maps it to NOT_UNDER_TRIAL,
 * the error-dialog gate stays closed while it is set). No instance
 * is created, nothing ever binds to Play's licensing service, no
 * repeated check is scheduled, and LicenseActivity — whose only two
 * launchers sit downstream of that bind — can never open.
 * handleTrialEnd is made to return immediately as well, so the
 * trial-end path cannot reset the state and re-bind behind the
 * stubbed entry points. All three methods are plain dex with real
 * bodies (not VM-virtualized). Required for ANY re-signed WVC build
 * to open, with or without the premium unlock, so it defaults on.
 *
 * Release note: the first v1.35.4 CI run compiled this patch green
 * but semantic-release lost a push race against another commit
 * landing on main mid-run, so the release is re-cut from this
 * comment-only revision.
 */
@Suppress("unused")
val bypassLicenseCheckPatch = bytecodePatch(
    name = "Bypass PairIP license check",
    description = "Required for re-signed builds: PairIP asks Google Play to license the app at startup and sends non-Play installs to a Play Store block page. Disables that check so the patched app can open.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WEB_VIDEO_CASTER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val setLicensedAndReturn =
            """
                sget-object v0, Lcom/pairip/licensecheck/LicenseClient${'$'}LicenseCheckState;->FULL_CHECK_OK:Lcom/pairip/licensecheck/LicenseClient${'$'}LicenseCheckState;
                sput-object v0, Lcom/pairip/licensecheck/LicenseClient;->licenseCheckState:Lcom/pairip/licensecheck/LicenseClient${'$'}LicenseCheckState;
                return-void
            """.trimIndent()

        LicenseCheckFingerprint.method.addInstructions(0, setLicensedAndReturn)
        logger.info("Web Video Caster: PairIP license check disabled (LicenseClient.checkLicense records FULL_CHECK_OK and returns).")

        InitializeLicenseCheckFingerprint.method.addInstructions(0, setLicensedAndReturn)
        logger.info("Web Video Caster: PairIP license check disabled (LicenseClient.initializeLicenseCheck records FULL_CHECK_OK and returns).")

        HandleTrialEndFingerprint.method.addInstructions(0, "return-void")
        logger.info("Web Video Caster: PairIP trial-end re-check disabled (LicenseClient.handleTrialEnd returns immediately).")
    }
}
