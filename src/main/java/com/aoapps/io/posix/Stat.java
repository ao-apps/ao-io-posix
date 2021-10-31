/*
 * ao-io-posix - Java interface to native POSIX filesystem objects.
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2015, 2016, 2021  AO Industries, Inc.
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
package com.aoapps.io.posix;

import java.io.FileNotFoundException;

/**
 * One stat call will have all of its output stored in an instance of this class.
 *
 * @see  PosixFile#getStat()
 *
 * @author  AO Industries, Inc.
 */
public class Stat {

	/**
	 * A stat that represents a non-existent file.
	 */
	public static final Stat NOT_EXISTS = new Stat(
		false,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0
	);

	private final boolean exists;
	private final long device;
	private final long inode;
	private final long mode;
	private final int numberLinks;
	private final int uid;
	private final int gid;
	private final long deviceIdentifier;
	private final long size;
	private final int blockSize;
	private final long blockCount;
	private final long accessTime;
	private final long modifyTime;
	private final long changeTime;

	public Stat(
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
	}

	/**
	 * Determines if a file exists, a symbolic link with an invalid destination
	 * is still considered to exist.
	 */
	public boolean exists() {
		return exists;
	}

	/**
	 * Gets the device for this file.
	 */
	public long getDevice() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return device;
	}

	/**
	 * Gets the inode for this file.
	 */
	public long getInode() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return inode;
	}

	/**
	 * Gets the complete mode of the file, including the bits representing the
	 * file type.
	 */
	public long getRawMode() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return mode;
	}

	/**
	 * Gets the permission bits of the mode of this file.
	 */
	public long getMode() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return mode & PosixFile.PERMISSION_MASK;
	}

	/**
	 * Gets a String representation of the mode of this file similar to the output of the POSIX <code>ls</code> command.
	 */
	public String getModeString() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.getModeString(mode);
	}

	/**
	 * Gets the link count for this file.
	 */
	public int getNumberLinks() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return numberLinks;
	}

	/**
	 * Gets the user ID of the file.
	 */
	public int getUid() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return uid;
	}

	/**
	 * Gets the group ID for this file.
	 */
	public int getGid() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return gid;
	}

	/**
	 * Gets the device identifier for this file.
	 */
	public long getDeviceIdentifier() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return deviceIdentifier;
	}

	/**
	 * Gets the size of the file.
	 */
	public long getSize() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return size;
	}

	/**
	 * Gets the block size for this file.
	 */
	public int getBlockSize() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return blockSize;
	}

	/**
	 * Gets the block count for this file.
	 */
	public long getBlockCount() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return blockCount;
	}

	/**
	 * Gets the last access to this file.
	 */
	public long getAccessTime() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return accessTime;
	}

	/**
	 * Gets the modification time of the file.
	 */
	public long getModifyTime() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return modifyTime;
	}

	/**
	 * Gets the change time of this file.
	 */
	public long getChangeTime() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return changeTime;
	}

	/**
	 * Determines if this file represents a block device.
	 */
	public boolean isBlockDevice() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.isBlockDevice(mode);
	}

	/**
	 * Determines if this file represents a character device.
	 */
	public boolean isCharacterDevice() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.isCharacterDevice(mode);
	}

	/**
	 * Determines if this file represents a directory.
	 */
	public boolean isDirectory() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.isDirectory(mode);
	}

	/**
	 * Determines if this file represents a FIFO.
	 */
	public boolean isFifo() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.isFifo(mode);
	}

	/**
	 * Determines if this file represents a regular file.
	 */
	public boolean isRegularFile() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.isRegularFile(mode);
	}

	/**
	 * Determines if this file represents a socket.
	 */
	public boolean isSocket() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.isSocket(mode);
	}

	/**
	 * Determines if this file represents a sybolic link.
	 */
	public boolean isSymLink() throws FileNotFoundException {
		if(!exists) throw new FileNotFoundException();
		return PosixFile.isSymLink(mode);
	}
}
