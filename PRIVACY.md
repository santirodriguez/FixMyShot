# Privacy - unreleased development preview

The development launcher uses the Android system Photo Picker to import only images selected by the user. It accepts bounded SDR JPEG/PNG/WebP sources for local review; originals are not modified. The external picker/provider may retrieve a cloud-backed photo independently, but FixMyShot itself requests no Internet or broad photo-library permission and does not upload images. There are no accounts, advertising or analytics. Application backup is disabled.

Selected files are copied into app-private cache, initially limited to 100 MiB per input, 1 GiB staged sources and 100 images per batch. Private staged copies may still contain original identifying metadata; the future removal policy does not erase their source bytes. A SHA-256 fingerprint is held in the draft to detect source changes before later export. Previews and bounded original-image thumbnails remain local; the thumbnail cache is limited to 24 MiB of resident bitmap data and may be cleared when Android reports memory pressure.

Discarding a session removes its currently tracked staged copies. On a later app opening, abandoned staged source files older than 24 hours are pruned. A crash or sudden process termination may delay this cleanup; it is not a secure-erasure promise. Clearing app data or uninstalling removes ordinary app-private state according to Android storage behavior. The current preview still has **no public save, gallery export or share-result path**.

The synthetic instrumentation harness runs only when explicitly launched by developers or CI. It uses generated pixels, writes temporary app-private candidates, deletes those candidates after tests and retains a local synthetic report containing device/API information and test measurements. CI uploads its synthetic emulator report as development evidence; the installed launcher has no report-upload function.

The implemented codec boundary accepts an already-decoded sRGB bitmap, not an original image file or its metadata. It generates a new private PNG/WebP candidate and does not copy source metadata. This is not yet acceptance of the complete future import, metadata sanitization or recovery pipeline.

Future functional privacy behavior, including process-death recovery, output metadata-preservation limits and external output sharing, remains governed by the [blueprint](docs/blueprints/first-functional-release.md). Metadata removal will not redact text or identities visible in image pixels. No production privacy website is claimed to be deployed.
