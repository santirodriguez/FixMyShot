#include <jni.h>
#include <android/bitmap.h>
#include <webp/encode.h>
#include <algorithm>
#include <chrono>
#include <cstdio>
#include <memory>
#include <new>
#include <unistd.h>
#include <vector>

namespace {
struct Context {
    JNIEnv* env;
    jobject cancellation;
    jmethodID get;
    FILE* output;
    std::chrono::steady_clock::time_point deadline;
    int failure = 0;
    bool stopped() {
        if (failure != 0) return true;
        if (env->CallBooleanMethod(cancellation, get) || env->ExceptionCheck()) failure = 2;
        else if (std::chrono::steady_clock::now() >= deadline) failure = 3;
        return failure != 0;
    }
};
int writeBytes(const uint8_t* bytes, size_t count, const WebPPicture* picture) {
    auto* context = static_cast<Context*>(picture->custom_ptr);
    if (context->stopped()) return 0;
    if (std::fwrite(bytes, 1, count, context->output) != count) { context->failure = 4; return 0; }
    return 1;
}
int progress(int, const WebPPicture* picture) {
    return !static_cast<Context*>(picture->custom_ptr)->stopped();
}
struct BitmapLock {
    JNIEnv* env; jobject bitmap; void* pixels = nullptr;
    BitmapLock(JNIEnv* e, jobject b) : env(e), bitmap(b) {
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) pixels = nullptr;
    }
    ~BitmapLock() { if (pixels != nullptr) AndroidBitmap_unlockPixels(env, bitmap); }
};
struct Picture {
    WebPPicture value{};
    bool initialized = WebPPictureInit(&value) != 0;
    ~Picture() { if (initialized) WebPPictureFree(&value); }
};
}

extern "C" JNIEXPORT jint JNICALL
Java_org_cajapersonal_fixmyshot_codec_NativeWebp_encode(
    JNIEnv* env, jobject, jobject bitmap, jint fd, jobject cancellation, jlong timeoutMillis, jboolean premultiplied) {
    // Codes: 0 success, 1 encoder/input, 2 cancelled, 3 timeout, 4 I/O, 5 memory.
    try {
        AndroidBitmapInfo info{};
        if (fd < 0 || timeoutMillis <= 0 || timeoutMillis > 120000 ||
            AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
            info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info.width == 0 || info.height == 0 ||
            info.width > WEBP_MAX_DIMENSION || info.height > WEBP_MAX_DIMENSION ||
            static_cast<uint64_t>(info.width) * info.height > 24000000ULL) return 1;
        jclass type = env->GetObjectClass(cancellation);
        if (type == nullptr) return 1;
        const jmethodID get = env->GetMethodID(type, "get", "()Z");
        env->DeleteLocalRef(type);
        if (get == nullptr) return 1;
        const int duplicate = dup(fd);
        if (duplicate < 0) return 4;
        FILE* raw = fdopen(duplicate, "wb");
        if (raw == nullptr) { close(duplicate); return 4; }
        std::unique_ptr<FILE, decltype(&std::fclose)> file(raw, &std::fclose);
        Context context{env, cancellation, get, raw,
            std::chrono::steady_clock::now() + std::chrono::milliseconds(timeoutMillis)};
        if (context.stopped()) return context.failure;
        Picture picture;
        WebPConfig config;
        if (!picture.initialized || !WebPConfigInit(&config)) return 1;
        config.lossless = 1;
        config.quality = 75;
        config.method = 4;
        config.exact = 0; // Hidden RGB under alpha zero is outside the normalized pixel contract.
        config.thread_level = 0;
        if (!WebPValidateConfig(&config)) return 1;
        picture.value.use_argb = 1;
        picture.value.width = static_cast<int>(info.width);
        picture.value.height = static_cast<int>(info.height);
        picture.value.writer = writeBytes;
        picture.value.progress_hook = progress;
        picture.value.custom_ptr = &context;
        {
            std::vector<uint8_t> rgba(static_cast<size_t>(info.width) * info.height * 4);
            BitmapLock lock(env, bitmap);
            if (lock.pixels == nullptr) return 1;
            for (uint32_t y = 0; y < info.height; ++y) {
                if (context.stopped()) return context.failure;
                const auto* source = static_cast<const uint8_t*>(lock.pixels) + static_cast<size_t>(y) * info.stride;
                auto* target = rgba.data() + static_cast<size_t>(y) * info.width * 4;
                for (uint32_t x = 0; x < info.width; ++x) {
                    const unsigned alpha = source[x * 4 + 3];
                    for (unsigned channel = 0; channel < 3; ++channel) {
                        const unsigned value = source[x * 4 + channel];
                        target[x * 4 + channel] = static_cast<uint8_t>(
                            alpha == 0 ? 0 : (premultiplied ? std::min(255U, (value * 255U + alpha / 2U) / alpha) : value));
                    }
                    target[x * 4 + 3] = static_cast<uint8_t>(alpha);
                }
            }
            if (!WebPPictureImportRGBA(&picture.value, rgba.data(), static_cast<int>(info.width * 4))) return 5;
        }
        const bool encoded = WebPEncode(&config, &picture.value) != 0;
        if (context.stopped()) return context.failure;
        if (!encoded) return 1;
        return std::fflush(raw) == 0 ? 0 : 4;
    } catch (const std::bad_alloc&) { return 5; }
}
