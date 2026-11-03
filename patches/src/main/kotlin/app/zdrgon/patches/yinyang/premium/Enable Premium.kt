package app.zdrgon.patches.yinyang.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.yinyang.shared.Constants.COMPATIBILITY_YIN_YANG
import app.zdrgon.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * Yin Yang Launcher — Enable Premium (Lifetime).
 *
 * The app sells Yin Yang Pro as a Yearly subscription ($14.99/yr) or a
 * one-time Lifetime purchase ($29.99) through RevenueCat (entitlement
 * "pro_v1"), cross-checked on-device against a direct Play Billing
 * ownership query. Both answers are merged locally by PlayProLogic
 * into a YSubscription, and SessionManager.emitSession folds that into
 * SessionFlowData — the single session object whose isPremiumUser
 * boolean every feature gate in the app reads (18 reader files:
 * appearance/EditLook clocks, fonts, live wallpapers and icons,
 * homescreen, mindful features, MySpace, widgets, promotions).
 * Nothing is re-validated by a developer backend when a feature is
 * used; the gates trust the local session object.
 *
 * SessionFlowData has exactly one constructor, which stores its third
 * parameter verbatim into the isPremiumUser field. This patch forces
 * that parameter to true immediately before the store, so every
 * session — the real emitted one and the fallback instances feature
 * code builds — reports a premium user, the exact state a completed
 * Lifetime purchase produces. It creates no purchase, forges no
 * receipt, and touches nothing in RevenueCat or the billing library;
 * it only changes the app's own answer to its own question. Pinning
 * the constructor (rather than the subscription state) also survives
 * billing reconciliation: RC/Play syncs can rewrite the subscription,
 * but the session built from it still comes out premium.
 *
 * The stored subscription itself stays non-premium, so the paywall
 * and profile screens may still DISPLAY prices / a free label. That
 * is display only — the features themselves unlock.
 *
 * PairIP note: 5.13.1 is PairIP LIGHT (application + licensecheck in
 * classes2.dex, no VM, no native PairIP lib, no SignatureCheck) and
 * its local installer check is LIVE (LicenseClient
 * .performLocalInstallerCheck -> getInstallSourceInfo), so the
 * companion "Change installer source" patch in this folder is
 * REQUIRED for a re-signed build to get past the license check at
 * all — exactly the Video Cutter situation. Both patches ship
 * default-on.
 */
@Suppress("unused")
val enablePremiumPatch = bytecodePatch(
    name = "Enable Premium",
    description = "Unlocks Yin Yang Pro for free, as if the Lifetime purchase were bought: all clocks, fonts, live wallpapers and icons. Also enable the Change installer source patch (pre-selected) — it is required for the patched app to pass the license check.",
    default = true
) {
    compatibleWith(COMPATIBILITY_YIN_YANG)

    execute {
        SessionFlowDataConstructorFingerprint.method.apply {
            val insns = instructionsOrNull?.toList()
                ?: throw PatchException("SessionFlowData constructor has no instructions.")
            // The store of the isPremiumUser field: iput-boolean p3, p0, Loc/for;->try:Z
            val storeIndex = insns.indexOfFirst { insn ->
                if (insn.opcode != Opcode.IPUT_BOOLEAN) return@indexOfFirst false
                val ref = insn.getReference<FieldReference>() ?: return@indexOfFirst false
                ref.definingClass == "Loc/for;" && ref.name == "try"
            }
            if (storeIndex < 0) {
                throw PatchException("Could not find the isPremiumUser store in the SessionFlowData constructor.")
            }
            // Force the parameter register to true right before it is stored.
            addInstructions(
                storeIndex,
                """
                    const/4 p3, 0x1
                """.trimIndent()
            )
        }
    }
}
