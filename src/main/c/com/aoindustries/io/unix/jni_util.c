/*
 * The following functions are copied from the JDK 1.5 source code file jni_util.c
 */
#include "jni_util.h"
#include <stdlib.h>
#include <string.h>

JNIEXPORT void JNICALL
JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg)
{
	jclass cls = (*env)->FindClass(env, name);

	if (cls != 0) /* Otherwise an exception has already been thrown */
		(*env)->ThrowNew(env, cls, msg);
}

JNIEXPORT void JNICALL
JNU_ThrowOutOfMemoryError(JNIEnv *env, const char *msg)
{
	JNU_ThrowByName(env, "java/lang/OutOfMemoryError", msg);
}

/* Optimized for char set ISO_8559_1 */
jstring
newString8859_1(JNIEnv *env, const char *str)
{
	int len = (int)strlen(str);
	jchar buf[512];
	jchar *str1;
	jstring result;
	int i;

	if (len > 512) {
		str1 = (jchar *)malloc(len * sizeof(jchar));
		if (str1 == 0) {
			JNU_ThrowOutOfMemoryError(env, 0);
			return 0;
		}
	} else
		str1 = buf;

	for (i=0;i<len;i++)
		str1[i] = (unsigned char)str[i];
	result = (*env)->NewString(env, str1, len);
	if (str1 != buf)
		free(str1);
	return result;
}

const char*
getString8859_1Chars(JNIEnv *env, jstring jstr)
{
	int i;
	char *result;
	jint len = (*env)->GetStringLength(env, jstr);
	const jchar *str = (*env)->GetStringCritical(env, jstr, 0);
	if (str == 0) {
		return 0;
	}

	result = (char *)malloc(len+1);
	if (result == 0) {
		(*env)->ReleaseStringCritical(env, jstr, str);
		JNU_ThrowOutOfMemoryError(env, 0);
		return 0;
	}

	for (i=0; i<len; i++) {
		jchar unicode = str[i];
		if (unicode <= 0x00ff)
			result[i] = unicode;
		else
			result[i] = '?';
	}

	result[len] = '\0';
	(*env)->ReleaseStringCritical(env, jstr, str);
	return result;
}

void releaseString8859_1Chars(const char* str) {
	free((void *)str);
}
