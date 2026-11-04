package app.zdrgon.patches.diskwala.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.diskwala.shared.Constants.COMPATIBILITY_DISKWALA
import java.util.logging.Logger

/**
 * DiskWala — Bypass PairIP VM and signature check.
 *
 * Ported from kuntal-devrat/diskwala-patches ("Disable forced
 * update", built for DiskWala 24.5 and reported working), re-derived
 * against Matsen's exact 27.3 dexes. This is the layer our
 * v1.52/v1.56 DiskWala sets never touched, and the layer the
 * on-device failures pointed at:
 *
 *  - PairIP's Application.<clinit> calls StartupLauncher.launch() —
 *    the encrypted startup program runs at CLASS LOAD, before
 *    attachBaseContext (which v1.56 rewrote) ever executes.
 *  - SignatureCheck.verifyIntegrity and the LicenseClient internals
 *    delegate their real work to VMRunner.invoke(name, args), whose
 *    only callee is the PRIVATE STATIC NATIVE executeVM in
 *    libpairipcore.so. Stubbing LicenseClient's verdict handlers
 *    (v1.56) cannot stop a VM program that acts from inside the
 *    native call itself.
 *
 * This patch stubs the entry points themselves, exactly as kuntal's
 * does: StartupLauncher.launch returns immediately, VMRunner.invoke
 * returns null without ever entering native code, VMRunner
 * .setContext returns immediately, SignatureCheck.verifyIntegrity
 * returns immediately and verifySignatureMatches returns true, and
 * the PairIP license ContentProvider can no longer start a check.
 * LicenseContentProvider1 existed in 24.5 but is absent in 27.3;
 * its stub is wrapped so it no-ops.
 *
 * The companion "Patch native libraries" patch neutralizes the
 * same VM at the native entry points as a second layer.
 *
 * Required for ANY re-signed DiskWala build to open, so it
 * defaults on.
 */
@Suppress("unused")
val bypassVmSignaturePatch = bytecodePatch(
    name = "Bypass PairIP VM and signature check",
    description = "Required for re-signed builds: PairIP runs an encrypted startup/license program inside its native VM (libpairipcore) at app start — before the license-check code the other patches stub. This stops the VM from ever executing (StartupLauncher and VMRunner return immediately), makes the signature check a no-op, and prevents the PairIP license provider from starting a check.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_DISKWALA)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // NOT wrapped in runCatching: these two stubs are the point of
        // this patch. If either fingerprint ever fails to match, the
        // patch must fail loudly instead of shipping a build whose
        // PairIP VM can still run.
        StartupLauncherLaunchFingerprint.method.addInstructions(0, "return-void")
        logger.info("DiskWala: PairIP StartupLauncher.launch stubbed — the startup program never runs.")

        VMRunnerInvokeFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
        logger.info("DiskWala: PairIP VMRunner.invoke stubbed to return null — executeVM is never entered.")

        runCatching {
            VMRunnerSetContextFingerprint.method.addInstructions(0, "return-void")
            logger.info("DiskWala: PairIP VMRunner.setContext stubbed.")
        }.onFailure { logger.warning("DiskWala: VMRunner.setContext stub skipped: $it") }

        runCatching {
            SignatureCheckVerifyIntegrityFingerprint.method.addInstructions(0, "return-void")
            logger.info("DiskWala: PairIP SignatureCheck.verifyIntegrity stubbed.")
        }.onFailure { logger.warning("DiskWala: verifyIntegrity stub skipped: $it") }

        runCatching {
            SignatureCheckVerifySignatureMatchesFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            logger.info("DiskWala: PairIP SignatureCheck.verifySignatureMatches pinned to true.")
        }.onFailure { logger.warning("DiskWala: verifySignatureMatches stub skipped: $it") }

        runCatching {
            LicenseContentProviderOnCreateFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            logger.info("DiskWala: PairIP LicenseContentProvider.onCreate pinned to true.")
        }.onFailure { logger.warning("DiskWala: LicenseContentProvider stub skipped: $it") }

        // Present in 24.5, absent in 27.3 — expected to no-op.
        runCatching {
            LicenseContentProvider1OnCreateFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            logger.info("DiskWala: PairIP LicenseContentProvider1.onCreate pinned to true.")
        }
    }
}
