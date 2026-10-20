# Privacy - development bootstrap

The current development launcher does not import user images, save public exports, request permissions or communicate over a network. No accounts, advertising or analytics are included. Android backup and device-transfer rules exclude application content.

The synthetic instrumentation harness runs only when explicitly launched by developers or CI. It uses generated pixels, writes temporary app-private candidates, deletes those candidates after tests and retains a local synthetic report containing device/API information and test measurements. CI uploads its synthetic emulator report as development evidence; the installed launcher has no report-upload function.

The implemented codec boundary accepts an already-decoded sRGB bitmap, not an original image file or its metadata. It generates a new private PNG/WebP candidate and does not copy source metadata. This is not yet acceptance of the complete future import, metadata sanitization or recovery pipeline.

Future functional privacy behavior, including session cleanup, explicit metadata-preservation limits and external picker/share behavior, remains governed by the [blueprint](docs/blueprints/first-functional-release.md). Metadata removal will not redact text or identities visible in image pixels. No production privacy website is claimed to be deployed.
