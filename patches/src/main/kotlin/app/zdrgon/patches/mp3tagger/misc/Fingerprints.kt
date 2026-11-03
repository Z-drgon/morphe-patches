package app.zdrgon.patches.mp3tagger.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Pro state reader — Smart MP3 Tagger 26.8.130 ──────────────────────
//
// Direct Play Billing (no RevenueCat/Adapty). Four products feed ONE
// unified Pro boolean; there is no separate subscription-vs-lifetime
// state anywhere in the app:
//
//   one-time  tageditor...mp3edit.remove_ads   "Super Pro Forever" $49.99
//   sub       com.smarttageditor.sub_pro       monthly $2.99
//   sub       com.smarttageditor.sub_pro_7     weekly  $1.99
//   sub       com.smarttageditor.sub_pro_year  yearly  $31.99
//
// The billing wrapper (Lc52;, classes.dex) keeps the owned products in
// a local in-memory list; Lc52;->b(Ljava/lang/String;)Z answers "is
// this SKU owned" from that list, and it has exactly ONE caller in the
// whole app — the reader below. No server is consulted at read time
// (no dev backend exists in the app at all):
//
//   Lmr0;->a()Z   (PUBLIC FINAL)
//     -> c52.b("...remove_ads") || c52.b("com.smarttageditor.sub_pro")
//        || c52.b("com.smarttageditor.sub_pro_year")
//        || c52.b("com.smarttageditor.sub_pro_7")
//
// Verified 2026-11-03 (androguard xref census of classes.dex):
// 11 call sites — MainActivity.p, MoreTabFragment.D, Lq5; (4 sites),
// Lt5;, Lt8;, Lu8;, Lup1;, Lx5;. The raw state fields of Lmr0; are
// never read or written outside the class itself, so nothing bypasses
// the reader.
//
// NOTE: Morphe fingerprints compare access flags by EXACT equality;
// the method is declared "public final".

/** The single Pro-state reader. Pinning it pins the whole app. */
object IsProFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    definingClass = "Lmr0;",
    name = "a"
)
