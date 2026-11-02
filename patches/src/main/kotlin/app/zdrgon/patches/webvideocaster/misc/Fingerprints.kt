package app.zdrgon.patches.webvideocaster.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Web Video Caster 6.1.0 — premium manager (classes6.dex) ─────────────
//
// The app merges two premium sources into one local state held by the
// manager class Le89; (instantiated once per process):
//   field b: MutableStateFlow<PremiumAccount (Lt79;)>
//   field c: MutableStateFlow<Boolean> — the hasPremium flow, seeded FALSE
//   field d: MutableStateFlow<PremiumAccountState (Ls89;)>
// RevenueCat (Lhz9;) and the Supabase "WVC Premium Account" service
// (Lej4;) are the only feeders, and both funnel through the single
// setter Lv36;->v(Lt79;Ljava/lang/String;)V — v36 is the callback object
// stored in Le89;->g. That setter is therefore the one place where every
// premium decision is written.
//
// Entitlement values (interface Lrc9;): Lqc9; = Unknown, the Lpc9;
// family = known entitlement with Loc9; = "Yes" (active), Lmc9;/Lnc9; =
// "No". Lhz9;->a builds Loc9; exactly when RevenueCat reports the
// "Premium" entitlement active. The synchronous gate Ltc9;->b/c reads
// the same flow and returns true iff the entitlement is Loc9; (or a
// local trial timestamp is still valid).

/**
 * Lv36;->v(Lt79;Ljava/lang/String;)V — the central "set premium account"
 * funnel. PUBLIC only (0x0001, verified in the 6.1.0 dex); the method
 * logs "Setting Has Premium: " and stores the computed Boolean into
 * Le89;->c just before returning. Morphe fingerprints compare access
 * flags by EXACT equality, so no extra flags may be listed here.
 */
object SetPremiumAccountFingerprint : Fingerprint(
    definingClass = "Lv36;",
    name = "v",
    returnType = "V",
    parameters = listOf("Lt79;", "Ljava/lang/String;"),
    accessFlags = listOf(AccessFlags.PUBLIC),
    strings = listOf("Setting Has Premium: "),
)

/**
 * Le89;-><init>(WebVideoCasterApplication)V — the premium manager
 * constructor (0x10001 PUBLIC CONSTRUCTOR, verified in the 6.1.0 dex).
 * It seeds the hasPremium flow (field c) with Boolean.FALSE; the patch
 * flips that single sget-object to Boolean.TRUE so the flow starts
 * premium before the first entitlement sync lands.
 */
object PremiumManagerInitFingerprint : Fingerprint(
    definingClass = "Le89;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Lcom/instantbits/cast/webvideo/WebVideoCasterApplication;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)

/**
 * Lcom/pairip/SignatureCheck;->verifyIntegrity(Landroid/content/Context;)V
 * (classes2.dex, PUBLIC STATIC, verified in the 6.1.0 dex).
 *
 * PairIP's Application class (com.pairip.application.Application, the
 * app's real Application) calls this FIRST in attachBaseContext —
 * before LicenseClient.checkLicense and before the app's own
 * attachBaseContext/onCreate, with no try/catch around it. The method
 * SHA-256-hashes the installed app's signing certificate and throws
 * SignatureCheck$SignatureTamperedException ("Apk signature is
 * invalid.") unless it matches PairIP's expected developer signature
 * (or a hardcoded test signature). Any re-signed build — Morphe
 * Manager's key included — therefore crashes uncatchably at process
 * start, which is exactly the on-device crash of the v1.35.1 test
 * build ("app crashed" dialog on launch). Gradient Weather ships an
 * older PairIP config whose Application has NO SignatureCheck at all
 * (attachBaseContext there only calls LicenseClient.checkLicense),
 * which is why Gradient survives re-signing with just the installer
 * spoof and WVC does not.
 */
object SignatureCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    strings = listOf("Apk signature is invalid.", "Signature check ok"),
)

// ── PairIP license check (classes2.dex) ─────────────────────────────────
//
// LicenseClient is PairIP's Play-licensing client. The ONLY dex entry
// points into it are the static checkLicense(Context): PairIP's
// Application.attachBaseContext (right after verifyIntegrity) and
// LicenseContentProvider.onCreate. A cross-dex scan of all 7 dex files
// shows no other class in the app even references LicenseClient,
// LicenseActivity or TrialClient, so every launch path funnels through
// the methods fingerprinted below.
//
// checkLicense posts to the main thread:
// getInstance(ctx).initializeLicenseCheck(), which switches on the
// static licenseCheckState (LicenseCheckState enum, seeded
// CHECK_REQUIRED by <clinit>):
//   CHECK_REQUIRED        -> bind to Play's ILicensingService
//                            (com.android.vending), send a
//                            LicenseCheckV2 request; the local installer
//                            branch is gated on localCheckEnabled, which
//                            is never written in the dex (false), so the
//                            shipped flow is the online check.
//   FULL_CHECK_OK         -> re-validates the cached JWS payload.
//   LOCAL_CHECK_OK /
//   LOCAL_CHECK_REPORTED  -> no-op.
//   REPEATED_CHECK_REQUIRED -> fresh connection (periodic re-check).
// The service answer lands in processResponse(code, bundle):
//   0 LICENSED     -> JWS validated, state becomes FULL_CHECK_OK (or
//                     REPEATED_CHECK_REQUIRED when repeated-check
//                     metadata is present) — the genuine success state.
//   2 NOT_LICENSED -> startPaywallActivity(): LicenseActivity opens
//                     with activitytype=PAYWALL and the service-supplied
//                     PAYWALL_INTENT PendingIntent, which opens the
//                     Play Store listing — for a non-Play install that
//                     page is the "Get this app from Play" block screen
//                     seen on-device with v1.35.3.
//   other          -> LicenseCheckException -> handleError -> error
//                     dialog via the same LicenseActivity vehicle.
// handleTrialEnd (reached only via stopTrial <- the caller-less
// TrialClient facade) is a third, independent re-check trigger: it
// resets licenseCheckState to CHECK_REQUIRED and binds afresh,
// bypassing initializeLicenseCheck.

/**
 * LicenseClient.checkLicense(Landroid/content/Context;)V — the static
 * entry (0x0009 PUBLIC STATIC, verified in the 6.1.0 dex; both of its
 * strings are in the method body).
 */
object LicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    strings = listOf("Cannot check license with null context.", "Skipping license check in isolated process."),
)

/**
 * LicenseClient.initializeLicenseCheck()V — the instance method the
 * static entry posts to the main thread (0x0001 PUBLIC, verified in
 * the 6.1.0 dex). Its body contains no const-strings, so the match
 * rests on class + name + signature + exact access flags; the method
 * is the only one of its name in the class.
 */
object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    returnType = "V",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

/**
 * LicenseClient.handleTrialEnd()V — the trial-end re-check trigger
 * (0x0004 PROTECTED, verified in the 6.1.0 dex; its log string is in
 * the method body).
 */
object HandleTrialEndFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "handleTrialEnd",
    returnType = "V",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PROTECTED),
    strings = listOf("Trial end event triggered; initiating full license check."),
)
