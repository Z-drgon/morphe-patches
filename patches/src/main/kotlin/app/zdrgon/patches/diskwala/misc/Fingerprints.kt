package app.zdrgon.patches.diskwala.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Premium status production point — DiskWala 27.3 ───────────────────
//
// DiskWala's subscriptions run through RevenueCat (paywall packages
// $rc_monthly / $rc_three_month / $rc_annual). The app never reads
// RevenueCat's CustomerInfo directly: a census of all 9 dexes shows ZERO
// entitlement reads outside the RevenueCat SDK and one mapper in the
// app's billing manager (classes3.dex):
//
//   Lu5/f;->j(Lcom/revenuecat/purchases/CustomerInfo;)Lu5/b;
//     Lu5/b = PremiumStatus(isPremium:Z, expiresAtMs:Long?,
//                           activeEntitlementIds:List)
//
// Every production of a PremiumStatus from store data goes through that
// one static mapper: the purchase flow, restore, the scheduled premium
// refresh, log-in, and RevenueCat's UpdatedCustomerInfoListener all call
// it. The resulting status is bridged into the session manager (Lo5/d),
// which holds premium as local StateFlows, persists "premium_until" in
// DataStore, and — once a true status lands via Lo5/d;->j(Z, Long) —
// refuses later downgrades. All feature gates (ads, downloads,
// streaming, file names) read that local session state; nothing asks a
// server when the app decides whether Premium is on, and no entitlement
// or feature data is served by a DiskWala backend (the only DiskWala
// hosts in the binary are diskwala.com deep links).
//
// The "tied to the device" paywall line is the RevenueCat app user ID:
// no ANDROID_ID / hardware-ID check exists in the app's own code.
//
// NOTE: Morphe fingerprints compare access flags by EXACT equality;
// the dex record for the mapper is PUBLIC | STATIC (0x9), 5 registers.

/** The single CustomerInfo -> PremiumStatus mapper. Pinning it pins the app. */
object PremiumStatusMapperFingerprint : Fingerprint(
    returnType = "Lu5/b;",
    parameters = listOf("Lcom/revenuecat/purchases/CustomerInfo;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    definingClass = "Lu5/f;",
    name = "j"
)
