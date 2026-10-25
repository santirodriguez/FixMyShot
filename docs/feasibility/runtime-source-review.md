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
