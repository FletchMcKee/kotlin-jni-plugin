#include <jni.h>
#include <cmath>
#include <android/bitmap.h>
#include <algorithm>
#include <vector>

static void boxBlur(uint32_t* pixels, int width, int height, int radius) {
    std::vector<uint32_t> temp(width * height);

    // Horizontal pass
    for (int y = 0; y < height; y++) {
        uint32_t r = 0, g = 0, b = 0, a = 0;
        int count = 0;

        // Initialize window
        for (int x = 0; x <= radius && x < width; x++) {
            uint32_t pixel = pixels[y * width + x];
            a += (pixel >> 24) & 0xFF;
            r += (pixel >> 16) & 0xFF;
            g += (pixel >> 8) & 0xFF;
            b += pixel & 0xFF;
            count++;
        }

        // Sliding window
        for (int x = 0; x < width; x++) {
            if (x + radius + 1 < width) {
                uint32_t pixel = pixels[y * width + x + radius + 1];
                a += (pixel >> 24) & 0xFF;
                r += (pixel >> 16) & 0xFF;
                g += (pixel >> 8) & 0xFF;
                b += pixel & 0xFF;
                count++;
            }

            if (x - radius > 0) {
                uint32_t pixel = pixels[y * width + x - radius - 1];
                a -= (pixel >> 24) & 0xFF;
                r -= (pixel >> 16) & 0xFF;
                g -= (pixel >> 8) & 0xFF;
                b -= pixel & 0xFF;
                count--;
            }

            temp[y * width + x] = ((a / count) << 24) |
                                  ((r / count) << 16) |
                                  ((g / count) << 8) |
                                  (b / count);
        }
    }

    // Vertical pass
    for (int x = 0; x < width; x++) {
        uint32_t r = 0, g = 0, b = 0, a = 0;
        int count = 0;

        // Initialize window
        for (int y = 0; y <= radius && y < height; y++) {
            uint32_t pixel = temp[y * width + x];
            a += (pixel >> 24) & 0xFF;
            r += (pixel >> 16) & 0xFF;
            g += (pixel >> 8) & 0xFF;
            b += pixel & 0xFF;
            count++;
        }

        // Sliding window
        for (int y = 0; y < height; y++) {
            if (y + radius + 1 < height) {
                uint32_t pixel = temp[(y + radius + 1) * width + x];
                a += (pixel >> 24) & 0xFF;
                r += (pixel >> 16) & 0xFF;
                g += (pixel >> 8) & 0xFF;
                b += pixel & 0xFF;
                count++;
            }

            if (y - radius > 0) {
                uint32_t pixel = temp[(y - radius - 1) * width + x];
                a -= (pixel >> 24) & 0xFF;
                r -= (pixel >> 16) & 0xFF;
                g -= (pixel >> 8) & 0xFF;
                b -= pixel & 0xFF;
                count--;
            }

            pixels[y * width + x] = ((a / count) << 24) |
                                    ((r / count) << 16) |
                                    ((g / count) << 8) |
                                    (b / count);
        }
    }
}

extern "C" JNIEXPORT jobject JNICALL Java_io_github_fletchmckee_ktjni_samples_simple_GaussianBlurKt_gaussianBlur
        (JNIEnv *env, __attribute__((unused)) jclass clazz, jobject bitmap, jfloat sigma) {
    AndroidBitmapInfo info;
    void* pixels;

    // Get bitmap info
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        return nullptr;
    }

    // Only support ARGB_8888
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }

    // Lock pixels for reading
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return nullptr;
    }

    // Create result bitmap
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                          "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888",
                                                   "Landroid/graphics/Bitmap$Config;");
    jobject config = env->GetStaticObjectField(configClass, argb8888Field);

    jobject resultBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod,
                                                       (jint)info.width, (jint)info.height, config);

    void* resultPixels;
    if (AndroidBitmap_lockPixels(env, resultBitmap, &resultPixels) < 0) {
        AndroidBitmap_unlockPixels(env, bitmap);
        return nullptr;
    }

    // Copy original to result
    memcpy(resultPixels, pixels, info.width * info.height * 4);

    // Convert sigma to box blur radius using Gaussian approximation formula
    float idealBoxWidth = std::sqrt((12.0f * sigma * sigma / 3.0f) + 1.0f);
    int radius = std::max(1, static_cast<int>((idealBoxWidth - 1.0f) / 2.0f));
    radius = std::min(100, radius);

    // Apply blur (3 passes for Gaussian approximation)
    for (int pass = 0; pass < 3; pass++) {
        boxBlur((uint32_t*)resultPixels, static_cast<int>(info.width), static_cast<int>(info.height), radius);
    }

    // Unlock pixels
    AndroidBitmap_unlockPixels(env, bitmap);
    AndroidBitmap_unlockPixels(env, resultBitmap);

    return resultBitmap;
}
