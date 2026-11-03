package app.zdrgon.patches.diskwala.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.diskwala.shared.Constants.COMPATIBILITY_DISKWALA

/**
 * DiskWala — Enable Premium.
 *
 * Premium is sold as RevenueCat subscriptions ($rc_monthly $9.99,
 * $rc_three_month $25.99, $rc_annual $79.99). Whatever RevenueCat
 * reports, the app converts it to its own local state through exactly
 * one static mapper in its billing manager:
 *
 *   Lu5/f;->j(CustomerInfo) -> PremiumStatus(isPremium, expiresAtMs,
 *                                            activeEntitlementIds)
 *
 * Purchase, restore, the scheduled refresh, log-in and RevenueCat's
 * own updated-info listener all produce PremiumStatus exclusively
 * through that mapper (verified 2026-11-03: zero entitlement reads
 * anywhere else in the app's 9 dexes). The status then flows into the
 * session manager's local state — StateFlows plus a persisted
 * "premium_until" — which every premium gate reads: no ads, real file
 * names, HD downloads, faster streaming. No DiskWala server is asked
 * at decision time and no feature data is provisioned per-account;
 * the "tied to the device" line on the paywall is just the
 * RevenueCat app user ID.
 *
 * This patch pins the mapper to always return
 * PremiumStatus(true, Long.MAX_VALUE, []) — the local answer a paying
 * subscriber's sync produces, with no expiry. The session manager
 * treats a confirmed true as sticky (it refuses later downgrades), so
 * one refresh after install latches Premium on. It creates no
 * purchase, forges no receipt, and touches nothing in the billing or
 * RevenueCat libraries; it only changes the app's own translation of
 * store state into its own state.
 *
 * PairIP note: 27.3 ships full PairIP (application class, license
 * check and VM in classes2.dex, libpairipcore.so in the native split)
 * and its LOCAL installer check is live — LicenseClient
 * .performLocalInstallerCheck reads the installing package name and
 * rejects non-Play installs. The companion "Change installer source"
 * patch in this folder is therefore REQUIRED for a re-signed build to
 * get past the license check, exactly as with Video Cutter. Both
 * patches ship default-on; keep the companion enabled.
 */
@Suppress("unused")
val enablePremiumPatch = bytecodePatch(
    name = "Enable Premium",
    description = "Turns on Premium (no ads, real file names, HD downloads, faster streaming) as if subscribed. Also enable the Change installer source patch (pre-selected) — PairIP's installer check requires it.",
    default = true
) {
    compatibleWith(COMPATIBILITY_DISKWALA)

    execute {
        PremiumStatusMapperFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lu5/b;
                const/4 v1, 0x1
                const-wide v2, 0x7fffffffffffffff
                invoke-static {v2, v3}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
                move-result-object v2
                sget-object v3, Ltb/w;->b:Ltb/w;
                invoke-direct {v0, v1, v2, v3}, Lu5/b;-><init>(ZLjava/lang/Long;Ljava/util/List;)V
                return-object v0
            """.trimIndent()
        )
    }
}
