#include "io_github_fletchmckee_ktjni_samples_simple_NativeLib.h"
#include "io_github_fletchmckee_ktjni_samples_simple_Extension.h"
#include "io_github_fletchmckee_ktjni_samples_simple_Extension_SubExtension.h"
#include "io_github_fletchmckee_ktjni_samples_simple_Parent.h"
#include "io_github_fletchmckee_ktjni_samples_simple_Parent_Child.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_stringFromJni(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF("Hello from Jni");
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_longFromJni__(
        JNIEnv *env,
        jobject /* this */) {
    return static_cast<jlong>(42);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_longFromJni__J(
        JNIEnv *env,
        jobject /* this */,
        jlong value) {
    return value * 2;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Parent_00024Child_childJniMethod(
        JNIEnv *,
        jobject/* this */) {
    // Do nothing
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Parent_parentJniMethod(
        JNIEnv *,
        jobject/* this */) {
    // Do more nothing
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Extension_nativeExtension__
        (JNIEnv *, jobject) {
    return static_cast<jint>(5);
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Extension_nativeExtension__Ljava_lang_String_2
        (JNIEnv *, jclass, jstring) {
    return static_cast<jint>(5);
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Extension_nativeExtensionV2
        (JNIEnv *, jclass, jstring) {
    return static_cast<jint>(99);
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Extension_00024SubExtension_anotherTest
        (JNIEnv *, jobject, jint) {
    return static_cast<jint>(8);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_Extension_nativeExtension___3F_3F
        (JNIEnv *env, jobject /* this */, jfloatArray input1Array, jfloatArray input2Array) {

    jsize input1Length = env->GetArrayLength(input1Array);
    jsize input2Length = env->GetArrayLength(input2Array);

    if (input1Length == 0 || input2Length == 0) {
        return 0.0f;
    }

    // Get C-style access to Java arrays
    jfloat *input1 = env->GetFloatArrayElements(input1Array, nullptr);
    jfloat *input2 = env->GetFloatArrayElements(input2Array, nullptr);

    jsize length = (input1Length < input2Length) ? input1Length : input2Length;

    jfloat result = 0.0f;
    for (jsize i = 0; i < length; i++) {
        result += input1[i] * input2[i];
    }

    env->ReleaseFloatArrayElements(input1Array, input1, JNI_ABORT);
    env->ReleaseFloatArrayElements(input2Array, input2, JNI_ABORT);

    return result;
}

extern "C" JNIEXPORT void JNICALL Java_io_github_fletchmckee_ktjni_samples_simple_MixedJavaExample_nativeJavaInJava
        (JNIEnv *, jclass) {
    // Do more nothing.
}
