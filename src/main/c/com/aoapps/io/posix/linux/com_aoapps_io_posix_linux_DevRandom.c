/*
 * ao-io-posix - Java interface to native POSIX filesystem objects.
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2015, 2016, 2021  AO Industries, Inc.
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
 * along with ao-io-posix.  If not, see <http://www.gnu.org/licenses/>.
 */
#include <stdlib.h>
#include <jni.h>
#include "../aocode_shared.h"
#include "com_aoapps_io_posix_linux_DevRandom.h"
#include <errno.h>
#include <fcntl.h>
#include <linux/types.h>
#include <linux/random.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

extern int errno;

/*
 * Class:     com_aoapps_io_posix_linux_DevRandom
 * Method:    addEntropy0
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_com_aoapps_io_posix_linux_DevRandom_addEntropy0(JNIEnv* env, jclass cls, jbyteArray randomData) {
	jclass newExcCls=NULL;

	// First, build up the rand_pool_info structure
	jsize len = (*env)->GetArrayLength(env, randomData);
	struct rand_pool_info* rand_info=(struct rand_pool_info*)malloc(sizeof(struct rand_pool_info) + sizeof(char)*len);
	if(rand_info!=NULL) {
		rand_info->entropy_count=len<<3;
		rand_info->buf_size=len;
		jbyte* body=(*env)->GetByteArrayElements(env, randomData, 0);
		if(body!=NULL) {
			int c;
			for(c=0;c<len;c++) {
				((char*)rand_info->buf)[c]=body[c];
			}
			(*env)->ReleaseByteArrayElements(env, randomData, body, 0);

			// Second, add this random data to the kernel
			int fdout=open("/dev/random", O_WRONLY);
			if(fdout>0) {
				if(ioctl(fdout, RNDADDENTROPY, rand_info)!=0) newExcCls=(*env)->FindClass(env, getErrorType(errno));
				close(fdout);
			} else newExcCls=(*env)->FindClass(env, getErrorType(errno));
		} else newExcCls=(*env)->FindClass(env, getErrorType(errno));
		free(rand_info);
	} else newExcCls=(*env)->FindClass(env, getErrorType(errno));

	// Throw any exceptions
	if(newExcCls!=NULL) (*env)->ThrowNew(env, newExcCls, strerror(errno));
	return;
}
