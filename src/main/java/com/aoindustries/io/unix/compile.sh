#!/bin/sh
#
# ao-io-unix - Java interface to native Unix filesystem objects.
# Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2015  AO Industries, Inc.
#     support@aoindustries.com
#     7262 Bull Pen Cir
#     Mobile, AL 36695
#
# This file is part of ao-io-unix.
#
# ao-io-unix is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# ao-io-unix is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with ao-io-unix.  If not, see <http://www.gnu.org/licenses/>.
#
gcc -D_FILE_OFFSET_BITS=64 \
	-fPIC \
	-O2 \
	-shared -lcrypt \
	-I/opt/jdk1.6.0_05/include \
	-I/opt/jdk1.6.0_05/include/linux \
	-o libaocode.so \
	aocode_shared.c \
	jni_util.c \
	com_aoindustries_io_unix_UnixFile.c \
	linux/com_aoindustries_io_unix_linux_DevRandom.c || exit "$?"
strip libaocode.so || exit "$?"
