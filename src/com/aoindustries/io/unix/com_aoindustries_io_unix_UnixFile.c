/*
 * ao-io-unix - Java interface to native Unix filesystem objects.
 * Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2015  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-io-unix.
 *
 * ao-io-unix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-io-unix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-io-unix.  If not, see <http://www.gnu.org/licenses/>.
 */
#include <crypt.h>
#include <jni.h>
#include "aocode_shared.h"
#include "jni_util.h"
#include "com_aoindustries_io_unix_UnixFile.h"
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <utime.h>

extern int errno;

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    chown0
 * Signature: (Ljava/lang/String;II)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_chown0(JNIEnv* env, jclass cls, jstring jfilename, jint uid, jint gid) {
    jclass newExcCls=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        if(lchown(filename, uid, gid)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    getStat0
 * Signature: (Ljava/lang/String;Lcom/aoindustries/io/unix/Stat;)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_getStat0(JNIEnv* env, jobject jthis, jstring jfilename, jobject jstat) {
    jclass newExcCls=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        // Perform the stat into the stat buffer
        struct stat* buff=(struct stat*)malloc(sizeof(struct stat));
        if(buff!=NULL) {
            jboolean exists;
            jlong device;
            jlong inode;
            jlong mode;
            jint numberLinks;
            jint uid;
            jint gid;
            jlong deviceIdentifier;
            jlong size;
            jint blockSize;
            jlong blockCount;
            jlong accessTime;
            jlong modifyTime;
            jlong changeTime;
            if(lstat(filename, buff)==0) {
                exists           = JNI_TRUE;
                device           = (jlong)buff->st_dev;
                inode            = (jlong)buff->st_ino;
                mode             = (jlong)buff->st_mode;
                numberLinks      = (jint)buff->st_nlink;
                uid              = (jint)buff->st_uid;
                gid              = (jint)buff->st_gid;
                deviceIdentifier = (jlong)buff->st_rdev;
                size             = (jlong)buff->st_size;
                blockSize        = (jint)buff->st_blksize;
                blockCount       = (jlong)buff->st_blocks;
                accessTime       = ((jlong)(buff->st_atime))*1000;
                modifyTime       = ((jlong)(buff->st_mtime))*1000;
                changeTime       = ((jlong)(buff->st_ctime))*1000;
            } else if(errno==ENOENT || errno==ENOTDIR) {
                exists           = JNI_FALSE;
                device           = 0;
                inode            = 0;
                mode             = 0;
                numberLinks      = 0;
                uid              = 0;
                gid              = 0;
                deviceIdentifier = 0;
                size             = 0;
                blockSize        = 0;
                blockCount       = 0;
                accessTime       = 0;
                modifyTime       = 0;
                changeTime       = 0;
            } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
            free(buff);
            if(newExcCls==NULL) {
                jclass statClass=(*env)->FindClass(env, "com/aoindustries/io/unix/Stat");
                if(statClass!=NULL) {
                    jmethodID statInitMethod = (*env)->GetMethodID(env, statClass, "init", "(Lcom/aoindustries/io/unix/UnixFile;ZJJJIIIJJIJJJJ)V");
                    if(statInitMethod!=NULL) {
                        (*env)->CallVoidMethod(
                            env,
                            jstat,
                            statInitMethod,
                            jthis,
                            exists,
                            device,
                            inode,
                            mode,
                            numberLinks,
                            uid,
                            gid,
                            deviceIdentifier,
                            size,
                            blockSize,
                            blockCount,
                            accessTime,
                            modifyTime,
                            changeTime
                        );
                    }
                }
            }
        } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    crypt0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoindustries_io_unix_UnixFile_crypt0(JNIEnv* env, jclass cls, jstring jpassword, jstring jsalt) {
    jclass newExcCls=NULL;
    jstring jcrypted=NULL;
    const char* password=(*env)->GetStringUTFChars(env, jpassword, NULL);
    if(password!=NULL) {
        const char* salt=(*env)->GetStringUTFChars(env, jsalt, NULL);
        if(salt!=NULL) {
            char* crypted=(char*)crypt(password, salt);
            if(crypted!=NULL) jcrypted=(*env)->NewStringUTF(env, crypted);
            else newExcCls=(*env)->FindClass(env, getErrorType(errno));
            (*env)->ReleaseStringUTFChars(env, jsalt, salt);
        }
        (*env)->ReleaseStringUTFChars(env, jpassword, password);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return jcrypted;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    mktemp0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoindustries_io_unix_UnixFile_mktemp0(JNIEnv* env, jclass cls , jstring jtemplate) {
    jclass newExcCls=NULL;
    jstring jfilename=NULL;
    const char* template=getString8859_1Chars(env, jtemplate);
    if(template!=NULL) {
        int len=strlen(template)+1;
        char* filename=(char*)malloc(len);
        if(filename!=NULL) {
            memcpy(filename, template, len);
            {
                int fd=mkstemp(filename);
                if(fd!=-1) {
                    if(close(fd)==0) {
                        jfilename=newString8859_1(env, filename);
                    } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
                } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
            }
            free(filename);
        } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
        releaseString8859_1Chars(template);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return jfilename;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    mknod0
 * Signature: (Ljava/lang/String;JJ)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_mknod0(JNIEnv* env, jclass cls, jstring jfilename, jlong mode, jlong device) {
    jclass newExcCls=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        if(mknod(filename, mode, device)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    mkfifo0
 * Signature: (Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_mkfifo0(JNIEnv* env, jclass cls, jstring jfilename, jlong mode) {
    jclass newExcCls=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        if(mknod(filename, S_IFIFO|mode, 0)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    setMode0
 * Signature: (Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_setMode0(JNIEnv* env, jclass cls, jstring jfilename, jlong mode) {
    jclass newExcCls=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        if(chmod(filename, mode)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    symLink0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_symLink0(JNIEnv* env, jclass cls, jstring jfilename, jstring jdestination) {
    jclass newExcCls=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        const char* destination=getString8859_1Chars(env, jdestination);
        if(destination!=NULL) {
            if(symlink(destination, filename)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
            releaseString8859_1Chars(destination);
        }
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    link0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_link0(JNIEnv* env, jclass cls, jstring jfilename, jstring jdestination) {
    jclass newExcCls=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        const char* destination=getString8859_1Chars(env, jdestination);
        if(destination!=NULL) {
            if(link(destination, filename)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
            releaseString8859_1Chars(destination);
        }
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    readLink0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoindustries_io_unix_UnixFile_readLink0(JNIEnv* env, jclass cls, jstring jfilename) {
    jclass newExcCls=NULL;
    jstring jdestination=NULL;
    const char* filename=getString8859_1Chars(env, jfilename);
    if(filename!=NULL) {
        char* destination=(char*)malloc(4097);
        if(destination!=NULL) {
            int charCount=readlink(filename, destination, 4096);
            if(charCount!=-1) {
                destination[charCount]='\0';
                jdestination=newString8859_1(env, destination);
            } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
            free(destination);
        } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
        releaseString8859_1Chars(filename);
    }
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return jdestination;
}

/*
 * Class:     com_aoindustries_io_unix_UnixFile
 * Method:    utime0
 * Signature: (Ljava/lang/String;JJ)V
 */
JNIEXPORT void JNICALL Java_com_aoindustries_io_unix_UnixFile_utime0(JNIEnv* env, jclass cls, jstring jfilename, jlong atime, jlong mtime) {
    jclass newExcCls = NULL;
    struct utimbuf* times=(struct utimbuf*)malloc(sizeof(struct utimbuf));
    if(times!=NULL) {
        const char* filename=getString8859_1Chars(env, jfilename);
        if(filename!=NULL) {
            times->actime=atime/1000;
            times->modtime=mtime/1000;
            if(utime(filename, times)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
            releaseString8859_1Chars(filename);
        }
        free(times);
    } else newExcCls=(*env)->FindClass(env, getErrorType(errno));
    if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
    return;
}
