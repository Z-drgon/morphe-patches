package app.zdrgon.patches.gamesinone.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Google Play Billing — Games In One 0.4.5 ─────────────────────────────
//
// The only queryPurchasesAsync overload in the app is the modern
// QueryPurchasesParams one (verified in the 0.4.5 base dex on 2026-11-01).
// Public API method on the SDK's own BillingClientImpl — never obfuscated,
// unique by (name, returnType, parameters) within the class.

/**
 * BillingClientImpl.queryPurchasesAsync(QueryPurchasesParams,
 * PurchasesResponseListener)V (.registers 9 → p0=v6, p1=v7, p2=v8; no
 * try/catch entries).
 *
 * Unity IAP fetches owned purchases through this call at store init. The
 * patch wraps it so the game's listener receives a forged owned purchase
 * for the VIP product alongside whatever Play actually returns.
 */
object QueryPurchasesAsyncFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Lcom/android/billingclient/api/QueryPurchasesParams;",
        "Lcom/android/billingclient/api/PurchasesResponseListener;"
    ),
    accessFlags = listOf(AccessFlags.PUBLIC),
    definingClass = "Lcom/android/billingclient/api/BillingClientImpl;",
    name = "queryPurchasesAsync"
)
