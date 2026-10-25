# Published runtime source material

Scope: accepted compiled source e516aa89 and the unchanged487component/858file
inventory, exact app lock and133component runtime catalog. This stages source
material; it does not authenticate additional binaries, prove binary/source
build equivalence or approve complete corresponding source/distribution.
The [receipt](evidence/runtime-source-companions-e516aa89/review.json) retains
exact inputs, capture URLs, archive pins and review boundaries.

## What is now staged

Of133selected coordinates,95have frozen binary archives. The bounded collector
retrieved94published source companions without an error:9,438,796compressed
bytes,3,673code files/36,243,975source bytes. Every actual archive passed full
CRC, SHA256 and a second replay of its sorted per-member digest manifest.
Eighty-seven companions contain code. Seven have no source payload; their
**exact frozen binaries were separately downloaded and hashed**, with zero
class definitions in the outer or nested JARs. These are small compatibility
shells, not seven unaccounted implementations. Original small manifests,
resource XML and rules remain recorded; no resource exemption is inferred.

The known empty DataStore repackaged-Protobuf source companion was not
retried. Its existing release build/catalog mapping pins upstream
protobuf-javalite4.28.2 and namespace relocation. The upstream published source
archive is now staged too:453804B/SHA256
2460a92938259e4441826e57638193d0bde9cb5cdd0dafd5367aacb833f9a971,
159Java files/3,100,354bytes. This is original upstream source **before** the
AndroidX relocation; it does not prove the repackaged JAR can be reproduced.
The original BSD notice and pinned AndroidX build mapping remain in the
[license owner](runtime-license-review.md#corrected-protobuf-omission).

All38coordinates without a frozen binary now have exact frozen metadata
captured and reviewed:34have available-at variants pointing to already selected
binary components, three are POM-packaged BOMs with dependency management, and
kotlin-stdlib-common2.4.21 has one dependency-only variant pointing to the
selected stdlib2.4.21. Full raw metadata, all candidate targets/attributes and
pins are retained in metadata-ownership.json and metadata/. This establishes
static source-owner relations, not an actual Gradle variant-selection trace or
a general source exemption. Every recorded candidate target already has a
frozen binary and an inspected source companion.

## Repeat a source capture deliberately

No automatic retry, Android build, signing or trust update occurs. Use a fresh
output and the three independently inspected pins:

```sh
python3 scripts/capture-runtime-sources.py \
  docs/feasibility/evidence/gradle-baseline-f9434ab9.xml \
  app/gradle.lockfile LICENSES/runtime-components.json /new/runtime-source-stage \
  --expected-metadata 1fd918b7e439fb421cd18848d37f0d2a744aa6fb623553e6b7a77d38d43b5755 \
  --expected-lock 82e2f2f1f037385a526645b68e25bc1ac12a1c9882e3004b929d42cc3c3c3935 \
  --expected-catalog f3b2d280a7c140e431ff643565c6bab8fe01f04edfb6bbfef9c7e3d073c8b407
```

After capture, from its output directory run sha256sum -c against the committed
SOURCE-SHA256SUMS.txt at the receipt path. A new capture is not accepted if any
expected archive changes or is missing. All94source URLs and per-archive/member
pins are durable text; no new heavy binary Git upload is necessary. The separate
Protobuf upstream URL/digest is in protobuf-upstream.json. Retrieve that exact
URL into a fresh file and verify its recorded SHA256 before use; do not treat
its Maven filename as equivalence to the relocated AndroidX binary.

The collector uses only official Google Maven/Maven Central HTTPS routes with
restricted redirects, six workers, a120second deadline, bounded compressed and
expanded sizes, safe unique ZIP names and CRC reads. It never extracts or runs
archive code. Failures are recorded without retry. Six offline tests cover
unsafe/duplicate/symlink members, corruption/size limits, URL/redirect limits,
input/catalog selection pins, the known-empty exception and failure/output
reuse behavior. Run python3 scripts/test-runtime-source-capture.py.

## Remaining release work

Review the actual packaged closure against the staged source material,
including relocation, generated code and required build materials. Assemble
final corresponding-source assets with exact project/full WebP sources, NDK
helper and AndroidX native source/notices after the final accepted-source freeze.
Published source companions alone are not a complete offline toolchain or a
proof every source/resource/build obligation is fulfilled. The current app's
GPL identity and existing original upstream notices remain unchanged. Publisher
review325/858, full distribution-source review, physical acceptance and
separately authorized production signing/publication retain their own gates.


## Actual packaged class owners and missing materials

The [APK owner audit](evidence/runtime-source-closure-e516aa89/review.json)
reads the accepted full b677 APK and all95actual frozen binary archives
(40,739,883bytes). Every archive matches its frozen SHA256; the initial timed-out
captures, six local exact-hash reuses, eleven curl completions and two successful
same-publisher alternate URL completions remain separate records. No publisher
row is approved by this download.

All16,986DEX class names now have an explicit bounded category:13,245match a
class in a frozen archive,2,961have a D8 generated-name relation to such a class,
212are R/R-member names tied to actual AAR manifest namespaces, and568are in
the accepted project namespace. There are zero unassigned class names. The
lifecycle-viewmodel anchor is a real small AAR manifest namespace; it must not
be guessed from a general androidx prefix. Duplicate owners fail closed.
Seven offline tests cover class parsing, SourceFile/internal-name integrity,
malformed/truncated input, nested classes, explicit resource namespaces, JVM
package overrides and generated-name relations requiring an existing origin.

Class/name attribution is not source/binary equivalence. The398remaining
package/SourceFile-name mismatches are retained diagnostics, including79absent
SourceFile attributes. They are not398unaccounted libraries: Kotlin inlining,
JVM package overrides, generated inspection/AIDL/parcel classes and repackaging
make this debug attribute unsuitable as a source-completeness oracle. Do not
silently mark its unmatched cases as verified original source.

The audit found two concrete omissions in the earlier staged material:

* DataStore's JNI translation unit was missing beside its CMake/shared-counter
  sources. The exact1.1.7release source at420da5f2 now supplies the original
  2370byte file, SHA256adde5c7d79854f584e413277a03bd25ba4055c110d70c327d42f496e8fc4ce93,
  matching Git blob7a02dac044499fa19ad5020a1d3eb6d7edeb800e and its pinned
  directory. Its only local include resolves to the already retained header.
* Emoji2's source companion omits its bundled FlatBuffers/metadata code.
  Its1.4.0release build/settings identify the Noto external project and two
  namespace relocations.24original FlatBuffers1.12Java files, the historical
  Noto1.12build/schema and two original generated metadata Java files are now
  retained with original notices. The frozen classes expose the1.12marker;
  historical Noto tag android-13.0.0_r52 resolves to896f6b4d. This does **not**
  prove that tag is Emoji2's exact build checkout. Current Noto main uses2.0
  and was not substituted. A naive namespace-only byte comparison matches0of45
  classes; that negative result is preserved, not promoted to equivalence.

Five original Core1.16stable AIDL files and its Kotlin build file are also
retained from the official release endpoint096ee90d. Initial old aidl/Groovy
paths returned404; inspected directories established the actual stableAidl/
Kotlin layout before the successful capture. This supplies generation inputs
without pretending their output was independently rebuilt. All original
headers, source URLs, hashes and explicit limits remain in the receipt.

## Assembled source-material asset

[The bundle receipt](evidence/source-material-bundle-e516aa89.json) records a
real32,779,542byte source-material archive with1,282safe unique regular members,
SHA256b9e007771a2a18aa3a76d30cdff0fa82346251afbfc1fab9cc93ed083b9b2693.
Two full generations are byte-identical on the recorded Python/zlib runtime.
Every member/manifest hash, normalized ownership/mode/time and original nested
project/WebP source-kit hash passes. It adds the94companions, upstream Protobuf,
1,074original AAR resource/manifest/rule inputs from72coordinates and the
supplemental/review material. Resources are original published AAR inputs;
this does not prove each is the original preferred editing form.

The offline assembler is scripts/prepare-source-material-bundle.py. It requires
the exact existing project/WebP kit, source-stage directory,95frozen binaries,
committed source/closure evidence and pinned upstream Protobuf file; outputs
must be fresh. It downloads/builds/signs/uploads nothing. The committed producer,
URLs and byte pins make this source-material stage recoverable without another
large Git upload. Preserve the original source kit's separate Git-restoration
procedure. No complete offline toolchain or source/binary equivalence follows.
Final distribution/source review, including the exact external relocation and
generation context, remains open alongside publisher and physical/signing gates.
