/*
 * The following functions are derived from the JDK 1.5 source code file jni_util.h
 */
#ifndef JNI_UTIL_H
#define JNI_UTIL_H
#include <jni.h>
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg);
JNIEXPORT void JNICALL JNU_ThrowOutOfMemoryError(JNIEnv *env, const char *msg);
extern const char* getString8859_1Chars(JNIEnv *env, jstring jstr);
extern jstring newString8859_1(JNIEnv *env, const char *str);
extern void releaseString8859_1Chars(const char* str);

#ifdef __cplusplus
}
#endif
#endif
