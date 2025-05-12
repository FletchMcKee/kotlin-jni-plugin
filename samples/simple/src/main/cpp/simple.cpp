#include "NativeLib.h"

extern "C" JNIEXPORT jstring

JNICALL
Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */)
{
    return env->NewStringUTF("Hello from hardcoded JNI header");
}
