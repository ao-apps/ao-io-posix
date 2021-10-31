/*
 * ao-io-posix - Java interface to native POSIX filesystem objects.
 * Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2015, 2016, 2021  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-io-posix.
 *
 * ao-io-posix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-io-posix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-io-posix.  If not, see <https://www.gnu.org/licenses/>.
 */
#include <crypt.h>
#include <jni.h>
#include "aocode_shared.h"
#include "jni_util.h"
#include "com_aoapps_io_posix_PosixFile.h"
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    chown0
 * Signature: (Ljava/lang/String;II)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_chown0(JNIEnv* env, jclass cls, jstring jfilename, jint uid, jint gid) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    getStat0
 * Signature: (Ljava/lang/String;)Lcom/aoapps/io/posix/Stat;
 */
JNIEXPORT jobject JNICALL Java_com_aoapps_io_posix_PosixFile_getStat0(JNIEnv* env, jobject jthis, jstring jfilename) {
	jclass newExcCls=NULL;
	jobject stat=NULL;
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
				// Unused: device           = 0;
				// Unused: inode            = 0;
				// Unused: mode             = 0;
				// Unused: numberLinks      = 0;
				// Unused: uid              = 0;
				// Unused: gid              = 0;
				// Unused: deviceIdentifier = 0;
				// Unused: size             = 0;
				// Unused: blockSize        = 0;
				// Unused: blockCount       = 0;
				// Unused: accessTime       = 0;
				// Unused: modifyTime       = 0;
				// Unused: changeTime       = 0;
			} else newExcCls=(*env)->FindClass(env, getErrorType(errno));
			free(buff);
			if(newExcCls==NULL) {
				jclass statClass=(*env)->FindClass(env, "com/aoapps/io/posix/Stat");
				if(statClass!=NULL) {
					if(exists == JNI_FALSE) {
						// not exists, return the static field
						jfieldID field = (*env)->GetStaticFieldID(env, statClass, "NOT_EXISTS", "Lcom/aoapps/io/posix/Stat;");
						if(field!=NULL) {
							stat = (*env)->GetStaticObjectField(env, statClass, field);
						}
					} else {
						// exists, return a new object
						jmethodID constructor = (*env)->GetMethodID(env, statClass, "<init>", "(ZJJJIIIJJIJJJJ)V");
						if(constructor!=NULL) {
							stat = (*env)->NewObject(
								env,
								statClass,
								constructor,
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
			}
		} else newExcCls=(*env)->FindClass(env, getErrorType(errno));
		releaseString8859_1Chars(filename);
	}
	if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
	return stat;
}

/*
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    crypt0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoapps_io_posix_PosixFile_crypt0(JNIEnv* env, jclass cls, jstring jpassword, jstring jsalt) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    mktemp0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoapps_io_posix_PosixFile_mktemp0(JNIEnv* env, jclass cls , jstring jtemplate) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    mknod0
 * Signature: (Ljava/lang/String;JJ)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_mknod0(JNIEnv* env, jclass cls, jstring jfilename, jlong mode, jlong device) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    mkfifo0
 * Signature: (Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_mkfifo0(JNIEnv* env, jclass cls, jstring jfilename, jlong mode) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    setMode0
 * Signature: (Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_setMode0(JNIEnv* env, jclass cls, jstring jfilename, jlong mode) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    symLink0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_symLink0(JNIEnv* env, jclass cls, jstring jfilename, jstring jdestination) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    link0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_link0(JNIEnv* env, jclass cls, jstring jfilename, jstring jdestination) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    readLink0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aoapps_io_posix_PosixFile_readLink0(JNIEnv* env, jclass cls, jstring jfilename) {
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
 * Class:     com_aoapps_io_posix_PosixFile
 * Method:    utime0
 * Signature: (Ljava/lang/String;JJ)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_PosixFile_utime0(JNIEnv* env, jclass cls, jstring jfilename, jlong atime, jlong mtime) {
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
