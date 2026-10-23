<!-- Source template. check-release.py can render version fields into a NEW local draft.
     Edit every EDIT marker, reconcile current limitations and verify final asset links.
     Do not publish this template or interpret rendered fields as acceptance evidence. -->

<p align="center">
  <img src="https://raw.githubusercontent.com/santirodriguez/FixMyShot/{{TAG}}/assets/branding/header.png" alt="FixMyShot" width="760" />
</p>

<h1 align="center">FixMyShot {{VERSION}}</h1>
<p align="center"><strong>Screenshots, ready anywhere.</strong><br />A little less screenshot gymnastics.</p>

> [!IMPORTANT]
> **UNPUBLISHED DRAFT.** Replace this notice with the verified channel and meaningful limitations only after candidate acceptance. Rendering this page does not make a release.

## The good stuff

<!-- EDIT: One sentence explaining the practical benefit of this version. -->

| What's new | Why it helps |
|---|---|
| <!-- EDIT: feature --> | <!-- EDIT: user-visible result --> |

## Smoother around the edges

<!-- EDIT: Concise improvements and fixes. Remove empty sections. Keep technical receipts below. -->

## Before you install

<!-- EDIT: Actual supported Android versions, format restrictions, unresolved issues and migration notes.
     Never omit remaining alpha/native/upgrade gates or call an unexecuted check a pass. -->

Images are processed locally. New sessions start with PNG and identifying-metadata removal. Originals remain unchanged. Removing metadata does not redact visible content.

## Get the app

<!-- EDIT: These links resolve only AFTER the exact assets are published. Verify them then. -->

| File | What it is |
|---|---|
| [{{APK}}](https://github.com/santirodriguez/FixMyShot/releases/download/{{TAG}}/{{APK}}) | The single signed universal APK |
| [SHA256SUMS.txt](https://github.com/santirodriguez/FixMyShot/releases/download/{{TAG}}/SHA256SUMS.txt) | SHA-256 for the APK and build receipt |
| [BUILD-INFO.json](https://github.com/santirodriguez/FixMyShot/releases/download/{{TAG}}/BUILD-INFO.json) | Source, unsigned/signed digests, public certificate and build-tool receipt |

**Obtainium:** add `https://github.com/santirodriguez/FixMyShot`, enable prereleases for the 0.x channel and select the universal APK if needed. APK filter: `^FixMyShot-.*-universal\.apk$`. Fresh install and same-certificate upgrade must be verified on these exact bytes before claiming them supported.

<details>
<summary><strong>Under the hood · build {{CODE}}</strong></summary>

<!-- EDIT: Bind the exact source commit, signed APK SHA-256, public certificate SHA-256,
     independent unsigned rebuild result and actual device/install/upgrade evidence.
     Do not include private keys, passwords, operator paths or raw user-image diagnostics. -->

- Application ID: `org.cajapersonal.fixmyshot`
- Version: `{{VERSION}}` · versionCode `{{CODE}}` · tag `{{TAG}}`
- [Source and build instructions](https://github.com/santirodriguez/FixMyShot/tree/{{TAG}})
- [Privacy](https://github.com/santirodriguez/FixMyShot/blob/{{TAG}}/PRIVACY.md) · [Format limits](https://github.com/santirodriguez/FixMyShot/blob/{{TAG}}/docs/FORMATS.md)

</details>

Made by [Santiago Rodriguez](https://santiagorodriguez.com) · [Support development](https://santiagorodriguez.com/donate) · GNU GPLv3 only.
