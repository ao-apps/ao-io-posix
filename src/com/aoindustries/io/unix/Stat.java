/*
 * ao-io-unix - Java interface to native Unix filesystem objects.
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2015  AO Industries, Inc.
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
package com.aoindustries.io.unix;

import java.io.FileNotFoundException;

/**
 * One stat call may have all of its output stored in an instance of this class.
 *
 * @see  UnixFile#getStat()
 * @see  UnixFile#getStat(Stat)
 *
 * @author  AO Industries, Inc.
 */
final public class Stat {

    private UnixFile unixFile;
    private boolean exists;
    private long device;
    private long inode;
    private long mode;
    private int numberLinks;
    private int uid;
    private int gid;
    private long deviceIdentifier;
    private long size;
    private int blockSize;
    private long blockCount;
    private long accessTime;
    private long modifyTime;
    private long changeTime;

    public Stat() {
        // No need to reset the new object, since state already reset by JVM: reset();
    }

    /**
     * Resets this Stat to the uninitialized state.  This is used to ensure access to the contents fails instead of return data from an old stat call.
     */
    public void reset() {
        this.unixFile = null;
        this.exists = false;
        this.device = 0;
        this.inode = 0;
        this.mode = 0;
        this.numberLinks = 0;
        this.uid = 0;
        this.gid = 0;
        this.deviceIdentifier = 0;
        this.size = 0;
        this.blockSize = 0;
        this.blockCount = 0;
        this.accessTime = 0;
        this.modifyTime = 0;
        this.changeTime = 0;
    }

    void init(
        UnixFile unixFile,
        boolean exists,
        long device,
        long inode,
        long mode,
        int numberLinks,
        int uid,
        int gid,
        long deviceIdentifier,
        long size,
        int blockSize,
        long blockCount,
        long accessTime,
        long modifyTime,
        long changeTime
    ) {
        this.exists = exists;
        this.device = device;
        this.inode = inode;
        this.mode = mode;
        this.numberLinks = numberLinks;
        this.uid = uid;
        this.gid = gid;
        this.deviceIdentifier = deviceIdentifier;
        this.size = size;
        this.blockSize = blockSize;
        this.blockCount = blockCount;
        this.accessTime = accessTime;
        this.modifyTime = modifyTime;
        this.changeTime = changeTime;
        this.unixFile = unixFile;
    }
    
    private void checkInitted() {
        if(unixFile==null) throw new AssertionError("unixFile is null: Stat object not yet used in a call to UnixFile.getStat(Stat)");
    }

    /**
     * Gets the <code>UnixFile</code> the current stat was obtained from or <code>null</code> if not yet
     * used.
     */
    public UnixFile getUnixFile() {
        return unixFile;
    }

    /**
     * Determines if a file exists, a symbolic link with an invalid destination
     * is still considered to exist.
     */
    public boolean exists() {
        checkInitted();
        return exists;
    }
    
    /**
     * Gets the device for this file.
     */
    public long getDevice() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return device;
    }

    /**
     * Gets the inode for this file.
     */
    public long getInode() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return inode;
    }

    /**
     * Gets the complete mode of the file, including the bits representing the
     * file type.
     */
    public long getRawMode() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return mode;
    }

    /**
     * Gets the permission bits of the mode of this file.
     */
    public long getMode() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return mode & UnixFile.PERMISSION_MASK;
    }

    /**
     * Gets a String representation of the mode of this file similar to the output of the Unix ls command.
     */
    public String getModeString() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.getModeString(mode);
    }

    /**
     * Gets the link count for this file.
     */
    public int getNumberLinks() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return numberLinks;
    }

    /**
     * Gets the user ID of the file.
     */
    public int getUid() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return uid;
    }

    /**
     * Gets the group ID for this file.
     */
    public int getGid() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return gid;
    }

    /**
     * Gets the device identifier for this file.
     */
    public long getDeviceIdentifier() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return deviceIdentifier;
    }

    /**
     * Gets the size of the file.
     */
    public long getSize() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return size;
    }

    /**
     * Gets the block size for this file.
     */
    public int getBlockSize() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return blockSize;
    }

    /**
     * Gets the block count for this file.
     */
    public long getBlockCount() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return blockCount;
    }

    /**
     * Gets the last access to this file.
     */
    public long getAccessTime() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return accessTime;
    }

    /**
     * Gets the modification time of the file.
     */
    public long getModifyTime() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return modifyTime;
    }

    /**
     * Gets the change time of this file.
     */
    public long getChangeTime() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return changeTime;
    }
    
    /**
     * Determines if this file represents a block device.
     */
    public boolean isBlockDevice() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.isBlockDevice(mode);
    }

    /**
     * Determines if this file represents a character device.
     */
    public boolean isCharacterDevice() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.isCharacterDevice(mode);
    }

    /**
     * Determines if this file represents a directory.
     */
    public boolean isDirectory() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.isDirectory(mode);
    }

    /**
     * Determines if this file represents a FIFO.
     */
    public boolean isFifo() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.isFifo(mode);
    }

    /**
     * Determines if this file represents a regular file.
     */
    public boolean isRegularFile() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.isRegularFile(mode);
    }

    /**
     * Determines if this file represents a socket.
     */
    public boolean isSocket() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.isSocket(mode);
    }

    /**
     * Determines if this file represents a sybolic link.
     */
    public boolean isSymLink() throws FileNotFoundException {
        checkInitted();
        if(!exists) throw new FileNotFoundException(unixFile.getPath());
        return UnixFile.isSymLink(mode);
    }
}
