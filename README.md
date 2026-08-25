# 📡🧩 Zdrgon's Morphe Patches

Custom Morphe patches including Direct-to-Cell (DTC) Satellite Data Optimization for T-Mobile / SpaceX connectivity.

## ❓ About

This project contains custom patches for Morphe Manager & Morphe Desktop:

- **Satellite Data Optimization (Universal)**: Automatically injects `<meta-data android:name="android.telephony.PROPERTY_SATELLITE_DATA_OPTIMIZED" android:value="PACKAGE_NAME" />` into `AndroidManifest.xml` for any selected application (WireGuard, Lichess, YouTube ReVanced, browsers, etc.), enabling data usage over T-Mobile / SpaceX Direct-To-Cell satellite connections per Google's Android Satellite Connectivity guidance.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=Zdrgon/morphe-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/Z-drgon/morphe-patches/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
<details open>
<summary>📦 XYZ app&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 2.0.0 | 1.0.2 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Example Patch](#example-patch) | Example patch to start with. |  |

</details>

<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Satellite Data Optimization](#satellite-data-optimization) | Injects PROPERTY_SATELLITE_DATA_OPTIMIZED meta-data tag into AndroidManifest.xml to enable Direct-To-Cell (DTC) satellite data on T-Mobile / SpaceX network. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

UserXYZ Patches are licensed under the [GNU General Public License v3.0](LICENSE)
