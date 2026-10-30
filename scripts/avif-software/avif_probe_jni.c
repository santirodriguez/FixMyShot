// SPDX-License-Identifier: GPL-3.0-only
// Only callable by the opt-in Android instrumentation fixture; never by app UI.
#include <jni.h>
#include <avif/avif.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>
#include <time.h>
#include <unistd.h>

#define SMALL_WIDTH 65
#define SMALL_HEIGHT 67
#define LARGE_WIDTH 1344
#define LARGE_HEIGHT 2992
#define MAX_ENCODED_BYTES (8U * 1024U * 1024U)

static const uint8_t COLORS[6][3] = {
    {64, 64, 64}, {128, 128, 128}, {192, 192, 192},
    {192, 64, 64}, {64, 192, 64}, {64, 64, 192}
};

static void fixture_pixel(int x, int y, int width, int height, uint8_t *out) {
    if (y < height / 2) {
        memcpy(out, COLORS[x * 6 / width], 3);
    } else if (y < height * 3 / 4) {
        uint8_t v = (y % 16 == 0 || x % 32 == 0) ? 255 : 18;
        out[0] = out[1] = out[2] = v;
    } else if (x % 32 <= 1 && y % 16 <= 10) {
        out[0] = 20; out[1] = 100; out[2] = 220;
    } else {
        out[0] = out[1] = out[2] = 255;
    }
}

static uint64_t monotonic_ns(void) {
    struct timespec ts;
    return clock_gettime(CLOCK_MONOTONIC, &ts) == 0 ?
        (uint64_t)ts.tv_sec * 1000000000ULL + (uint64_t)ts.tv_nsec : 0;
}

static int write_exclusive(const char *path, const uint8_t *data, size_t size) {
    int fd = open(path, O_CREAT | O_WRONLY | O_EXCL | O_CLOEXEC, 0600);
    if (fd < 0) return 10;
    int error = 0;
    size_t written = 0;
    while (written < size) {
        ssize_t n = write(fd, data + written, size - written);
        if (n <= 0) { error = 11; break; }
        written += (size_t)n;
    }
    if (close(fd) != 0) error = 11;
    if (error) unlink(path);
    return error;
}

static jlongArray reply(JNIEnv *env, jlong status, jlong bytes,
                        jlong encode_ns, jlong maxrss_kib) {
    const jlong data[4] = {status, bytes, encode_ns, maxrss_kib};
    jlongArray result = (*env)->NewLongArray(env, 4);
    if (result) (*env)->SetLongArrayRegion(env, result, 0, 4, data);
    return result;
}

JNIEXPORT jlongArray JNICALL
Java_org_cajapersonal_fixmyshot_avifprobe_AvifSoftwareProbeTest_encodeFixture(
        JNIEnv *env, jclass clazz, jstring output_path, jint width, jint height) {
    (void)clazz;
    if (!output_path ||
        !((width == SMALL_WIDTH && height == SMALL_HEIGHT) ||
          (width == LARGE_WIDTH && height == LARGE_HEIGHT))) {
        return reply(env, 2, 0, 0, -1);
    }
    const char *path = (*env)->GetStringUTFChars(env, output_path, NULL);
    if (!path) return NULL;
    if (!strstr(path, "/files/avif_probe_") || strlen(path) > 512) {
        (*env)->ReleaseStringUTFChars(env, output_path, path);
        return reply(env, 3, 0, 0, -1);
    }
    int status = 0;
    size_t encoded_bytes = 0;
    uint64_t elapsed = 0;
    const size_t row_bytes = (size_t)width * 3;
    avifImage *image = avifImageCreate(width, height, 8, AVIF_PIXEL_FORMAT_YUV444);
    if (!image) status = 4;
    if (!status) {
        image->yuvRange = AVIF_RANGE_FULL;
        image->colorPrimaries = AVIF_COLOR_PRIMARIES_BT709;
        image->transferCharacteristics = AVIF_TRANSFER_CHARACTERISTICS_SRGB;
        image->matrixCoefficients = AVIF_MATRIX_COEFFICIENTS_BT709;
        avifRGBImage rgb;
        avifRGBImageSetDefaults(&rgb, image);
        rgb.format = AVIF_RGB_FORMAT_RGB;
        rgb.depth = 8;
        rgb.rowBytes = (uint32_t)row_bytes;
        rgb.pixels = malloc(row_bytes * (size_t)height);
        if (!rgb.pixels) {
            status = 5;
        } else {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    fixture_pixel(x, y, width, height,
                        rgb.pixels + (size_t)y * row_bytes + (size_t)x * 3);
                }
            }
            if (avifImageRGBToYUV(image, &rgb) != AVIF_RESULT_OK) status = 6;
            free(rgb.pixels);
        }
    }
    if (!status) {
        avifEncoder *encoder = avifEncoderCreate();
        if (!encoder) {
            status = 7;
        } else {
            encoder->quality = 100;
            encoder->speed = 6;
            encoder->maxThreads = 2;
            avifRWData data = AVIF_DATA_EMPTY;
            uint64_t start = monotonic_ns();
            avifResult res = avifEncoderWrite(encoder, image, &data);
            elapsed = monotonic_ns() - start;
            if (res != AVIF_RESULT_OK || data.size < 20 ||
                data.size > MAX_ENCODED_BYTES) {
                status = 8;
            } else {
                status = write_exclusive(path, data.data, data.size);
                if (!status) encoded_bytes = data.size;
            }
            avifRWDataFree(&data);
            avifEncoderDestroy(encoder);
        }
    }
    if (image) avifImageDestroy(image);
    (*env)->ReleaseStringUTFChars(env, output_path, path);
    struct rusage usage;
    long rss = getrusage(RUSAGE_SELF, &usage) == 0 ? usage.ru_maxrss : -1;
    return reply(env, status, (jlong)encoded_bytes, (jlong)elapsed, (jlong)rss);
}
