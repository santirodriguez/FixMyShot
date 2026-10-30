package org.cajapersonal.fixmyshot.avifphysical;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Explicit user-initiated physical AVIF feasibility: no permissions, telemetry,
 * screenshot upload, external storage write, or production exporter integration.
 */
public final class ProbeActivity extends Activity {
    private static final int OPEN_IMAGE = 101;
    private static final int SAVE_REPORT = 102;
    private static final int WIDTH = 1344;
    private static final int HEIGHT = 2992;
    private static final int REAL_TRIALS = 3;
    private static final long SAMPLE_INTERVAL_MS = 150;

    static { System.loadLibrary("avif_physical"); }
    private static native long[] encodeBitmap(Bitmap pixels, String privatePath);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Bitmap source;
    private Bitmap reopened;
    private File encoded;
    private JSONObject report;
    private boolean busy = false;
    private boolean synthetic = false;
    private TextView status;
    private ImageView preview;
    private Button pick;
    private Button run;
    private Button original;
    private Button compressed;
    private Button good;
    private Button bad;
    private Button save;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int inset = dp(16);
        content.setPadding(inset, inset, inset, inset);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);

        TextView guidance = new TextView(this);
        guidance.setText(R.string.instructions);
        guidance.setTextSize(16);
        content.addView(guidance);
        TextView warning = new TextView(this);
        warning.setText(R.string.privacy_notice);
        content.addView(warning);
        status = new TextView(this);
        status.setText(R.string.no_input);
        status.setTextSize(16);
        status.setPadding(0, dp(12), 0, dp(12));
        content.addView(status);

        pick = button(content, R.string.select_image, () -> selectScreenshot());
        run = button(content, R.string.start_benchmark, () -> startBenchmark(REAL_TRIALS));
        original = button(content, R.string.preview_source, () -> preview.setImageBitmap(source));
        compressed = button(content, R.string.preview_avif, () -> preview.setImageBitmap(reopened));
        good = button(content, R.string.visual_ok, () -> setVisualAssessment("ACCEPTABLE_TO_REVIEWER"));
        bad = button(content, R.string.visual_bad, () -> setVisualAssessment("VISIBLE_ARTIFACTS_REPORTED"));
        save = button(content, R.string.export_report, () -> saveReport());

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        content.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(440)));
        refreshButtons();

        // Disposable emulator check only. Never silently process a user's image.
        synthetic = getIntent().getBooleanExtra("synthetic_ci", false)
                && (Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone"));
        if (synthetic) {
            source = syntheticFixture();
            refreshButtons();
            startBenchmark(1);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button button(LinearLayout parent, int resource, Runnable action) {
        Button button = new Button(this);
        button.setText(resource);
        button.setOnClickListener(v -> action.run());
        parent.addView(button);
        return button;
    }

    private void refreshButtons() {
        pick.setEnabled(!busy && !synthetic);
        run.setEnabled(!busy && source != null && !synthetic);
        boolean resultReady = !busy && report != null;
        original.setEnabled(resultReady && source != null);
        compressed.setEnabled(resultReady && reopened != null);
        good.setEnabled(resultReady && reopened != null);
        bad.setEnabled(resultReady && reopened != null);
        save.setEnabled(resultReady);
    }

    private void selectScreenshot() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("image/png");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(picker, OPEN_IMAGE);
    }

    private void saveReport() {
        if (report == null) return;
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.setType("application/json");
        create.putExtra(Intent.EXTRA_TITLE, "fixmyshot-avif-physical-report.json");
        startActivityForResult(create, SAVE_REPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == OPEN_IMAGE && !busy) {
            busy = true;
            refreshButtons();
            status.setText(R.string.loading);
            worker.execute(() -> {
                try {
                    Bitmap loaded = decodeSelectedPng(uri);
                    runOnUiThread(() -> {
                        if (source != null) source.recycle();
                        if (reopened != null) reopened.recycle();
                        source = loaded;
                        reopened = null;
                        report = null;
                        clearPrivateOutput();
                        preview.setImageBitmap(source);
                        status.setText(R.string.ready);
                        busy = false;
                        refreshButtons();
                    });
                } catch (Exception ex) {
                    showError(ex);
                }
            });
        } else if (requestCode == SAVE_REPORT && report != null) {
            try (java.io.OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
                // ContentResolver owns the user-approved document. Never export image bytes.
                if (output == null) throw new IllegalStateException("No writable document");
                output.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
                output.flush();
                status.setText(R.string.saved);
            } catch (Exception e) {
                showError(e);
            }
        }
    }

    private Bitmap decodeSelectedPng(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("Input is inaccessible");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth != WIDTH || bounds.outHeight != HEIGHT) {
            throw new IllegalArgumentException("Only a 1344x2992 PNG screenshot is accepted");
        }
        if (bounds.outColorSpace != null && !bounds.outColorSpace.isSrgb()) {
            throw new IllegalArgumentException("The source must be opaque 8-bit sRGB, not HDR/P3");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
        options.inScaled = false;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("Input is inaccessible");
            Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
            if (decoded == null || decoded.getConfig() != Bitmap.Config.ARGB_8888
                    || decoded.getWidth() != WIDTH || decoded.getHeight() != HEIGHT
                    || decoded.getColorSpace() == null || !decoded.getColorSpace().isSrgb()) {
                throw new IllegalArgumentException("Unsupported pixel format");
            }
            return decoded;
        }
    }

    private void startBenchmark(int trials) {
        if (busy || source == null) return;
        busy = true;
        report = null;
        // Each run retains at most one app-private preview; old candidates are deleted.
        preview.setImageBitmap(source);
        if (reopened != null) reopened.recycle();
        reopened = null;
        clearPrivateOutput();
        refreshButtons();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        status.setText(R.string.running);
        worker.execute(() -> {
            try {
                JSONObject result = new JSONObject();
                result.put("kind", "ISOLATED_PHYSICAL_AVIF_R1__NOT_PRODUCTION");
                result.put("device_model", Build.MODEL);
                result.put("android_api", Build.VERSION.SDK_INT);
                result.put("page_size_bytes", Os.sysconf(OsConstants._SC_PAGESIZE));
                result.put("package", getPackageName());
                result.put("input_kind", synthetic ? "synthetic_ci" : "consented_png_screenshot");
                result.put("input_size", "1344x2992");
                result.put("codec", "pinned_libavif_1.4.2_libaom_3.15.2");
                result.put("original_image_in_report", false);
                result.put("pss_sampling", "Debug.getPss sampled concurrently; maximum observed, NOT true peak");
                result.put("pss_interval_target_ms", SAMPLE_INTERVAL_MS);
                result.put("manual_visual", "NOT_REVIEWED");
                JSONArray trialsJson = new JSONArray();
                String nonce = synthetic ? "ci" : Long.toString(SystemClock.elapsedRealtimeNanos());
                for (int i = 0; i < trials; i++) {
                    final int iteration = i + 1;
                    runOnUiThread(() -> status.setText(getString(R.string.running)
                            + " " + iteration + "/" + trials));
                    File path = new File(getCacheDir(), "physical_" + nonce + "_" + i + ".avif");
                    if (path.exists()) throw new IllegalStateException("Refusing existing output");
                    long before = Debug.getPss();
                    SampleMeter meter = new SampleMeter();
                    meter.start();
                    long start = SystemClock.elapsedRealtimeNanos();
                    long[] values;
                    long totalNs;
                    JSONObject sample;
                    try {
                        values = encodeBitmap(source, path.getAbsolutePath());
                    } finally {
                        totalNs = SystemClock.elapsedRealtimeNanos() - start;
                        // Stop the sampler even if JNI throws, so no background timer leaks.
                        sample = meter.finish();
                    }
                    long after = Debug.getPss();
                    if (values == null || values.length != 5 || values[0] != 0
                            || values[1] <= 20 || values[1] != path.length()) {
                        path.delete();
                        throw new IllegalStateException("Native AVIF failure; status="
                                + (values != null && values.length > 0 ? values[0] : -1));
                    }
                    JSONObject measure = new JSONObject();
                    measure.put("iteration", iteration);
                    measure.put("total_wall_ns", totalNs);
                    measure.put("convert_ns", values[2]);
                    measure.put("encode_ns", values[3]);
                    measure.put("output_bytes", values[1]);
                    measure.put("process_peak_rss_kib", values[4]);
                    measure.put("pss_before_kib", before);
                    measure.put("pss_after_kib", after);
                    measure.put("concurrent_memory", sample);
                    trialsJson.put(measure);
                    if (i == 0) {
                        encoded = path;
                    } else if (!path.delete()) {
                        throw new IllegalStateException("Cannot remove private trial output");
                    }
                }
                result.put("trials", trialsJson);
                Bitmap deviceDecode = BitmapFactory.decodeFile(encoded.getAbsolutePath());
                if (deviceDecode == null) {
                    result.put("device_decode_status", "UNAVAILABLE_OR_FAILED");
                } else {
                    if (deviceDecode.getWidth() != WIDTH || deviceDecode.getHeight() != HEIGHT) {
                        result.put("device_decode_status", "WRONG_GEOMETRY");
                        deviceDecode.recycle();
                    } else {
                        result.put("device_decode_status", "OK");
                        result.put("quality", comparePixels(source, deviceDecode));
                        reopened = deviceDecode;
                    }
                }
                report = result;
                if (synthetic) {
                    File privateReport = new File(getFilesDir(), "physical_ci_report.json");
                    if (privateReport.exists()) throw new IllegalStateException("CI report already exists");
                    try (FileOutputStream output = new FileOutputStream(privateReport)) {
                        output.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
                        output.getFD().sync();
                    }
                }
                runOnUiThread(() -> {
                    status.setText(getString(R.string.finished)
                            + "\n" + summarize(result));
                    preview.setImageBitmap(reopened != null ? reopened : source);
                    busy = false;
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    refreshButtons();
                });
            } catch (Exception e) {
                showError(e);
            }
        });
    }

    private static String summarize(JSONObject json) {
        JSONArray rows = json.optJSONArray("trials");
        JSONObject quality = json.optJSONObject("quality");
        if (rows == null) return "No measurements";
        StringBuilder text = new StringBuilder("Runs: ").append(rows.length());
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            text.append("\n#").append(i + 1)
                .append(" encode ").append(row.optLong("encode_ns") / 1e6).append(" ms")
                .append(", sampled PSS ")
                .append(row.optJSONObject("concurrent_memory").optLong("max_pss_kib"))
                .append(" KiB");
        }
        if (quality != null) {
            text.append("\nPSNR: ").append(quality.optDouble("psnr_db"))
                .append(" dB; max channel error: ")
                .append(quality.optInt("max_channel_error"));
        }
        text.append("\nThese are sampled maxima, not absolute peak PSS or production p95.");
        return text.toString();
    }

    private static JSONObject comparePixels(Bitmap before, Bitmap after) throws Exception {
        int[] left = new int[WIDTH], right = new int[WIDTH];
        double sumSquares = 0;
        int maxError = 0;
        long alphaFailures = 0;
        for (int y = 0; y < HEIGHT; y++) {
            before.getPixels(left, 0, WIDTH, 0, y, WIDTH, 1);
            after.getPixels(right, 0, WIDTH, 0, y, WIDTH, 1);
            for (int x = 0; x < WIDTH; x++) {
                int a = left[x], b = right[x];
                if (Color.alpha(a) != 255 || Color.alpha(b) != 255) alphaFailures++;
                int[] diffs = {
                    Math.abs(Color.red(a) - Color.red(b)),
                    Math.abs(Color.green(a) - Color.green(b)),
                    Math.abs(Color.blue(a) - Color.blue(b))
                };
                for (int error : diffs) {
                    maxError = Math.max(maxError, error);
                    sumSquares += (double) error * error;
                }
            }
        }
        JSONObject quality = new JSONObject();
        double mse = sumSquares / (WIDTH * (double) HEIGHT * 3.0);
        quality.put("mse_rgb", mse);
        quality.put("psnr_db", mse == 0 ? JSONObject.NULL : 10.0 * Math.log10(65025.0 / mse));
        quality.put("exact_rgb", mse == 0);
        quality.put("max_channel_error", maxError);
        quality.put("nonopaque_pixels", alphaFailures);
        quality.put("comparison_scope", "Android display decode, 8-bit sRGB; visual review separate");
        return quality;
    }

    private void setVisualAssessment(String assessment) {
        if (report == null) return;
        try {
            report.put("manual_visual", assessment);
            status.setText(getString(R.string.visual_recorded));
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        runOnUiThread(() -> {
            clearPrivateOutput();
            status.setText(getString(R.string.failed) + ": " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            busy = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            refreshButtons();
        });
    }

    private void clearPrivateOutput() {
        if (encoded != null && encoded.exists()) encoded.delete();
        encoded = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdown();
        // Only this probe's own cache file; never touch any imported document.
        if (!busy) clearPrivateOutput();
    }

    private static Bitmap syntheticFixture() {
        Bitmap bm = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        int[][] colors = {
            {64,64,64}, {128,128,128}, {192,192,192},
            {192,64,64}, {64,192,64}, {64,64,192}
        };
        int[] row = new int[WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int r, g, b;
                if (y < HEIGHT / 2) {
                    int[] color = colors[x * 6 / WIDTH];
                    r = color[0]; g = color[1]; b = color[2];
                } else if (y < HEIGHT * 3 / 4) {
                    r = g = b = (y % 16 == 0 || x % 32 == 0) ? 255 : 18;
                } else if (x % 32 <= 1 && y % 16 <= 10) {
                    r = 20; g = 100; b = 220;
                } else {
                    r = g = b = 255;
                }
                row[x] = Color.rgb(r, g, b);
            }
            bm.setPixels(row, 0, WIDTH, 0, y, WIDTH, 1);
        }
        return bm;
    }

    private static final class SampleMeter {
        private final ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor();
        private final AtomicBoolean active = new AtomicBoolean(false);
        private final AtomicLong maxPss = new AtomicLong(0);
        private final AtomicLong maxNative = new AtomicLong(0);
        private final AtomicInteger count = new AtomicInteger();
        private final AtomicInteger failures = new AtomicInteger();

        void start() {
            active.set(true);
            clock.scheduleWithFixedDelay(() -> {
                if (!active.get()) return;
                try {
                    long pssKiB = Debug.getPss();
                    long nativeBytes = Debug.getNativeHeapAllocatedSize();
                    if (!active.get()) return;
                    if (pssKiB > 0) {
                        maxPss.accumulateAndGet(pssKiB, Math::max);
                        maxNative.accumulateAndGet(nativeBytes, Math::max);
                        count.incrementAndGet();
                    } else {
                        failures.incrementAndGet();
                    }
                } catch (RuntimeException ex) {
                    failures.incrementAndGet();
                }
            }, 0, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        JSONObject finish() throws Exception {
            active.set(false);
            clock.shutdownNow();
            clock.awaitTermination(2, TimeUnit.SECONDS);
            JSONObject result = new JSONObject();
            result.put("valid_samples", count.get());
            result.put("sample_failures", failures.get());
            result.put("max_pss_kib", count.get() == 0 ? JSONObject.NULL : maxPss.get());
            result.put("max_native_heap_allocated_bytes", count.get() == 0
                    ? JSONObject.NULL : maxNative.get());
            result.put("sample_method", "android.os.Debug.getPss");
            result.put("sampled_during_native_call", count.get() > 0);
            return result;
        }
    }
}
