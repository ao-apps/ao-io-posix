/*
 * aocode-public - Reusable Java library of general tools with minimal external dependencies.
 * Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aocode-public.
 *
 * aocode-public is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aocode-public is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aocode-public.  If not, see <http://www.gnu.org/licenses/>.
 */
#include <jni.h>
#include <errno.h>
//include <stdlib.h>
#include <string.h>
#include "aocode_shared.h"

extern int errno;

// Gets the proper exception type for the provided errno
const char* getErrorType(const int err) {
    const char* errString;
    if(err==EACCES) errString=SECURITY_EXCEPTION;
    else if(err==EBADF) errString=IO_EXCEPTION;
    else if(err==EEXIST) errString=IO_EXCEPTION;
    else if(err==EFAULT) errString=RUNTIME_EXCEPTION;
    else if(err==EINTR) errString=INTERRUPTED_IO_EXCEPTION;
    else if(err==EINVAL) errString=ILLEGAL_ARGUMENT_EXCEPTION;
    else if(err==EIO) errString=IO_EXCEPTION;
    else if(err==ELOOP) errString=FILE_NOT_FOUND_EXCEPTION;
    else if(err==EMLINK) errString=IO_EXCEPTION;
    else if(err==ENAMETOOLONG) errString=ILLEGAL_ARGUMENT_EXCEPTION;
    else if(err==ENOENT) errString=FILE_NOT_FOUND_EXCEPTION;
    else if(err==ENOMEM) errString=OUT_OF_MEMORY_EXCEPTION;
    else if(err==ENOSPC) errString=IO_EXCEPTION;
    else if(err==ENOSYS) errString=NO_SUCH_METHOD_EXCEPTION;
    else if(err==ENOTDIR) errString=IO_EXCEPTION;
    else if(err==EPERM) errString=SECURITY_EXCEPTION;
    else if(err==EROFS) errString=IO_EXCEPTION;
    else if(err==EXDEV) errString=IO_EXCEPTION;
    else errString=RUNTIME_EXCEPTION;
    return errString;
}
