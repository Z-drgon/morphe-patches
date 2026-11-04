package app.zdrgon.patches.diskwala.native

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.zdrgon.patches.diskwala.shared.Constants.COMPATIBILITY_DISKWALA
import java.io.RandomAccessFile
import java.util.logging.Logger

// ── DiskWala 27.3 libpairipcore.so — symbol-derived entry points ──────
//
// Kuntal's "Patch native libraries" (24.5) writes RET stubs into
// libpairipcore.so at HARD offsets (JNI_OnLoad 0x8b52c, ExecuteProgram
// 0x6bf70, plus seven internal sites). Those offsets do NOT transfer
// to 27.3 — verified 2026-11-03 against Matsen's exact XAPK:
// 27.3's lib is 608,896 bytes and its .dynsym places the exports
// elsewhere, and the bytes at kuntal's offsets are mid-function
// instructions (writing there would corrupt the library):
//   JNI_OnLoad     0x8bc3c (size 6212)   kuntal: 0x8b52c ✗
//   ExecuteProgram 0x6c7d0 (size 1896)   kuntal: 0x6bf70 ✗
//   JNI_OnUnload   0x8d480 (size 832)
// The MECHANISM transfers; the addresses were re-derived from the
// 27.3 .dynsym. Both 27.3 entries start with textbook AArch64
// prologues, recorded below as a guard: the patch writes ONLY if
// the file size and both stock prologues match exactly, and throws
// otherwise (a loud failure beats a silently corrupted library).
// Kuntal's other 24.5 native targets (libcrypto.so, libreactnative
// .so, libhermes.so, libhermestooling.so,
// libreactnativequickcrypto.so, assets/index.android.bundle) do
// not exist in 27.3 at all — 27.3's merged APK carries 18 .so
// files, none of them React Native/Hermes — so they are not ported.

private const val PAIRIPCORE_SIZE = 608896L

private const val JNI_ONLOAD_OFFSET = 0x8bc3cL
private const val EXECUTE_PROGRAM_OFFSET = 0x6c7d0L

/** Little-endian first instruction word of the stock entry. */
private fun wordAt(bytes: ByteArray, offset: Long): Long {
    val o = offset.toInt()
    return (bytes[o].toLong() and 0xFF) or
        ((bytes[o + 1].toLong() and 0xFF) shl 8) or
        ((bytes[o + 2].toLong() and 0xFF) shl 16) or
        ((bytes[o + 3].toLong() and 0xFF) shl 24)
}

// Stock 27.3 prologues: JNI_OnLoad = sub sp, sp, #0x130 (d104c3ff);
// ExecuteProgram = sub sp, sp, #0x1a0 (d10683ff).
private const val JNI_ONLOAD_STOCK_WORD = 0xd104c3ffL
private const val EXECUTE_PROGRAM_STOCK_WORD = 0xd10683ffL

private fun arm64(vararg instructions: Long): ByteArray {
    val buf = ByteArray(instructions.size * 4)
    for (i in instructions.indices) {
        val v = instructions[i]
        buf[i * 4 + 0] = (v and 0xFF).toByte()
        buf[i * 4 + 1] = ((v shr 8) and 0xFF).toByte()
        buf[i * 4 + 2] = ((v shr 16) and 0xFF).toByte()
        buf[i * 4 + 3] = ((v shr 24) and 0xFF).toByte()
    }
    return buf
}

/**
 * DiskWala — Patch native libraries (27.3 port of kuntal's patch).
 *
 * Neutralizes PairIP at its two exported native entry points in
 * libpairipcore.so: JNI_OnLoad returns JNI_VERSION_1_6 (0x10006)
 * immediately — registering no VM natives and running no native
 * startup checks — and ExecuteProgram, the PairIP VM interpreter
 * entry, returns 0 without executing any VM program. Combined with
 * the dex-side "Bypass PairIP VM and signature check" patch
 * (VMRunner.invoke stubbed, so executeVM is unreachable from Java),
 * no PairIP VM program can run in a re-signed build.
 *
 * Required for ANY re-signed DiskWala build to open, so it
 * defaults on.
 */
@Suppress("unused")
val nativeLibraryPatch = rawResourcePatch(
    name = "Patch native libraries",
    description = "Required for re-signed builds: neutralizes PairIP inside libpairipcore.so itself — JNI_OnLoad returns success without registering the PairIP VM, and ExecuteProgram (the VM interpreter) returns without running any PairIP program. Offsets were re-derived for 27.3 from the library's exported symbols and are verified against the stock bytes before anything is written.",
    default = true
) {
    compatibleWith(COMPATIBILITY_DISKWALA)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val path = "lib/arm64-v8a/libpairipcore.so"
        val soFile = get(path, true)
        if (!soFile.exists()) {
            throw PatchException(
                "DiskWala Patch native libraries: $path not found — the " +
                    "libraries ship in the XAPK's split, so patch the full " +
                    "XAPK bundle, not a base-only APK"
            )
        }

        val bytes = soFile.readBytes()
        if (bytes.size.toLong() != PAIRIPCORE_SIZE ||
            wordAt(bytes, JNI_ONLOAD_OFFSET) != JNI_ONLOAD_STOCK_WORD ||
            wordAt(bytes, EXECUTE_PROGRAM_OFFSET) != EXECUTE_PROGRAM_STOCK_WORD
        ) {
            throw PatchException(
                "DiskWala Patch native libraries: libpairipcore.so does " +
                    "not match the verified 27.3 build (size " +
                    "${bytes.size}, expected $PAIRIPCORE_SIZE with stock " +
                    "prologues at JNI_OnLoad/ExecuteProgram) — refusing " +
                    "to write at unverified offsets"
            )
        }

        RandomAccessFile(soFile, "rw").use { raf ->
            // JNI_OnLoad -> mov w0, #6; movk w0, #1, lsl #16; ret
            // (returns JNI_VERSION_1_6 = 0x10006, registers nothing)
            raf.seek(JNI_ONLOAD_OFFSET)
            raf.write(arm64(0x528000c0, 0x72a00020, 0xd65f03c0))
            // ExecuteProgram -> mov x0, #0; ret
            raf.seek(EXECUTE_PROGRAM_OFFSET)
            raf.write(arm64(0xd2800000, 0xd65f03c0))
        }
        logger.info("DiskWala: libpairipcore.so neutralized — JNI_OnLoad @0x8bc3c returns JNI 1.6, ExecuteProgram @0x6c7d0 returns 0.")
    }
}
