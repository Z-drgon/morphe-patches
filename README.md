# Matsen's Morphe Patches

Custom Morphe patches for Android apps. Add this repo as a source in Morphe Manager or Morphe Desktop, pick an app, and patch.

## How to use

1. Open **Morphe Manager** on your Android device.
2. Go to **Settings** > **Patch Sources** and add:
   ```
   https://github.com/mwbra130/Retro-Bowl-College
   ```
   Or tap: [Add to Morphe](https://morphe.software/add-source?github=mwbra130/Retro-Bowl-College)
3. Pick the app and the patch, then patch. **Uninstall the original app first** — patched builds are signed with a different key and won't install over the original.

   Tip: if the patches don't show up or look outdated, remove this source and add it again. Morphe Manager caches the patch bundle.

## Building locally

To build the patch bundle (`.mpp`) on your machine:

```powershell
.\gradlew.bat buildAndroid
```

The compiled patch package will be generated at:
`patches/build/libs/patches-*.mpp`

Load this `.mpp` file into [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) or Morphe Manager to patch your APKs.

---

<!-- PATCHES_START EXPANDED -->
> **[v1.35.1](https://github.com/mwbra130/Retro-Bowl-College/releases/tag/v1.35.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;25 patches total
<details open>
<summary>Strava&nbsp;&nbsp;•&nbsp;&nbsp;7 patches</summary>
<br>

**Supported versions:**

| 486.9 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Add settings](#add-settings) | Long-press the Home tab in Strava's bottom bar to open 'Morphe patch settings'. Lets you toggle feed cleanup, route trim, bottom bar tabs and the launcher name/icon at runtime, without re-patching. |  |
| [Custom app name and icon](#custom-app-name-and-icon) | Changes the app's displayed name and launcher icon. The icon choices are Strava's own built-in alternate icons, plus an accent-colored one. Name and icon can later be switched at runtime from the patch settings screen. | • App name<br>• App icon |
| [Custom theme colors](#custom-theme-colors) | Replaces Strava's brand orange (#FC5200) accent color in the app's native screens with a color of your choice. | • Accent color |
| [Enable password login](#enable-password-login) | Shows the email and password login option instead of forcing the one-time email code flow. Your password is still checked by Strava's servers exactly as in the official app. |  |
| [Hide feed elements](#hide-feed-elements) | Hides who-to-follow suggestions, suggested challenges and other clutter cards from the activity feed. Requires the 'Hide subscription promotions' patch, which provides the shared feed filtering. | • Who to Follow<br>• Suggested Challenges<br>• Join Challenge<br>• Joined a club<br>• Activity lookback<br>• Local events |
| [Hide subscription promotions](#hide-subscription-promotions) | Hides subscription upsell modules and promotional cards from the feed and other modular screens. UI cleanup only: does not unlock any paid features. | • Upselling<br>• Promotions |
| [Local route privacy trim](#local-route-privacy-trim) | Trims extra route distance from both ends of the activity map on this device only, on top of Strava's own privacy filtering. Display-only: recorded and uploaded data are unchanged, and other people still see the activity exactly as your Strava privacy settings dictate. | • Meters to trim |

</details>

<details open>
<summary>Subway Surfers&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Supported versions:**

| 3.70.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Block Ads](#block-ads) | Blocks all Unity Ads (interstitial and rewarded) by no-opping the show() methods. No ads will display. |  |
| [Skip Unity Ads](#skip-unity-ads) | Blocks forced interstitial ads — no fullscreen ad between runs. Rewarded ads still work normally and grant their reward. |  |

</details>

<details open>
<summary>Subway Surfers&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Supported versions:**

| 3.70.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Block Ads](#block-ads) | Blocks all Unity Ads (interstitial and rewarded) by no-opping the show() methods. No ads will display. |  |
| [Skip Unity Ads](#skip-unity-ads) | Blocks forced interstitial ads — no fullscreen ad between runs. Rewarded ads still work normally and grant their reward. |  |

</details>

<details open>
<summary>BeReal&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**Supported versions:**

| 3.97.1 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Block ads](#block-ads) | Blocks fullscreen interstitial ads and removes sponsored posts from feeds. |  |
| [Disable video autoplay](#disable-video-autoplay) | Videos stay paused until you tap them. Works in all feeds and profiles. |  |
| [Remove suggested-people card](#remove-suggested-people-card) | Removes the suggested-people card from the feed. |  |

</details>

<details open>
<summary>Gradient Weather&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Supported versions:**

| 1.1.1 | 1.3.4 |
| :---: | :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Change installer source](#change-installer-source) | Spoofs the installer source so the app appears to be installed from an app store. | • Spoofed package installer name |
| [Enable Premium](#enable-premium) | Unlocks Gradient Weather Premium. Use With Spoof Install Source |  |

</details>

<details open>
<summary>Geometry Dash Lite&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Supported versions:**

| 2.2.147 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Complete all levels](#complete-all-levels) | Marks every official level 100% complete with all secret coins. |  |

</details>

<details open>
<summary>Crossy Road&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**Supported versions:**

| 7.14.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Free store](#free-store) | The in-game store is free. Just tap "Buy" and the item is yours — no payment needed. For an ad-free game, just buy the ad-block item from the store. |  |
| [Unlimited gold](#unlimited-gold) | Your coin balance always shows 65,535 and spending coins always works without deducting any — pull the prize machine and buy coin characters as much as you want. |  |
| [Unlock all characters](#unlock-all-characters) | Every character is unlocked and playable — paid, coin, prize machine, progress and secret characters included. Your real collection progress is not changed; characters are unlocked while the patch is applied. |  |

</details>

<details open>
<summary>Smashy Road 2&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Supported versions:**

| 1.54 | 1.45 |
| :---: | :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Unlimited coins & unlocks](#unlimited-coins-unlocks) | Sets cash to 9,999,999, every upgrade card to 10,000, unlocks ALL vehicles and characters (no spinning needed), unlocks the Pro Pass, slot/lottery machines, completes all main + side missions, and maxes vehicle durability. Backs up your save before touching it. To revert: place an empty file named SR2_RESTORE.txt in Android/data/com.rkgames.basisgame/files/ and open the game. |  |
| [Unlock All Slot Machines](#unlock-all-slot-machines) | Unlocks all slot machines (1/2/3) without Google Play purchase. Patches the native ownership checks to always grant the machines. |  |

</details>

<details open>
<summary>Games In One - Watch Games&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Supported versions:**

| 0.4.5 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Unlock premium](#unlock-premium) | Unlocks all games (VIP). The store sees the one-time VIP purchase as owned — no real purchase is made. |  |

</details>

<details open>
<summary>Retro Bowl College&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Supported versions:**

| 1.1.2 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Unlock premium](#unlock-premium) | Unlocks the full version. No real purchase is made. |  |

</details>

<details open>
<summary>Web Video Caster&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Supported versions:**

| 6.1.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Unlock premium](#unlock-premium) | Unlocks Web Video Caster Premium (ad removal, ad block, bookmarks, queue and the other premium features). No real purchase is made. |  |

</details>

<!-- PATCHES_END -->

## App notes

**BeReal** (`com.bereal.ft`, XAPK) — the fingerprints target **3.96.0** specifically (the app is obfuscated, so class/method names change every release). If BeReal updates, patches may fail to apply until the fingerprints are re-derived. Uninstall the original first and log back in after installing the patched build — back up anything you care about first. No login, posting, or messaging functionality is touched.

**Geometry Dash Lite** (`com.robtopx.geometryjumplite`, XAPK) — open the game **twice**: the first launch creates the save file, the second applies the completions. Each launch (until it succeeds once) shows a short toast: "no save yet — reopen the game once", "could not read save, will retry", or "marked N levels + M coins complete — reopen the game". Diagnostics land in the app's external files dir (`/Android/data/com.robtopx.geometryjumplite/files/`, readable over USB): `gdl_patch_before.xml`, `gdl_patch_after.xml`, `gdl_patch_log.txt`. Fake completions sync to RobTop's servers, which carries a ban risk.

**Retro Bowl College** (`com.newstargames.retrobowlcollege`, v1.1.2) — uninstall the original game first (patched builds are signed with a different key and won't install over it), then install the patched APK.

**Crossy Road** (`com.yodo1.crossyroad`, XAPK, 7.14.0) — patch the full XAPK/APKM bundle, not a base-only .apk: in 7.14.0 the game libraries ship only in the bundle's split APKs, so a base-only file fails with a "no libil2cpp.so" error. Uninstall the original first, then install the patched build.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
