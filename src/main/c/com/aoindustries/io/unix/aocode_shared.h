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
 * along with ao-io-posix.  If not, see <http://www.gnu.org/licenses/>.
 */
#include <jni.h>
#ifndef _Included_aocode_shared
#define _Included_aocode_shared
#ifdef __cplusplus
extern "C" {
#endif
static const char* FILE_NOT_FOUND_EXCEPTION="java/io/FileNotFoundException";
static const char* IO_EXCEPTION="java/io/IOException";
static const char* ILLEGAL_ARGUMENT_EXCEPTION="java/lang/IllegalArgumentException";
static const char* INTERRUPTED_IO_EXCEPTION="java/io/InterruptedIOException";
static const char* NO_SUCH_METHOD_EXCEPTION="java/lang/NoSuchMethodException";
static const char* OUT_OF_MEMORY_EXCEPTION="java/lang/OutOfMemoryError";
static const char* RUNTIME_EXCEPTION="java/lang/RuntimeException";
static const char* SECURITY_EXCEPTION="java/lang/SecurityException";

extern const char* getErrorType(const int err);
#ifdef __cplusplus
}
#endif
#endif
