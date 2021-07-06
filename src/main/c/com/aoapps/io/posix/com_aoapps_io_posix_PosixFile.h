/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_aoapps_io_posix_PosixFile */

#ifndef _Included_com_aoapps_io_posix_PosixFile
#define _Included_com_aoapps_io_posix_PosixFile
#ifdef __cplusplus
extern "C" {
#endif
#undef com_aoapps_io_posix_PosixFile_ROOT_UID
#define com_aoapps_io_posix_PosixFile_ROOT_UID 0L
#undef com_aoapps_io_posix_PosixFile_ROOT_GID
#define com_aoapps_io_posix_PosixFile_ROOT_GID 0L
#undef com_aoapps_io_posix_PosixFile_PERMISSION_MASK
#define com_aoapps_io_posix_PosixFile_PERMISSION_MASK 4095LL
#undef com_aoapps_io_posix_PosixFile_OTHER_EXECUTE
#define com_aoapps_io_posix_PosixFile_OTHER_EXECUTE 1LL
#undef com_aoapps_io_posix_PosixFile_NOT_OTHER_EXECUTE
#define com_aoapps_io_posix_PosixFile_NOT_OTHER_EXECUTE -2LL
#undef com_aoapps_io_posix_PosixFile_OTHER_WRITE
#define com_aoapps_io_posix_PosixFile_OTHER_WRITE 2LL
#undef com_aoapps_io_posix_PosixFile_NOT_OTHER_WRITE
#define com_aoapps_io_posix_PosixFile_NOT_OTHER_WRITE -3LL
#undef com_aoapps_io_posix_PosixFile_OTHER_READ
#define com_aoapps_io_posix_PosixFile_OTHER_READ 4LL
#undef com_aoapps_io_posix_PosixFile_NOT_OTHER_READ
#define com_aoapps_io_posix_PosixFile_NOT_OTHER_READ -5LL
#undef com_aoapps_io_posix_PosixFile_GROUP_EXECUTE
#define com_aoapps_io_posix_PosixFile_GROUP_EXECUTE 8LL
#undef com_aoapps_io_posix_PosixFile_NOT_GROUP_EXECUTE
#define com_aoapps_io_posix_PosixFile_NOT_GROUP_EXECUTE -9LL
#undef com_aoapps_io_posix_PosixFile_GROUP_WRITE
#define com_aoapps_io_posix_PosixFile_GROUP_WRITE 16LL
#undef com_aoapps_io_posix_PosixFile_NOT_GROUP_WRITE
#define com_aoapps_io_posix_PosixFile_NOT_GROUP_WRITE -17LL
#undef com_aoapps_io_posix_PosixFile_GROUP_READ
#define com_aoapps_io_posix_PosixFile_GROUP_READ 32LL
#undef com_aoapps_io_posix_PosixFile_NOT_GROUP_READ
#define com_aoapps_io_posix_PosixFile_NOT_GROUP_READ -33LL
#undef com_aoapps_io_posix_PosixFile_USER_EXECUTE
#define com_aoapps_io_posix_PosixFile_USER_EXECUTE 64LL
#undef com_aoapps_io_posix_PosixFile_NOT_USER_EXECUTE
#define com_aoapps_io_posix_PosixFile_NOT_USER_EXECUTE -65LL
#undef com_aoapps_io_posix_PosixFile_USER_WRITE
#define com_aoapps_io_posix_PosixFile_USER_WRITE 128LL
#undef com_aoapps_io_posix_PosixFile_NOT_USER_WRITE
#define com_aoapps_io_posix_PosixFile_NOT_USER_WRITE -129LL
#undef com_aoapps_io_posix_PosixFile_USER_READ
#define com_aoapps_io_posix_PosixFile_USER_READ 256LL
#undef com_aoapps_io_posix_PosixFile_NOT_USER_READ
#define com_aoapps_io_posix_PosixFile_NOT_USER_READ -257LL
#undef com_aoapps_io_posix_PosixFile_SAVE_TEXT_IMAGE
#define com_aoapps_io_posix_PosixFile_SAVE_TEXT_IMAGE 512LL
#undef com_aoapps_io_posix_PosixFile_NOT_SAVE_TEXT_IMAGE
#define com_aoapps_io_posix_PosixFile_NOT_SAVE_TEXT_IMAGE -513LL
#undef com_aoapps_io_posix_PosixFile_SET_GID
#define com_aoapps_io_posix_PosixFile_SET_GID 1024LL
#undef com_aoapps_io_posix_PosixFile_NOT_SET_GID
#define com_aoapps_io_posix_PosixFile_NOT_SET_GID -1025LL
#undef com_aoapps_io_posix_PosixFile_SET_UID
#define com_aoapps_io_posix_PosixFile_SET_UID 2048LL
#undef com_aoapps_io_posix_PosixFile_NOT_SET_UID
#define com_aoapps_io_posix_PosixFile_NOT_SET_UID -2049LL
#undef com_aoapps_io_posix_PosixFile_TYPE_MASK
#define com_aoapps_io_posix_PosixFile_TYPE_MASK 61440LL
#undef com_aoapps_io_posix_PosixFile_IS_FIFO
#define com_aoapps_io_posix_PosixFile_IS_FIFO 4096LL
#undef com_aoapps_io_posix_PosixFile_IS_CHARACTER_DEVICE
#define com_aoapps_io_posix_PosixFile_IS_CHARACTER_DEVICE 8192LL
#undef com_aoapps_io_posix_PosixFile_IS_DIRECTORY
#define com_aoapps_io_posix_PosixFile_IS_DIRECTORY 16384LL
#undef com_aoapps_io_posix_PosixFile_IS_BLOCK_DEVICE
#define com_aoapps_io_posix_PosixFile_IS_BLOCK_DEVICE 24576LL
#undef com_aoapps_io_posix_PosixFile_IS_REGULAR_FILE
#define com_aoapps_io_posix_PosixFile_IS_REGULAR_FILE 32768LL
#undef com_aoapps_io_posix_PosixFile_IS_SYM_LINK
#define com_aoapps_io_posix_PosixFile_IS_SYM_LINK 40960LL
#undef com_aoapps_io_posix_PosixFile_IS_SOCKET
#define com_aoapps_io_posix_PosixFile_IS_SOCKET 49152LL
/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    chown0
 * Signature: (Ljava/lang/String;II)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_chown0
  (JNIEnv *, jclass, jstring, jint, jint);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    getStat0
 * Signature: (Ljava/lang/String;)Lcom/aoapps/io/posix/Stat;
 */
JNIEXPORT jobject JNICALL Java_com_aoapps_io_posix_PosixFile_getStat0
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    crypt0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoapps_io_posix_PosixFile_crypt0
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    mktemp0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoapps_io_posix_PosixFile_mktemp0
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    mknod0
 * Signature: (Ljava/lang/String;JJ)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_mknod0
  (JNIEnv *, jclass, jstring, jlong, jlong);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    mkfifo0
 * Signature: (Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_mkfifo0
  (JNIEnv *, jclass, jstring, jlong);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    setMode0
 * Signature: (Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_setMode0
  (JNIEnv *, jclass, jstring, jlong);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    symLink0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_symLink0
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    link0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_link0
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    readLink0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoapps_io_posix_PosixFile_readLink0
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    utime0
 * Signature: (Ljava/lang/String;JJ)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_utime0
  (JNIEnv *, jclass, jstring, jlong, jlong);

#ifdef __cplusplus
}
#endif
#endif