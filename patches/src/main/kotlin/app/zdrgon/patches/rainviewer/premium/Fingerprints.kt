package app.zdrgon.patches.rainviewer.premium

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// RainViewer 8.4.1 (44084), classes3.dex — both methods verified against the
// real dex on 2026-11-02. The premium status holder is Ltw7; and the status
// enum is Lhw7; (FREE, V1, "pro" V2, INTERSTITIAL, REFERRAL, NO_ADS,
// ESSENTIAL). The PRO value is the static field Lhw7;->e:Lhw7;.
//
// Morphe compares access flags by exact equality, and Kotlin suspend
// functions in this build are "public final" — both flags are required.

/**
 * Ltw7;->g(ZZLft0;)Ljava/lang/Enum; — the server status recompute. It GETs
 * mobile/purchases/android/actual, maps the response to a UserPremiumStatus
 * and, when non-null, that result wins the status sync. The
 * "premium_expired_work" string is scheduled from this method only, for real
 * subscribers' expiry.
 */
internal object ServerPremiumStatusFingerprint : Fingerprint(
    definingClass = "Ltw7;",
    name = "g",
    returnType = "Ljava/lang/Enum;",
    parameters = listOf("Z", "Z", "Lft0;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    strings = listOf("premium_expired_work"),
)

/**
 * Ltw7;->e(Lft0;)Ljava/lang/Enum; — the local status path. It computes the
 * status from Play Billing, then persists it: clears the "premium" and
 * "premium_v1" prefs, sets one of them from the status ordinal (1 ->
 * premium_v1, 2 -> premium) and returns the status. Those prefs are what the
 * widgets and PremiumFeaturesContentProvider read.
 */
internal object LocalPremiumStatusFingerprint : Fingerprint(
    definingClass = "Ltw7;",
    name = "e",
    returnType = "Ljava/lang/Enum;",
    parameters = listOf("Lft0;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    strings = listOf("premium_v1", "premium"),
)
