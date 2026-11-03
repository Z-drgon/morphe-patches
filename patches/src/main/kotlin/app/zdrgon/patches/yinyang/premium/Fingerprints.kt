package app.zdrgon.patches.yinyang.premium

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Enable Premium funnel — Yin Yang Launcher 5.13.1 (189) ────────────
//
// Premium is RevenueCat (entitlement "pro_v1") merged on-device with a
// direct Play Billing ownership query (PlayOwnershipChecker ->
// PlayAnswerSource -> PlayProCheck -> PlayProLogic, Decision:
// USE_REVENUECAT / USE_PLAY / KEEP_CURRENT). SessionManager
// .updateSubscriptionSession folds both into a YSubscription
// (Lwa/log; isPremium / isLifetime / isPlayOnly), and emitSession then
// builds the ONE object every feature gate in the app reads:
//
//   Loc/for;  "SessionFlowData(yUser, ySubscription, isPremiumUser)"
//     fields: for = yUser, new = ySubscription, try = isPremiumUser
//     emitSession computes try = ySubscription.isPremium && app flag.
//
// 18 files read SessionFlowData.isPremiumUser directly (field try),
// including the Pro features from the paywall — appearance
// EditLookFragment / EditLookRedesignFragment (clocks, fonts, live
// wallpapers, icons), AppearanceSettingViewModel, homescreen views,
// MindfulPause / MindfulTimer / MindSpace, MySpace, TPWidgets and
// PromoCheckWorker. Nothing outside the auth package consults
// RevenueCat, PlayProCheck or the raw YSubscription to GATE a feature
// (paywall + profile screens read raw state for display only).
//
// SessionFlowData has exactly ONE constructor in 5.13.1 (verified in
// the classes3.dex smali):
//
//   .method public constructor <init>(Lcom/yinyang/launcher/features/auth/repo/YUser;Lwa/log;Z)V
//       .locals 0
//       invoke-direct {p0}, Ljava/lang/Object;-><init>()V
//       iput-object  p1, p0, Loc/for;->for:...
//       iput-object  p2, p0, Loc/for;->new:Lwa/log;
//       iput-boolean p3, p0, Loc/for;->try:Z     <-- isPremiumUser
//       return-void
//
// Fallback/default SessionFlowData instances built inside feature code
// pass through this same constructor, so forcing p3 to true here makes
// EVERY session the app ever builds report a premium user — precisely
// what a real Lifetime ($29.99) buyer's session carries.
//
// NOTE: Morphe fingerprints compare access flags by EXACT equality;
// the dex record for a constructor is PUBLIC | CONSTRUCTOR (0x10001).

/** The single SessionFlowData constructor — the isPremiumUser funnel. */
object SessionFlowDataConstructorFingerprint : Fingerprint(
    definingClass = "Loc/for;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Lcom/yinyang/launcher/features/auth/repo/YUser;",
        "Lwa/log;",
        "Z",
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)
