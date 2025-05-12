// NativeLib.h (hardcoded for now)
#ifndef _Included_io_github_fletchmckee_ktjni_samples_simple_NativeLib
#define _Included_io_github_fletchmckee_ktjni_samples_simple_NativeLib

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL Java_io_github_fletchmckee_ktjni_samples_simple_NativeLib_stringFromJNI
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
