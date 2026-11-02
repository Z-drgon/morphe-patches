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
