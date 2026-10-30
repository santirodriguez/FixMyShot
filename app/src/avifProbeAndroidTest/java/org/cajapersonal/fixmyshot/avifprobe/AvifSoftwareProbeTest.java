package org.cajapersonal.fixmyshot.avifprobe;

// Only compiled through the opt-in diagnostic AndroidTest source directory.
import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class AvifSoftwareProbeTest {
    static {
        System.loadLibrary("avif_probe");
    }

    private static native long[] encodeFixture(String outputPath, int width, int height);

    private static long pssKiB(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assertNotNull(manager);
        android.os.Debug.MemoryInfo[] samples =
                manager.getProcessMemoryInfo(new int[] {Process.myPid()});
        assertEquals(1, samples.length);
        return samples[0].getTotalPss();
    }

    private static JSONArray measure(Context context, int width, int height, int trials)
            throws Exception {
        JSONArray records = new JSONArray();
        File retained = new File(context.getFilesDir(),
                "avif_probe_" + width + "x" + height + ".avif");
        assertFalse("Existing private diagnostic output must not be overwritten",
                retained.exists());
        for (int i = 0; i < trials; i++) {
            File output = new File(context.getFilesDir(),
                    "avif_probe_" + width + "x" + height + "_" + i + ".avif");
            assertFalse(output.exists());
            long before = pssKiB(context);
            long start = SystemClock.elapsedRealtimeNanos();
            long[] nativeResult = encodeFixture(output.getAbsolutePath(), width, height);
            long elapsed = SystemClock.elapsedRealtimeNanos() - start;
            long after = pssKiB(context);
            assertNotNull(nativeResult);
            assertEquals(4, nativeResult.length);
            assertEquals("JNI encoder did not succeed", 0L, nativeResult[0]);
            assertTrue(nativeResult[1] > 20);
            assertEquals(nativeResult[1], output.length());
            JSONObject record = new JSONObject();
            record.put("total_elapsed_ns", elapsed);
            record.put("encode_ns", nativeResult[2]);
            record.put("rss_peak_kib", nativeResult[3]);
            record.put("pss_before_kib", before);
            record.put("pss_after_kib", after);
            record.put("encoded_bytes", nativeResult[1]);
            records.put(record);
            if (i == 0) {
                assertTrue(output.renameTo(retained));
            } else {
                assertTrue(output.delete());
            }
        }
        return records;
    }

    @Test
    public void encodeOnlySyntheticAvifWithIsolatedJni() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("org.cajapersonal.fixmyshot.avifprobe", context.getPackageName());
        JSONObject report = new JSONObject();
        report.put("kind", "ISOLATED_AVIF_JNI__NOT_PRODUCTION");
        report.put("package", context.getPackageName());
        report.put("small_65x67", measure(context, 65, 67, 20));
        report.put("large_1344x2992", measure(context, 1344, 2992, 5));
        File destination = new File(context.getFilesDir(), "avif_probe_metrics.json");
        assertFalse(destination.exists());
        try (FileOutputStream stream = new FileOutputStream(destination)) {
            stream.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
            stream.getFD().sync();
        }
        assertTrue(destination.length() > 100);
    }
}
