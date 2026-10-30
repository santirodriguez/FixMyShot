// SPDX-License-Identifier: GPL-3.0-only
// User-initiated, app-private AVIF physical probe; never used by normal exports.
#include <jni.h>
#include <android/bitmap.h>
#include <avif/avif.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>
#include <time.h>
#include <unistd.h>

#define WIDTH 1344
#define HEIGHT 2992
#define MAX_AVIF_BYTES (16U * 1024U * 1024U)

static uint64_t now_ns(void) {
    struct timespec ts;
    if (clock_gettime(CLOCK_MONOTONIC, &ts) != 0) return 0;
    return (uint64_t)ts.tv_sec * 1000000000ULL + (uint64_t)ts.tv_nsec;
}

static jlongArray result(JNIEnv *env, jlong code, jlong bytes,
                         jlong convert_ns, jlong encode_ns, jlong rss_kib) {
    jlong payload[5] = { code, bytes, convert_ns, encode_ns, rss_kib };
    jlongArray arr = (*env)->NewLongArray(env, 5);
    if (arr) (*env)->SetLongArrayRegion(env, arr, 0, 5, payload);
    return arr;
}

static int write_new_private_file(const char *path, const uint8_t *bytes, size_t length) {
    int fd = open(path, O_CREAT | O_EXCL | O_WRONLY | O_CLOEXEC, 0600);
    if (fd < 0) return 10;
    int error = 0;
    size_t position = 0;
    while (position < length) {
        ssize_t n = write(fd, bytes + position, length - position);
        if (n <= 0) { error = 11; break; }
        position += (size_t)n;
    }
    if (!error && fsync(fd) != 0) error = 12;
    if (close(fd) != 0) error = 12;
    if (error) unlink(path);
    return error;
}

JNIEXPORT jlongArray JNICALL
Java_org_cajapersonal_fixmyshot_avifphysical_ProbeActivity_encodeBitmap(
        JNIEnv *env, jclass owner, jobject bitmap, jstring destination) {
    (void)owner;
    if (!bitmap || !destination) return result(env, 1, 0, 0, 0, -1);
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
        info.width != WIDTH || info.height != HEIGHT ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        info.stride < WIDTH * 4 || info.stride > WIDTH * 8) {
        return result(env, 2, 0, 0, 0, -1);
    }
    const char *path = (*env)->GetStringUTFChars(env, destination, NULL);
    if (!path) return NULL;
    // Refuse unknown paths and traversal. Java constructs this from getCacheDir().
    if (strlen(path) > 640 || !strstr(path, "/cache/physical_") ||
        strstr(path, "..") || strrchr(path, '/') == NULL) {
        (*env)->ReleaseStringUTFChars(env, destination, path);
        return result(env, 3, 0, 0, 0, -1);
    }

    int code = 0;
    uint64_t convert_ns = 0, encode_ns = 0;
    size_t count = 0;
    avifImage *image = avifImageCreate(WIDTH, HEIGHT, 8, AVIF_PIXEL_FORMAT_YUV444);
    if (!image) code = 4;
    if (!code) {
        image->yuvRange = AVIF_RANGE_FULL;
        image->colorPrimaries = AVIF_COLOR_PRIMARIES_BT709;
        image->transferCharacteristics = AVIF_TRANSFER_CHARACTERISTICS_SRGB;
        image->matrixCoefficients = AVIF_MATRIX_COEFFICIENTS_BT709;
        void *pixels = NULL;
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS ||
            !pixels) {
            code = 5;
        } else {
            avifRGBImage rgb;
            avifRGBImageSetDefaults(&rgb, image);
            rgb.format = AVIF_RGB_FORMAT_RGBA;
            rgb.depth = 8;
            rgb.rowBytes = info.stride;
            rgb.pixels = (uint8_t *)pixels;
            // Opaque screenshots only: premultiplied/translucent input is not qualified.
            for (uint32_t y = 0; y < info.height && !code; y++) {
                const uint8_t *row = (const uint8_t *)pixels + (size_t)y * info.stride;
                for (uint32_t x = 0; x < info.width; x++) {
                    if (row[(size_t)x * 4 + 3] != 255) { code = 6; break; }
                }
            }
            if (!code) {
                uint64_t start = now_ns();
                if (avifImageRGBToYUV(image, &rgb) != AVIF_RESULT_OK) code = 7;
                convert_ns = now_ns() - start;
            }
            if (AndroidBitmap_unlockPixels(env, bitmap) != ANDROID_BITMAP_RESULT_SUCCESS) {
                code = 8;
            }
        }
    }
    if (!code) {
        avifEncoder *encoder = avifEncoderCreate();
        if (!encoder) {
            code = 9;
        } else {
            encoder->quality = 100;
            encoder->speed = 6;
            encoder->maxThreads = 2;
            avifRWData buffer = AVIF_DATA_EMPTY;
            uint64_t start = now_ns();
            avifResult rc = avifEncoderWrite(encoder, image, &buffer);
            encode_ns = now_ns() - start;
            if (rc != AVIF_RESULT_OK || buffer.size < 20 || buffer.size > MAX_AVIF_BYTES) {
                code = 13;
            } else {
                code = write_new_private_file(path, buffer.data, buffer.size);
                if (!code) count = buffer.size;
            }
            avifRWDataFree(&buffer);
            avifEncoderDestroy(encoder);
        }
    }
    if (image) avifImageDestroy(image);
    (*env)->ReleaseStringUTFChars(env, destination, path);
    struct rusage usage;
    long rss = getrusage(RUSAGE_SELF, &usage) == 0 ? usage.ru_maxrss : -1;
    return result(env, code, (jlong)count, (jlong)convert_ns,
                  (jlong)encode_ns, (jlong)rss);
}
