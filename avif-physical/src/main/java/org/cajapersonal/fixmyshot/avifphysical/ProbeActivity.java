package org.cajapersonal.fixmyshot.avifphysical;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
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
    // Constant positions in the built-in synthetic fixture; never capture user RGB.
    private static final int[][] SYNTHETIC_EDGE_POINTS = {
        {0, 2243},
        {0, 2244},
        {0, 2245},
        {0, 2250},
        {0, 2251},
        {1, 2250},
        {2, 2250},
        {31, 2250},
        {32, 2250},
        {33, 2250},
        {34, 2250},
        {16, 2250},
        {0, 2255},
        {0, 2256},
        {0, 2257},
        {0, 2266},
        {0, 2267},
        {100, 100},
        {100, 2000}
    };

    static { System.loadLibrary("avif_physical"); }
    private static native long[] encodeBitmap(Bitmap pixels, String privatePath);
    private static native long[] encodeSynthetic420(Bitmap pixels, String privatePath);
    private static native long[] decodeSynthetic444(Bitmap target, String privatePath);

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
            // A failed new selection must never leave an earlier screenshot runnable.
            preview.setImageBitmap(null);
            if (source != null) source.recycle();
            if (reopened != null) reopened.recycle();
            source = null;
            reopened = null;
            report = null;
            clearPrivateOutput();
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
                result.put("encoder_settings", "quality100/speed6/threads2/depth8/full-range/BT709/sRGB");
                result.put("chroma_format", "YUV444");
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
                        if (synthetic) {
                            // Keep the deterministic CI fixture out of evictable cache.
                            // Real user-selected screenshots retain their existing cache policy.
                            File retained = new File(getFilesDir(), "physical_ci_0.avif");
                            if (retained.exists() || !path.renameTo(retained)) {
                                throw new IllegalStateException("Cannot retain synthetic CI fixture");
                            }
                            encoded = retained;
                        } else {
                            encoded = path;
                        }
                    } else if (!path.delete()) {
                        throw new IllegalStateException("Cannot remove private trial output");
                    }
                }
                result.put("trials", trialsJson);
                // Compare three actual Android display paths on exactly the same bytes.
                // Keep the original default route for preview; diagnostics do not
                // silently select a more favorable decoder or lower its acceptance bar.
                JSONArray decodeResults = compareEncoded(encoded, true, true);
                if (decodeResults.getJSONObject(0).has("quality")) {
                    result.put("quality", decodeResults.getJSONObject(0).getJSONObject("quality"));
                }
                if (synthetic) result.put("chroma420", compareSynthetic420());
                result.put("decoder_paths", decodeResults);
                result.put("device_decode_status",
                        decodeResults.getJSONObject(0).getString("status"));
                result.put("comparison_scope", "three Android 8-bit sRGB display routes; "
                        + "synthetic-only direct444 comparator; default preview and Android gate unchanged");
                result.put("decode_memory_scope", "app process only; framework codec-service memory excluded; "
                        + "sequential single-run samples are not whole-device peaks or cold/warm percentiles");
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

    private JSONArray compareEncoded(File input, boolean includeDirect, boolean retainPreview)
            throws Exception {
        JSONArray decodeResults = new JSONArray();
        String[] androidModes = {
                "bitmap_factory_default",
                "bitmap_factory_explicit_srgb",
                "image_decoder_srgb_software"};
        String[] comparisonModes = synthetic && includeDirect ? new String[]{
                androidModes[0], androidModes[1], androidModes[2],
                "libavif_aom_direct_444"} : androidModes;
        for (String mode : comparisonModes) {
            JSONObject outcome = new JSONObject();
            outcome.put("mode", mode);
            Bitmap decoded = null;
            try {
                long decodeStart = SystemClock.elapsedRealtimeNanos();
                outcome.put("pss_before_kib", Debug.getPss());
                SampleMeter decodeMeter = new SampleMeter();
                decodeMeter.start();
                try {
                    if ("bitmap_factory_default".equals(mode)) {
                        decoded = BitmapFactory.decodeFile(input.getAbsolutePath());
                    } else if ("bitmap_factory_explicit_srgb".equals(mode)) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        options.inPreferredColorSpace =
                                ColorSpace.get(ColorSpace.Named.SRGB);
                        options.inScaled = false;
                        decoded = BitmapFactory.decodeFile(input.getAbsolutePath(), options);
                    } else if ("image_decoder_srgb_software".equals(mode)) {
                        decoded = ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(input),
                                (decoder, info, source) -> {
                                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                                    decoder.setTargetColorSpace(
                                            ColorSpace.get(ColorSpace.Named.SRGB));
                                });
                    } else {
                        if (!synthetic) throw new IllegalStateException("Synthetic probe only");
                        decoded = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888,
                                false, ColorSpace.get(ColorSpace.Named.SRGB));
                        long[] values = decodeSynthetic444(decoded, input.getAbsolutePath());
                        if (values == null || values.length != 8) {
                            throw new IllegalStateException("Invalid direct decoder result");
                        }
                        JSONObject direct = new JSONObject();
                        direct.put("status_code", values[0]);
                        direct.put("decoded_width", values[1]);
                        direct.put("decoded_height", values[2]);
                        direct.put("decoded_depth", values[3]);
                        direct.put("libavif_pixel_format", values[4]);
                        direct.put("parse_decode_ns", values[5]);
                        direct.put("yuv_to_rgb_ns", values[6]);
                        direct.put("process_cumulative_peak_rss_kib", values[7]);
                        direct.put("codec", "pinned_libaom_decoder_libavif_builtin_rgb");
                        outcome.put("direct_444", direct);
                        if (values[0] != 0 || values[1] != WIDTH || values[2] != HEIGHT
                                || values[3] != 8 || values[4] != 1) {
                            throw new IllegalStateException("Direct decoder rejected fixture");
                        }
                    }
                } finally {
                    outcome.put("decode_wall_ns", SystemClock.elapsedRealtimeNanos() - decodeStart);
                    outcome.put("concurrent_decode_memory", decodeMeter.finish());
                    outcome.put("pss_after_kib", Debug.getPss());
                }
                if (decoded == null) {
                    outcome.put("status", "UNAVAILABLE_OR_FAILED");
                } else {
                    outcome.put("width", decoded.getWidth());
                    outcome.put("height", decoded.getHeight());
                    outcome.put("bitmap_config", String.valueOf(decoded.getConfig()));
                    outcome.put("color_space", decoded.getColorSpace() == null
                            ? "UNKNOWN" : decoded.getColorSpace().getName());
                    if (decoded.getWidth() != WIDTH || decoded.getHeight() != HEIGHT) {
                        outcome.put("status", "WRONG_GEOMETRY");
                    } else if (decoded.getColorSpace() == null
                            || !decoded.getColorSpace().isSrgb()) {
                        outcome.put("status", "NOT_SRGB");
                    } else {
                        outcome.put("status", "OK");
                        JSONObject quality = comparePixels(source, decoded);
                        outcome.put("quality", quality);
                        if (synthetic) {
                            // The normal user-selected image path NEVER records pixels.
                            outcome.put("synthetic_rgb_points",
                                    sampleSyntheticEdgePoints(source, decoded));
                        }
                        if (retainPreview && "bitmap_factory_default".equals(mode)) {
                            reopened = decoded;
                            decoded = null; // Retain default-preview ownership.
                        }
                    }
                }
            } catch (Exception ex) {
                outcome.put("status", "DECODE_EXCEPTION");
                // No user input path, original pixels or image metadata in report.
                outcome.put("exception_type", ex.getClass().getSimpleName());
            } finally {
                if (decoded != null) decoded.recycle();
            }
            decodeResults.put(outcome);
        }
        return decodeResults;
    }

    private JSONObject compareSynthetic420() throws Exception {
        if (!synthetic) throw new IllegalStateException("Synthetic probe only");
        File path = new File(getCacheDir(), "physical_ci_420.avif");
        File retained = new File(getFilesDir(), "physical_ci_420.avif");
        if (path.exists() || retained.exists()) throw new IllegalStateException("Existing synthetic420 output");
        SampleMeter meter = new SampleMeter();
        long before = Debug.getPss();
        meter.start();
        long start = SystemClock.elapsedRealtimeNanos();
        long[] values;
        JSONObject sample;
        long wall;
        try {
            values = encodeSynthetic420(source, path.getAbsolutePath());
        } finally {
            wall = SystemClock.elapsedRealtimeNanos() - start;
            sample = meter.finish();
        }
        if (values == null || values.length != 5 || values[0] != 0
                || values[1] <= 20 || values[1] != path.length()) {
            path.delete();
            throw new IllegalStateException("Synthetic420 encode failure");
        }
        if (!path.renameTo(retained)) throw new IllegalStateException("Cannot retain synthetic420");
        JSONObject measure = new JSONObject();
        measure.put("iteration", 1);
        measure.put("total_wall_ns", wall);
        measure.put("convert_ns", values[2]);
        measure.put("encode_ns", values[3]);
        measure.put("output_bytes", values[1]);
        measure.put("process_peak_rss_kib", values[4]);
        measure.put("pss_before_kib", before);
        measure.put("pss_after_kib", Debug.getPss());
        measure.put("concurrent_memory", sample);
        JSONObject result = new JSONObject();
        result.put("kind", "ISOLATED_PHYSICAL_AVIF_R1__NOT_PRODUCTION");
        result.put("input_kind", "synthetic_ci");
        result.put("input_size", "1344x2992");
        result.put("original_image_in_report", false);
        result.put("chroma_format", "YUV420");
        result.put("quality_setting", 100);
        result.put("encoder_settings", "quality100/speed6/threads2/depth8/full-range/BT709/sRGB");
        result.put("trials", new JSONArray().put(measure));
        JSONArray paths = compareEncoded(retained, false, false);
        result.put("decoder_paths", paths);
        result.put("device_decode_status", paths.getJSONObject(0).getString("status"));
        if (paths.getJSONObject(0).has("quality")) {
            result.put("quality", paths.getJSONObject(0).getJSONObject("quality"));
        }
        return result;
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
        JSONArray decoderPaths = json.optJSONArray("decoder_paths");
        if (decoderPaths != null) {
            for (int i = 0; i < decoderPaths.length(); i++) {
                JSONObject path = decoderPaths.optJSONObject(i);
                if (path == null) continue;
                JSONObject q = path.optJSONObject("quality");
                text.append("\n").append(path.optString("mode"))
                        .append(": ").append(path.optString("status"));
                if (q != null) text.append("; max channel error ")
                        .append(q.optInt("max_channel_error"))
                        .append("; pixels >2 ").append(q.optLong("pixels_error_gt_2"));
            }
        }
        text.append("\nThese are sampled maxima, not absolute peak PSS or production p95.");
        return text.toString();
    }

    private static JSONArray sampleSyntheticEdgePoints(Bitmap expected, Bitmap decoded)
            throws Exception {
        JSONArray samples = new JSONArray();
        for (int[] position : SYNTHETIC_EDGE_POINTS) {
            int x = position[0];
            int y = position[1];
            int a = expected.getPixel(x, y);
            int b = decoded.getPixel(x, y);
            int dr = Color.red(b) - Color.red(a);
            int dg = Color.green(b) - Color.green(a);
            int db = Color.blue(b) - Color.blue(a);
            JSONObject sample = new JSONObject();
            sample.put("x", x);
            sample.put("y", y);
            sample.put("source_rgb", new JSONArray().put(Color.red(a))
                    .put(Color.green(a)).put(Color.blue(a)));
            sample.put("decoded_rgb", new JSONArray().put(Color.red(b))
                    .put(Color.green(b)).put(Color.blue(b)));
            sample.put("delta_rgb", new JSONArray().put(dr).put(dg).put(db));
            sample.put("max_abs_channel_error",
                    Math.max(Math.abs(dr), Math.max(Math.abs(dg), Math.abs(db))));
            samples.put(sample);
        }
        return samples;
    }

    private static JSONObject comparePixels(Bitmap before, Bitmap after) throws Exception {
        int[] left = new int[WIDTH], right = new int[WIDTH];
        double sumSquares = 0;
        int maxError = 0, worstX = -1, worstY = -1;
        long alphaFailures = 0, pixelsOver2 = 0, pixelsOver16 = 0;
        long[] pixelErrorBins = new long[7]; // 0,1,2,3..7,8..15,16..31,32+
        long[] channelOver2 = new long[3]; // RGB
        long[][] errorGrid = new long[4][6]; // Four vertical by six horizontal zones.
        for (int y = 0; y < HEIGHT; y++) {
            before.getPixels(left, 0, WIDTH, 0, y, WIDTH, 1);
            after.getPixels(right, 0, WIDTH, 0, y, WIDTH, 1);
            for (int x = 0; x < WIDTH; x++) {
                int a = left[x], b = right[x];
                if (Color.alpha(a) != 255 || Color.alpha(b) != 255) alphaFailures++;
                int redError = Math.abs(Color.red(a) - Color.red(b));
                int greenError = Math.abs(Color.green(a) - Color.green(b));
                int blueError = Math.abs(Color.blue(a) - Color.blue(b));
                if (redError > 2) channelOver2[0]++;
                if (greenError > 2) channelOver2[1]++;
                if (blueError > 2) channelOver2[2]++;
                int error = Math.max(redError, Math.max(greenError, blueError));
                if (error > maxError) {
                    maxError = error;
                    worstX = x;
                    worstY = y;
                }
                if (error > 2) {
                    pixelsOver2++;
                    errorGrid[y * 4 / HEIGHT][x * 6 / WIDTH]++;
                }
                if (error > 16) pixelsOver16++;
                pixelErrorBins[error == 0 ? 0 : error == 1 ? 1 : error == 2 ? 2
                        : error <= 7 ? 3 : error <= 15 ? 4 : error <= 31 ? 5 : 6]++;
                sumSquares += (double) redError * redError
                        + (double) greenError * greenError
                        + (double) blueError * blueError;
            }
        }
        JSONObject quality = new JSONObject();
        double mse = sumSquares / (WIDTH * (double) HEIGHT * 3.0);
        quality.put("mse_rgb", mse);
        quality.put("psnr_db", mse == 0 ? JSONObject.NULL : 10.0 * Math.log10(65025.0 / mse));
        quality.put("exact_rgb", mse == 0);
        quality.put("max_channel_error", maxError);
        quality.put("nonopaque_pixels", alphaFailures);
        quality.put("pixels_error_gt_2", pixelsOver2);
        quality.put("pixels_error_gt_16", pixelsOver16);
        quality.put("worst_error_x", worstX);
        quality.put("worst_error_y", worstY);
        JSONArray binCounts = new JSONArray();
        for (long count : pixelErrorBins) binCounts.put(count);
        quality.put("max_channel_error_histogram_0_1_2_3to7_8to15_16to31_32plus", binCounts);
        JSONArray channels = new JSONArray();
        for (long count : channelOver2) channels.put(count);
        quality.put("channel_error_gt_2_rgb", channels);
        JSONArray grid = new JSONArray();
        for (long[] row : errorGrid) {
            JSONArray cells = new JSONArray();
            for (long count : row) cells.put(count);
            grid.put(cells);
        }
        quality.put("pixel_error_gt_2_grid_4x6", grid);
        quality.put("comparison_scope", "Android display decoder sRGB 8-bit; visual review separate");
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
        // Disposable-emulator evidence lives until run-as extraction / AVD disposal.
        if (!busy && !synthetic) clearPrivateOutput();
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
