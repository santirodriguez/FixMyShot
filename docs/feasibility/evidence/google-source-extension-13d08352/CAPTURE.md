# Capture scope and replay

This evidence extends the exact frozen f9434ab9 inventory from the 297-file
checkpoint at 13d083523658569b675ee0aed674b7286b008e77. It is not active trust.
The original report.json is retained without modification. Its collector hash
identifies the unchanged review-runtime-source-copies.py helper, whose network
and source-byte checks were reused. Its runtime scope label is historical:
the exploratory adapter explicitly replaced its selection with every then-pending
Google-owned .module identity. No runtime membership follows from that label.
The independent reviewer recomputes the full 47-file selection from the frozen
metadata and exact prior receipt. The precise selection adapter was:

```python
def inputs(metadata, prior_bytes, lock, prior_pin, lock_pin):
    prior, rows, known = m.batch.checked_receipt(prior_bytes, metadata, prior_pin)
    assert m.batch.digest(lock) == lock_pin
    selected = []
    for identity, row in sorted(rows.items()):
        if (identity not in known and
                row['group'].startswith(('androidx.', 'com.android.')) and
                row['file'] == row['name']+'-'+row['version']+'.module'):
            selected.append((identity, row))
    assert len(selected) == 47
    return prior, rows, selected
m.inputs = inputs
```

The imported collector supplied six workers, a 120-second/32-MiB total budget,
15-second per-request maximum, bounded directory/file reads and no redirects
or automatic retries. It selected 47 new exact module routes, not the previously
unavailable POM routes. There were 26 byte matches and 21 directory HTTP 404s.
Those 21 remain pending; do not automatically retry them at the same release.
Raw UTF8 .bin files were renamed .module with byte-for-byte preservation.
No downloaded code was executed. Original directory JSON framing is retained.

The two Guava proofs use guava.directory.json from the same immutable Google
AOSP release and actual JAR/POM bytes already in runtime-publishers-04a8aa02.zip.
Their recalculated Git blobs match the directory's exact unique regular entries.
guava-ownership.json preserves original README and futures/listenablefuture1/pom.xml
from google/guava commit 5ae56d77e64305752b919ff2c96713f47ac0c94c, with exact
connector blob identities. These establish Google's ownership of this exact
project/artifact; they do not expand trust to other mirrored publishers or every
com.google.* artifact. This is source-copy evidence, not a detached PGP verdict.

After the preceding replay produces PRIOR297, from the repository root:

```sh
python3 scripts/review-google-source-extension.py \
  docs/feasibility/evidence/gradle-baseline-f9434ab9.xml \
  "$PRIOR297" \
  docs/feasibility/evidence/google-source-extension-13d08352 \
  docs/feasibility/evidence/runtime-publishers-04a8aa02.zip \
  /tmp/fixmyshot-reviewed325
```

Expected: 325 reviewed / 533 pending, with exactly 28 new proofs and every
prior 297 proof unchanged. Compare both output files byte-for-byte with
`gradle/migration-f9434ab9/`. Runtime-associated files: 217 reviewed / 11 pending;
other captured files: 108 reviewed / 522 pending. review.json pins the exact
producer/report/source/ownership/archive/candidate bytes and all outcomes.
No new binary upload or Android/OSV rerun is needed for these evidence-only changes.
