#include "io_github_fletchmckee_ktjni_samples_simple_NativeLib.h"

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_stringFromJni(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF("Hello from Jni");
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_longFromJni__(
        JNIEnv* env,
        jobject /* this */) {
    return static_cast<jlong>(42);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_longFromJni__J(
        JNIEnv* env,
        jobject /* this */,
        jlong value) {
    return value * 2;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Parent_00024Child_childJniMethod(
        JNIEnv*,
        jobject/* this */) {
    // Do nothing
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Parent_parentJniMethod(
        JNIEnv*,
        jobject/* this */) {
    // Do more nothing
}
