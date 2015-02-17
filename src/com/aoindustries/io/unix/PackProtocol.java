/*
 * aocode-public - Reusable Java library of general tools with minimal external dependencies.
 * Copyright (C) 2009, 2010, 2011  AO Industries, Inc.
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
package com.aoindustries.io.unix;

/**
 * The internal protocol values used between ParallelPack and ParallelUnpack.
 * 
 * @see  ParallelPack
 * @see  ParallelUnpack
 *
 * @author  AO Industries, Inc.
 */
class PackProtocol {

    /**
     * Make no instances.
     */
    private PackProtocol() {}

    /**
     * The header (magic value).
     */
    static final String HEADER="ParallelPack";

    /**
     * The version supported.
     * 
     * 1 - Original version
     * 2 - Added single byte response from unpack when connected over TCP to
     *     avoid EOFException on socket close
     * 3 - Added compression option
     */
    static final int VERSION=3;

    /**
     * These values are used on the main loop.
     */
    static final byte
        REGULAR_FILE = 0,
        DIRECTORY = 1,
        SYMLINK = 2,
        BLOCK_DEVICE = 3,
        CHARACTER_DEVICE = 4,
        FIFO = 5,
        END = 6
    ;
    
    /**
     * The buffer size.
     */
    static final short BUFFER_SIZE = 4096;
    
    static final int DEFAULT_PORT = 10000;
}
