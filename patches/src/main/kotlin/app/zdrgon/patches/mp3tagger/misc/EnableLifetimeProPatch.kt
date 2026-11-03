package app.zdrgon.patches.mp3tagger.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.mp3tagger.shared.Constants.COMPATIBILITY_SMART_MP3_TAGGER

/**
 * Smart MP3 Tagger — Enable Lifetime Pro.
 *
 * The app sells Pro four ways: three subscriptions (weekly / monthly /
 * yearly) and a one-time "Super Pro Forever" purchase ($49.99). All
 * four products feed a single local entitlement — there is no distinct
 * subscription state versus lifetime state. Every Pro question in the
 * app funnels through one reader:
 *
 *   Lmr0;->a()Z
 *     -> true iff the owned-products list contains the one-time
 *        "Super Pro Forever" SKU or any of the three subscription SKUs
 *
 * This patch pins that reader to true, so the app runs in exactly the
 * state the one-time Forever purchase produces: no ads, unlimited
 * functions, full performance — and, unlike a subscription state, a
 * pinned reader has nothing that can expire. The pin is at the reader,
 * so it does not matter what the billing sync stores at startup; the
 * stored value is simply never consulted.
 *
 * The patch creates no purchase and touches nothing in the billing
 * library; it only changes the app's own answer to its own question.
 * The paywall screen still opens if tapped, but the app already
 * believes Pro is active.
 */
@Suppress("unused")
val enableLifetimeProPatch = bytecodePatch(
    name = "Enable Lifetime Pro",
    description = "Turns on the app's Pro state (no ads, unlimited functions) as if the one-time \"Super Pro Forever\" purchase were owned — the lifetime state, with nothing to expire.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SMART_MP3_TAGGER)

    execute {
        IsProFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}
