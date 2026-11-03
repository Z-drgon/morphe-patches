package app.zdrgon.patches.idm.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── 1DM 20.0 (30257) — ad funnels ──────────────────────────────────────
//
// Census of the merged dexes (88,078 classes), verified 2026-11-03:
//
// The app requests ads through exactly two funnels.
//
// 1) Appodeal mediation (AppLovin MAX, Mintegral, Pangle, Unity Ads,
//    Chartboost, etc. sit behind it). The app-level entry points are
//    the static Appodeal API itself — the app has no separate loader
//    layer above it:
//      - MyAppCompatActivity.initializeAdsAndRun initializes Appodeal
//        with a type mask of 3 or 7 and registers the banner view id.
//      - The obfuscated ad manager Li/om; (BannerCallbacks +
//        InterstitialCallbacks) drives cache/show for the rotation.
//    Every Appodeal.show call site in app code (Li/b0; type 3
//    interstitial, Li/j5; and Li/nm; type 64 banner/native slot,
//    Li/x73;) treats a false return as the designed "no ad" outcome
//    (runs its continuation / notifies not-showing), so pinning show
//    to false is the safe off state. cache() is fire-and-forget
//    preloading; no-oping it starves the rotation.
//    Formats in use: interstitial (type 3) and the banner/native
//    slot (type 64, coordinated with the app's BannerManager).
//    MREC and native-feed views are never requested by app code
//    (zero app callers of getMrecView / getNativeAds /
//    getBannerView). REWARDED: no opt-in rewarded flow exists — zero
//    app callers of setRewardedVideoCallbacks or getReward, and the
//    init mask never includes a rewarded type. Nothing rewarded is
//    touched because there is nothing rewarded to touch.
//
// 2) Amazon Publisher Services (APS/DTB), a direct second path that
//    does NOT go through Appodeal:
//      - AmazonService.fetch() is the single internal funnel: its
//        fetchBanner / fetchBannerBackfill / fetchInterstitial are
//        called ONLY from fetch(). Its caches feed the browser's
//        BannerView.setAd (banner backfill) and Li/x73; (interstitial).
//        With fetch() no-oped the caches stay empty and consumers get
//        null, a state stock code already handles before the first
//        fetch completes.
//      - Li/g0; is a separate direct APS fetcher used by the Amazon
//        fallback case in Li/b0;: its two (Z)DTBAdResponse methods
//        synchronously load a DTBAdRequest. b0 null-checks the
//        response and bails to its continuation runnable on null, so
//        pinning both to null is safe.
//
// 3) The house promo strip (NewBannerView, id/defaultBannerViewNew)
//    is NOT a network ad: it is the app's own cross-promo view,
//    populated from Lcom/aspsine/multithreaddownload/core/BannerInfo;
//    (e.g. the baked-in NordVPN offers — strings "Click to Install
//    NordVPN", "Hide VPN offer"). Its show trigger is
//    Li/l82;->ۦۖ۬(MyAppCompatActivity)Z: it finds the NewBannerView
//    in the activity and schedules the fill/show runnable. Pinning
//    the trigger to false means the strip is never scheduled; the
//    rotation treats false as "not shown" and moves on.
//
// NOTE: Morphe fingerprints compare access flags by EXACT equality.
// The obfuscated method names below are the exact dex names, verified
// codepoint-by-codepoint against the 20.0 dexes:

/** Appodeal show funnel — every format the app displays goes through it. */
object AppodealShowFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Landroid/app/Activity;", "I"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    definingClass = "Lcom/appodeal/ads/Appodeal;",
    name = "show"
)

/** Appodeal preload (2-arg). Fire-and-forget; starves the rotation. */
object AppodealCacheFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "I"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    definingClass = "Lcom/appodeal/ads/Appodeal;",
    name = "cache"
)

/** Appodeal preload (3-arg, with count). Same funnel, belt and braces. */
object AppodealCacheWithCountFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "I", "I"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    definingClass = "Lcom/appodeal/ads/Appodeal;",
    name = "cache"
)

/** AmazonService.fetch() — the single funnel for all APS bid caching. */
object AmazonServiceFetchFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PRIVATE),
    definingClass = "Lidm/internet/download/manager/amazon/AmazonService;",
    name = "fetch"
)

/** Li/g0; direct APS fetch #1 (name U+06E6 U+06D6 U+06D6). */
object AmazonDirectFetchFingerprint : Fingerprint(
    returnType = "Lcom/amazon/device/ads/DTBAdResponse;",
    parameters = listOf("Z"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    definingClass = "Li/g0;",
    name = "ۦۖۖ"
)

/** Li/g0; direct APS fetch #2 (name U+06E6 U+06D6 U+06DC). */
object AmazonDirectFetchBackfillFingerprint : Fingerprint(
    returnType = "Lcom/amazon/device/ads/DTBAdResponse;",
    parameters = listOf("Z"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    definingClass = "Li/g0;",
    name = "ۦۖۜ"
)

/** House promo (NewBannerView) show trigger in Li/l82; (name U+06E6 U+06D6 U+06EC). */
object HousePromoTriggerFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Lacr/browser/lightning/activity/MyAppCompatActivity;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    definingClass = "Li/l82;",
    name = "ۦۖ۬"
)
