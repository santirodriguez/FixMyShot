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
#include <sys/stat.h>
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

static jlongArray encode_bitmap(
        JNIEnv *env, jobject bitmap, jstring destination, avifPixelFormat format) {
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

    if (format == AVIF_PIXEL_FORMAT_YUV420) {
        const char suffix[] = "/cache/physical_ci_420.avif";
        size_t length = strlen(path);
        if (length < sizeof(suffix) - 1 ||
            strcmp(path + length - (sizeof(suffix) - 1), suffix) != 0) {
            (*env)->ReleaseStringUTFChars(env, destination, path);
            return result(env, 3, 0, 0, 0, -1);
        }
    }
    int code = 0;
    uint64_t convert_ns = 0, encode_ns = 0;
    size_t count = 0;
    avifImage *image = avifImageCreate(WIDTH, HEIGHT, 8, format);
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

JNIEXPORT jlongArray JNICALL
Java_org_cajapersonal_fixmyshot_avifphysical_ProbeActivity_encodeBitmap(
        JNIEnv *env, jclass owner, jobject bitmap, jstring destination) {
    (void)owner;
    return encode_bitmap(env, bitmap, destination, AVIF_PIXEL_FORMAT_YUV444);
}

// Synthetic-only path; never wired to selected screenshots or normal export.
JNIEXPORT jlongArray JNICALL
Java_org_cajapersonal_fixmyshot_avifphysical_ProbeActivity_encodeSynthetic420(
        JNIEnv *env, jclass owner, jobject bitmap, jstring destination) {
    (void)owner;
    return encode_bitmap(env, bitmap, destination, AVIF_PIXEL_FORMAT_YUV420);
}

// Only the built-in emulator fixture is accepted. No selected user image path.
JNIEXPORT jlongArray JNICALL
Java_org_cajapersonal_fixmyshot_avifphysical_ProbeActivity_decodeSynthetic444(
        JNIEnv *env, jclass owner, jobject bitmap, jstring input) {
    (void)owner;
    jlong values[8] = {20, 0, 0, 0, 0, 0, 0, -1};
#ifdef FIXMYSHOT_AVIF_DIRECT_DECODE
    AndroidBitmapInfo info;
    if (!bitmap || !input ||
        AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
        info.width != WIDTH || info.height != HEIGHT ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        info.stride < WIDTH * 4 || info.stride > WIDTH * 8) {
        values[0] = 21;
    } else {
        const char *path = (*env)->GetStringUTFChars(env, input, NULL);
        if (!path) return NULL;
        const char suffix[] = "/files/physical_ci_0.avif";
        size_t length = strlen(path);
        struct stat st;
        if (length > 640 || length < sizeof(suffix) - 1 || strstr(path, "..") ||
            strcmp(path + length - (sizeof(suffix) - 1), suffix) != 0 ||
            stat(path, &st) != 0 || !S_ISREG(st.st_mode) ||
            st.st_size < 20 || st.st_size > MAX_AVIF_BYTES) {
            values[0] = 22;
        } else {
            avifDecoder *decoder = avifDecoderCreate();
            values[0] = 23;
            if (decoder) {
                decoder->codecChoice = AVIF_CODEC_CHOICE_AOM;
                decoder->maxThreads = 2;
                decoder->imageSizeLimit = WIDTH * HEIGHT;
                decoder->imageDimensionLimit = HEIGHT;
                decoder->imageCountLimit = 1;
                decoder->allowProgressive = AVIF_FALSE;
                decoder->ignoreExif = AVIF_TRUE;
                decoder->ignoreXMP = AVIF_TRUE;
                uint64_t start = now_ns();
                avifResult rc = avifDecoderSetIOFile(decoder, path);
                if (rc == AVIF_RESULT_OK) rc = avifDecoderParse(decoder);
                if (rc == AVIF_RESULT_OK &&
                    (decoder->imageCount != 1 || decoder->image->width != WIDTH ||
                     decoder->image->height != HEIGHT || decoder->image->depth != 8)) {
                    rc = AVIF_RESULT_INVALID_ARGUMENT;
                }
                if (rc == AVIF_RESULT_OK) rc = avifDecoderNextImage(decoder);
                values[5] = (jlong)(now_ns() - start);
                const avifImage *image = decoder->image;
                values[0] = 24;
                if (rc == AVIF_RESULT_OK && image->width == WIDTH && image->height == HEIGHT &&
                    image->depth == 8 && image->yuvFormat == AVIF_PIXEL_FORMAT_YUV444 &&
                    image->yuvRange == AVIF_RANGE_FULL && !image->alphaPlane &&
                    image->colorPrimaries == AVIF_COLOR_PRIMARIES_BT709 &&
                    image->transferCharacteristics == AVIF_TRANSFER_CHARACTERISTICS_SRGB &&
                    image->matrixCoefficients == AVIF_MATRIX_COEFFICIENTS_BT709 &&
                    image->transformFlags == 0) {
                    values[1] = image->width;
                    values[2] = image->height;
                    values[3] = image->depth;
                    values[4] = image->yuvFormat;
                    void *pixels = NULL;
                    values[0] = 25;
                    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESULT_SUCCESS && pixels) {
                        avifRGBImage rgb;
                        avifRGBImageSetDefaults(&rgb, image);
                        rgb.format = AVIF_RGB_FORMAT_RGBA;
                        rgb.depth = 8;
                        rgb.rowBytes = info.stride;
                        rgb.pixels = (uint8_t *)pixels;
                        rgb.avoidLibYUV = AVIF_TRUE;
                        rgb.maxThreads = 2;
                        start = now_ns();
                        rc = avifImageYUVToRGB(image, &rgb);
                        values[6] = (jlong)(now_ns() - start);
                        values[0] = rc == AVIF_RESULT_OK ? 0 : 26;
                        if (AndroidBitmap_unlockPixels(env, bitmap) != ANDROID_BITMAP_RESULT_SUCCESS) values[0] = 27;
                    }
                }
                avifDecoderDestroy(decoder);
            }
        }
        (*env)->ReleaseStringUTFChars(env, input, path);
    }
#else
    (void)bitmap;
    (void)input;
#endif
    struct rusage usage;
    if (getrusage(RUSAGE_SELF, &usage) == 0) values[7] = usage.ru_maxrss;
    jlongArray arr = (*env)->NewLongArray(env, 8);
    if (arr) (*env)->SetLongArrayRegion(env, arr, 0, 8, values);
    return arr;
}
