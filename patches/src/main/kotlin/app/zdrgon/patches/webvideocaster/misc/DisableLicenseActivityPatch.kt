package app.zdrgon.patches.webvideocaster.misc

import app.morphe.patcher.patch.resourcePatch
import app.zdrgon.patches.webvideocaster.shared.Constants.COMPATIBILITY_WEB_VIDEO_CASTER
import java.util.logging.Logger
import org.w3c.dom.Element

/**
 * Web Video Caster — Disable PairIP license activity (manifest layer).
 *
 * The component-level backstop for the "Bypass PairIP license check"
 * bytecode patch. That patch stubs every dex route into PairIP's
 * LicenseActivity (the checkLicense funnel, the processResponse
 * verdict handler, and the two launcher methods), and the stock-app
 * audit behind it found no other route: the activity is declared
 * exported=false with no intent-filter, LicenseContentProvider is
 * not declared in the manifest at all (its onCreate entry is inert),
 * no other dex references the license classes, and neither the
 * encrypted PairIP VM modules nor libpairipcore.so contain any
 * LicenseActivity/paywall reference. Even so, the block page
 * persisted on-device with the dex stubs verified in the output —
 * so this patch removes the target itself: with
 * android:enabled="false" on the component, ANY intent aimed at
 * LicenseActivity, from any code that could run in or around the
 * process, resolves to a disabled component and the activity can
 * never be created, let alone display the paywall or the error
 * dialog. Nothing else in the app resolves this component (its only
 * references are the two stubbed launcher methods), so disabling it
 * breaks no other flow. Defaults on: it is required for the license
 * bypass to be complete on re-signed builds.
 *
 * This patch is also the bundle-freshness tell: it first shipped in
 * the round-3 release. If "Disable PairIP license activity" does not
 * appear in Morphe Manager's patch list for Web Video Caster, the
 * bundle in Manager predates round 3 no matter what version the
 * source claims.
 */
@Suppress("unused")
val disableLicenseActivityPatch = resourcePatch(
    name = "Disable PairIP license activity",
    description = "Required for re-signed builds: disables PairIP's LicenseActivity component in the manifest, so the Play Store block page and license error dialog can never be shown, whatever tries to open them.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WEB_VIDEO_CASTER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        document("AndroidManifest.xml").use { document ->
            val activities = document.getElementsByTagName("activity")
            var found = false
            for (i in 0 until activities.length) {
                val element = activities.item(i) as Element
                if (element.getAttribute("android:name") == "com.pairip.licensecheck.LicenseActivity") {
                    element.setAttribute("android:enabled", "false")
                    found = true
                }
            }
            if (found) {
                logger.info("Web Video Caster: PairIP LicenseActivity disabled in the manifest (android:enabled=false).")
            } else {
                logger.warning("Web Video Caster: LicenseActivity not found in the manifest; nothing to disable.")
            }
        }
    }
}
