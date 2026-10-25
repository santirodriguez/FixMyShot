# Native source identity and notices

Scope: WebP source4fa21912, NDK28.2.13676358 and the two AndroidX native
libraries present in the accepted e8 packages across three ABIs. This is a
bounded source/notice review, not full security, legal or patent clearance.
The [receipt](evidence/native-source-review.json) pins every retained raw file.

## Source and attribution checks

The official [WebP release tag](https://chromium.googlesource.com/webm/libwebp/+/refs/tags/v1.6.0)
objectb7e29b9d75bd31422b00c2a446d49d7af06c328d resolves to the existing
CMake pin4fa21912338357f89e4fd51cf2368325b59e9bd9 and tree
3a7830b9713d9187cf1211862d4571ff58b0d507. A complete shallow Git fetch
from the publisher's GitHub mirror passed git fsck; all343 blobs/8073794bytes
were read and individually rehashed. The raw official commit/root response and
full per-file Git/SHA256 manifest agree with that tree. Source COPYING and
PATENTS exactly match the existing packaged canonical bytes. AUTHORS and the
reviewed upstream CMake/dependency files are retained as source evidence. No
release-tag signature was authenticated, and the manifest is not a source archive.

libwebp's Android CMake target additionally compiles the installed NDK's
`sources/android/cpufeatures/cpu-features.c`. Official
[ndk-r28c](https://android.googlesource.com/platform/ndk/+/refs/tags/ndk-r28c)
identifies build13676358, tag objecta51e1194e5ab334be5bace0a13bdbd7047a5a896
and commit805836ef53231ef86dd5fcc5652a1c28d74f4bc4. Both C/header carry
the same BSD two-clause copyright/conditions/disclaimer; the separate module
NOTICE is Apache-2.0. Merely bundling a general Apache license omitted the
BSD text. LICENSES/Android-NDK-cpufeatures.txt now preserves both original
texts verbatim, with their original framing. Existing legal-asset generation
packages it, THIRD_PARTY_NOTICES identifies its use and the package checker
now requires eight exact canonical assets. This changes notice bytes only.

`scripts/check-ndk-native-notices.py` compares the actual installed SDK source
C/header/NOTICE to these pinned release hashes, verifies package revision, and
checks the combined canonical attribution before recording the raw actual
inputs. The grouped Android build executes that check. Offline source replay
and canonical omission/same-size mutation refusals passed. Normal build40196478234 now passes the actual installed SDK check and both
eight-asset APK checks against exact archived source. Source382-file tree,
wrapper/locks, core69/lint/threeAPKs and native alignment pass. API36 also passed; its actual downloaded smoke evidence and all four report
guards were independently checked. Exact-source independent rebuild40197042011 also passed. Its actual downloaded
unsigned APK is byte-identical to the original5d package; the new source has
its own bounded build/runtime/reproducibility acceptance. See [the package receipt](evidence/android-5d568a7e.json).

The accepted package's other native names are libandroidx.graphics.path.so
and libdatastore_shared_counter.so. The official
[graphics-path1.0.1 entry](https://developer.android.com/jetpack/androidx/releases/graphics#graphics-path-1.0.1)
links release endpoint8a05a22af450d589ef911d772a001a49dcb05b71. Its captured
C++ and math headers have Apache-2.0 notices; each raw blob matches its pinned
Gitiles directory membership. The published9908byte sources JAR contains
Kotlin only, so native source availability was established through release
source rather than inferred from that archive. DataStore1.1.7's release
commit420da5f291449f11801e378c6789a328a06ec6cb similarly retains the shared
counter CMake/C++/header with Apache notices. No additional Skia notice was
invented merely because the path code interoperates with Android graphics.
These source records do not reproduce Google's AAR builds or prove all their
classes/functions are included in the final APK.

## Remaining limits and reproduction

Independent builders can fetch the exact upstream WebP commit used by the
tracked CMake configuration and compare the per-file manifest. Keep all source
notices in a complete corresponding-source checkout. A final distribution
still needs its recoverable full source/native build materials staged with
the release artifacts; the exact project/full-WebP source kit is now prepared and validated as
described below. No complete corresponding-source, signature or publication
approval follows. General build-tool licensing, remaining533 publisher files, owning-tool
security and physical/runtime/distribution gates remain separate.

To replay the NDK boundary with an installed exact revision, run:

```sh
python3 scripts/check-ndk-native-notices.py \
  "$ANDROID_HOME/ndk/28.2.13676358" /tmp/fixmyshot-ndk-notices
```

The output path must be fresh. It contains only source text, source.properties
and a bounded receipt. Do not substitute source from a different NDK version.


## Validated source-kit staging

[The source-kit receipt](evidence/source-kit-e516aa89/review.json) records the
accepted e516aa89project source (468files) and the full previously audited WebP
source (343files/8073794B). Two complete archive generations are identical;
all818safe regular members, per-file manifest entries and normalized metadata
pass. Both original public commit/tree identities restore as pristine local
Git repositories, preserving executable modes and wrapper CRLF. No source
fetch/build/sign/upload occurred in packaging or restoration. Full other-runtime
source materials and final corresponding-source review remain open. See
[the preparation procedure](../RELEASING.md#02c--stage-recoverable-project-and-native-sources).
