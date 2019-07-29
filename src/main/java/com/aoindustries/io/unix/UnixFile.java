/*
 * ao-io-unix - Java interface to native Unix filesystem objects.
 * Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2015, 2016, 2017, 2018, 2019  AO Industries, Inc.
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

import com.aoindustries.io.FileUtils;
import com.aoindustries.io.IoUtils;
import com.aoindustries.util.BufferManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Access and modify all the Unix specific file attributes.  These updates are made using
 * a Linux shared library provided as a resource.  The source code is also supplied.
 * <p>
 * Note: The JVM must be in a single-byte locale, such as "C", "POSIX", or
 * "en_US".  UnixFile makes this assumption in its JNI implementation.
 * </p>
 * <p>
 * TODO: Rename to PosixFile, and rename project to ao-posix-file.
 * </p>
 *
 * @author  AO Industries, Inc.
 */
public class UnixFile {

	/**
	 * The UID of the root user.
	 * 
	 * Note: Copied to LinuxServerAccount.java to avoid interproject dependency.
	 */
	public static final int ROOT_UID = 0;

	/**
	 * The GID of the root user.
	 */
	public static final int ROOT_GID = 0;

	/**
	 * The mode mask for just the file permissions.
	 */
	public static final long PERMISSION_MASK = 07777;

	/**
	 * World execute permissions.
	 */
	public static final long OTHER_EXECUTE = 01;
	public static final long NOT_OTHER_EXECUTE = -1L - OTHER_EXECUTE;

	/**
	 * World write permissions.
	 */
	public static final long OTHER_WRITE = 02;
	public static final long NOT_OTHER_WRITE = -1L - OTHER_WRITE;

	/**
	 * World read permission.
	 */
	public static final long OTHER_READ = 04;
	public static final long NOT_OTHER_READ = -1L - OTHER_READ;

	/**
	 * Group execute permissions.
	 */
	public static final long GROUP_EXECUTE = 010;
	public static final long NOT_GROUP_EXECUTE = -1L - GROUP_EXECUTE;

	/**
	 * Group write permissions.
	 */
	public static final long GROUP_WRITE = 020;
	public static final long NOT_GROUP_WRITE = -1L - GROUP_WRITE;

	/**
	 * Group read permissions.
	 */
	public static final long GROUP_READ = 040;
	public static final long NOT_GROUP_READ = -1L - GROUP_READ;

	/**
	 * Owner execute permissions.
	 */
	public static final long USER_EXECUTE = 0100;
	public static final long NOT_USER_EXECUTE = -1L - USER_EXECUTE;

	/**
	 * Owner write permissions.
	 */
	public static final long USER_WRITE = 0200;
	public static final long NOT_USER_WRITE = -1L - USER_WRITE;

	/**
	 * Owner read permissions.
	 */
	public static final long USER_READ = 0400;
	public static final long NOT_USER_READ = -1L - USER_READ;

	/**
	 * Save text image.
	 */
	public static final long SAVE_TEXT_IMAGE = 01000;
	public static final long NOT_SAVE_TEXT_IMAGE = -1L - SAVE_TEXT_IMAGE;

	/**
	 * Set GID on execute.
	 */
	public static final long SET_GID = 02000;
	public static final long NOT_SET_GID = -1L - SET_GID;

	/**
	 * Set UID on execute.
	 */
	public static final long SET_UID = 04000;
	public static final long NOT_SET_UID = -1L - SET_UID;

	/**
	 * The mode mask for just the file type.
	 */
	public static final long TYPE_MASK = 0170000;

	/**
	 * Is a FIFO.
	 */
	public static final long IS_FIFO = 010000;

	/**
	 * Is a character special device.
	 */
	public static final long IS_CHARACTER_DEVICE = 020000;

	/**
	 * Is a directory.
	 */
	public static final long IS_DIRECTORY = 040000;

	/**
	 * Is a block device.
	 */
	public static final long IS_BLOCK_DEVICE = 060000;

	/**
	 * Is a regular file.
	 */
	public static final long IS_REGULAR_FILE = 0100000;

	/**
	 * Is a symbolic link.
	 */
	public static final long IS_SYM_LINK = 0120000;

	/**
	 * Is a socket.
	 */
	public static final long IS_SOCKET = 0140000;

	private static final Object libraryLock = new Object();
	volatile private static boolean loaded=false;
	public static void loadLibrary() {
		if(!loaded) {
			synchronized(libraryLock) {
				if(!loaded) {
					System.loadLibrary("aocode");
					loaded=true;
				}
			}
		}
	}

	/**
	 * The path.
	 */
	protected final String path;

	private File file;
	private UnixFile _parent;

	private static String checkPath(String path) {
		if(path.indexOf(0) != -1) {
			throw new IllegalArgumentException("Must not contain the NULL character: " + path);
		}
		return path;
	}

	/**
	 * Strictly requires the parent to be a directory if it exists.
	 *
	 * @deprecated  Please call #UnixFile(UnixFile,String,boolean) to explicitly control whether strict parent checking is performed
	 */
	@Deprecated
	public UnixFile(UnixFile parent, String path) throws IOException {
		this(parent, path, true);
	}

	/**
	 * When strictly checking, a parent must be a directory if it exists.
	 */
	public UnixFile(UnixFile parent, String path, boolean strict) throws IOException {
		if(parent==null) throw new NullPointerException("parent is null");
		if(strict) {
			Stat parentStat = parent.getStat();
			if(parentStat.exists() && !parentStat.isDirectory()) throw new IOException("parent is not a directory: " + parent.path);
		}
		if(parent.path.equals("/")) {
			this.path = checkPath(parent.path+path);
		} else {
			this.path = checkPath(parent.path + '/' + path);
		}
	}

	public UnixFile(File file) {
		if(file==null) throw new NullPointerException("file is null");
		this.path = checkPath(file.getPath());
		this.file=file;
	}

	public UnixFile(File parent, String filename) {
		this(parent.getPath(), filename);
	}

	public UnixFile(String path) {
		this.path = checkPath(path);
	}

	public UnixFile(String parent, String filename) {
		if(parent.equals("/")) {
			this.path = checkPath(parent+filename);
		} else {
			this.path = checkPath(parent + '/' + filename);
		}
	}

	/**
	 * Ensures that the calling thread is allowed to read this
	 * <code>UnixFile</code> in any way.
	 */
	final public void checkRead() throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkRead(getFile().getCanonicalPath());
	}

	/**
	 * Ensures that the calling thread is allowed to read this
	 * <code>path</code> in any way.
	 */
	static public void checkRead(String path) throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkRead(new File(path).getCanonicalPath());
	}

	/**
	 * Ensures that the calling thread is allowed to write to or modify this
	 * <code>UnixFile</code> in any way.
	 */
	final public void checkWrite() throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkWrite(getFile().getCanonicalPath());
	}

	/**
	 * Ensures that the calling thread is allowed to write to or modify this
	 * <code>path</code> in any way.
	 */
	static public void checkWrite(String path) throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkWrite(new File(path).getCanonicalPath());
	}

	/**
	 * Changes both the owner and group for a file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 */
	final public UnixFile chown(int uid, int gid) throws IOException {
		checkWrite();
		loadLibrary();
		chown0(path, uid, gid);
		return this;
	}

	private static native void chown0(String path, int uid, int gid) throws IOException;

	/**
	 * Stats the file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 */
	public Stat getStat() throws IOException {
		checkRead();
		loadLibrary();
		return getStat0(path);
	}

	private native Stat getStat0(String path) throws IOException;

	/**
	 * Compares this contents of this file to the contents of another file.
	 *
	 * This method will follow both path symbolic links and a final symbolic link.
	 */
	public boolean contentEquals(UnixFile otherUF) throws IOException {
		Stat stat = getStat();
		if(!stat.isRegularFile()) throw new IOException("Not a regular file: "+path);
		Stat otherStat = otherUF.getStat();
		if(!otherStat.isRegularFile()) throw new IOException("Not a regular file: "+otherUF.path);
		long size=stat.getSize();
		if(size!=otherStat.getSize()) return false;
		int buffSize=size<BufferManager.BUFFER_SIZE?(int)size:BufferManager.BUFFER_SIZE;
		if(buffSize<64) buffSize=64;
		try (
			InputStream in1 = new BufferedInputStream(new FileInputStream(getFile()), buffSize);
			InputStream in2 = new BufferedInputStream(new FileInputStream(otherUF.getFile()), buffSize)
		) {
			while(true) {
				int b1=in1.read();
				int b2=in2.read();
				if(b1!=b2) return false;
				if(b1==-1) break;
			}
		}
		return true;
	}

	/**
	 * Compares the contents of a file to a byte[].
	 *
	 * This method will follow both path symbolic links and a final symbolic link.
	 */
	public boolean contentEquals(byte[] otherFile) throws IOException {
		Stat stat = getStat();
		if(!stat.isRegularFile()) throw new IOException("Not a regular file: "+path);
		return FileUtils.contentEquals(getFile(), otherFile);
	}

	/**
	 * Compares this contents of this file to the contents of another file.
	 *
	 * This method will not follow any symbolic links and is not subject to race conditions.
	 *
	 * Java 1.8: Can do this in a pure Java way
	 */
	public boolean secureContentEquals(UnixFile otherUF, int uid_min, int gid_min) throws IOException {
		Stat stat = getStat();
		if(!stat.isRegularFile()) throw new IOException("Not a regular file: "+path);
		Stat otherStat = otherUF.getStat();
		if(!otherStat.isRegularFile()) throw new IOException("Not a regular file: "+otherUF.path);
		long size=stat.getSize();
		if(size!=otherStat.getSize()) return false;
		int buffSize=size<BufferManager.BUFFER_SIZE?(int)size:BufferManager.BUFFER_SIZE;
		if(buffSize<64) buffSize=64;
		try (
			InputStream in1 = new BufferedInputStream(getSecureInputStream(uid_min, gid_min), buffSize);
			InputStream in2 = new BufferedInputStream(otherUF.getSecureInputStream(uid_min, gid_min), buffSize)
		) {
			while(true) {
				int b1=in1.read();
				int b2=in2.read();
				if(b1!=b2) return false;
				if(b1==-1) break;
			}
		}
		return true;
	}

	/**
	 * Compares the contents of a file to a byte[].
	 *
	 * This method will not follow any symbolic links and is not subject to race conditions.
	 *
	 * Java 1.8: Can do this in a pure Java way
	 */
	public boolean secureContentEquals(byte[] otherFile, int uid_min, int gid_min) throws IOException {
		Stat stat = getStat();
		if(!stat.isRegularFile()) throw new IOException("Not a regular file: "+path);
		long size=stat.getSize();
		if(size!=otherFile.length) return false;
		int buffSize=size<BufferManager.BUFFER_SIZE?(int)size:BufferManager.BUFFER_SIZE;
		if(buffSize<64) buffSize=64;
		try (InputStream in1 = new BufferedInputStream(getSecureInputStream(uid_min, gid_min), buffSize)) {
			for(int c=0;c<otherFile.length;c++) {
				int b1=in1.read();
				int b2=otherFile[c]&0xff;
				if(b1!=b2) return false;
			}
		}
		return true;
	}

	private static final SecureRandom secureRandom = new SecureRandom();

	/**
	 * Copies one filesystem object to another.  It supports block devices, directories, fifos, regular files, and symbolic links.  Directories are not
	 * copied recursively.
	 *
	 * This method will follow both path symbolic links and a final symbolic link.
	 */
	public void copyTo(UnixFile otherUF, boolean overwrite) throws IOException {
		checkRead();
		otherUF.checkWrite();
		Stat stat = getStat();
		long mode=stat.getRawMode();
		Stat otherStat = otherUF.getStat();
		boolean oExists=otherStat.exists();
		if(!overwrite && oExists) throw new IOException("File already exists: "+otherUF);
		if(isBlockDevice(mode) || isCharacterDevice(mode)) {
			if(oExists) otherUF.delete();
			otherUF.mknod(mode, stat.getDeviceIdentifier()).chown(stat.getUid(), stat.getGid());
		} else if(isDirectory(mode)) {
			if(!oExists) otherUF.mkdir();
			otherUF.setMode(mode).chown(stat.getUid(), stat.getGid());
		} else if(isFifo(mode)) {
			if(oExists) otherUF.delete();
			otherUF.mkfifo(mode).chown(stat.getUid(), stat.getGid());
		} else if(isRegularFile(mode)) {
			try (
				InputStream in = new FileInputStream(getFile());
				OutputStream out = new FileOutputStream(otherUF.getFile())
			) {
				otherUF.setMode(mode).chown(stat.getUid(), stat.getGid());
				IoUtils.copy(in, out);
			}
		} else if(isSocket(mode)) throw new IOException("Unable to copy socket: "+path);
		else if(isSymLink(mode)) {
			// This takes the byte[] from readLink directory to symLink to avoid conversions from byte[]->String->byte[]
			otherUF.symLink(readLink()).chown(stat.getUid(), stat.getGid());
		} else throw new RuntimeException("Unknown mode type: "+Long.toOctalString(mode));
	}

	/**
	 * The set of supported crypt algorithms.
	 */
	public enum CryptAlgorithm {

		/**
		 * @deprecated This is the old-school weakest form, do not use unless somehow absolutely required.
		 */
		@Deprecated
		DES("", 2),

		/**
		 * @deprecated As of glibc 2.7, prefer the stronger {@link #SHA256} and {@link #SHA512} alternatives.
		 */
		@Deprecated
		MD5("$1$", 8),

		/**
		 * SHA-256 algorithm requires glibc 2.7+.
		 */
		SHA256("$5$", 16),

		/**
		 * SHA-512 algorithm requires glibc 2.7+.
		 */
		SHA512("$6$", 16);

		private final String saltPrefix;
		private final int saltLength;

		private CryptAlgorithm(String saltPrefix, int saltLength) {
			this.saltPrefix = saltPrefix;
			this.saltLength = saltLength;
		}

		public String getSaltPrefix() {
			return saltPrefix;
		}

		/**
		 * Gets the number of characters in the salt, not including the prefix.
		 */
		public int getSaltLength() {
			return saltLength;
		}

		/**
		 * Generates a random salt for this algorithm.
		 */
		public String generateSalt(SecureRandom secureRandom) {
			StringBuilder salt = new StringBuilder(saltPrefix.length() + saltLength);
			salt.append(saltPrefix);
			for(int c = 0; c < saltLength; c++) {
				int num = secureRandom.nextInt(64);
				if(num < 10) salt.append((char)(num + '0'));
				else if(num < 36) salt.append((char)(num - 10 + 'A'));
				else if(num < 62) salt.append((char)(num - 36 + 'a'));
				else if(num == 62) salt.append('.');
				else salt.append('/');
			}
			return salt.toString();
		}
	}

	/**
	 * Hashes a password using the MD5 crypt algorithm and the internal random source.
	 *
	 * @deprecated  Please provide the algorithm and call {@link #crypt(java.lang.String, com.aoindustries.io.unix.UnixFile.CryptAlgorithm)} instead.
	 */
	@Deprecated
	public static String crypt(String password) {
		return crypt(password, CryptAlgorithm.MD5, secureRandom);
	}

	/**
	 * Hashes a password using the MD5 crypt algorithm and the provided random source.
	 *
	 * @deprecated  Please provide the algorithm and call {@link #crypt(java.lang.String, com.aoindustries.io.unix.UnixFile.CryptAlgorithm, java.security.SecureRandom)} instead.
	 */
	@Deprecated
	public static String crypt(String password, SecureRandom secureRandom) {
		return crypt(password, CryptAlgorithm.MD5, secureRandom);
	}

	/**
	 * Hashes a password using the provided crypt algorithm and the internal random source.
	 */
	public static String crypt(String password, CryptAlgorithm algorithm) {
		return crypt(password, algorithm, secureRandom);
	}

	/**
	 * Hashes a password using the provided crypt algorithm and the provided random source.
	 */
	public static String crypt(String password, CryptAlgorithm algorithm, SecureRandom secureRandom) {
		return crypt(
			password,
			algorithm.generateSalt(secureRandom)
		);
	}

	/**
	 * crypt is not thread safe due to static data in the return value
	 */
	private static final Object cryptLock = new Object();

	/**
	 * Hashes a password using the provided salt.  The salt includes any
	 * {@link CryptAlgorithm#getSaltPrefix() salt prefix} for the algorithm.
	 * <p>
	 * Please refer to <code>man 3 crypt</code> for more details.
	 * </p>
	 */
	public static String crypt(String password, String salt) {
		loadLibrary();
		// crypt is not thread safe due to static data in the return value
		synchronized(cryptLock) {
			return crypt0(password, salt);
		}
	}

	private static native String crypt0(String password, String salt);

	/**
	 * Deletes this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @see  java.io.File#delete
	 */
	final public void delete() throws IOException {
		FileUtils.delete(getFile());
	}

	/**
	 * Deletes this file and if it is a directory, all files below it.
	 *
	 * Due to a race conditition, this method will follow symbolic links.  Please use
	 * <code>secureDeleteRecursive</code> instead.
	 *
	 * @see  #deleteRecursive(com.aoindustries.io.unix.UnixFile)
	 */
	final public void deleteRecursive() throws IOException {
		deleteRecursive(this);
	}

	/**
	 * @see  #deleteRecursive()
	 */
	private static void deleteRecursive(UnixFile file) throws IOException {
		try {
			Stat stat = file.getStat();
			// This next line matches directories specifically to avoid listing and recursing into symlink references
			if(stat.isDirectory()) {
				// TODO: Race condition between getStat and list(), how can we avoid this from pure Java???
				String[] list = file.list();
				if (list != null) {
					int len = list.length;
					for (int c = 0; c < len; c++) deleteRecursive(new UnixFile(file, list[c], false));
				}
			}
			file.delete();
		} catch(FileNotFoundException err) {
			// OK if it was deleted while we're trying to deleteRecursive it
		} catch(IOException err) {
			System.err.println("Error recursively delete: "+file.path);
			throw err;
		}
	}

	/**
	 * Java 1.8: Can do this in a pure Java way
	 */
	public static class SecuredDirectory {
		private final UnixFile directory;
		private final long mode;
		private final int uid;
		private final int gid;

		private SecuredDirectory(UnixFile directory, long mode, int uid, int gid) {
			this.directory=directory;
			this.mode=mode;
			this.uid=uid;
			this.gid=gid;
		}
	}

	/**
	 * Java 1.8: Can do this in a pure Java way
	 */
	final public void secureParents(
		List<SecuredDirectory> parentsChanged,
		int uid_min,
		int gid_min
	) throws IOException {
		// Build a stack of all parents
		Stack<UnixFile> parents=new Stack<>();
		{
			UnixFile parent=getParent();
			while(!parent.isRootDirectory()) {
				parents.push(parent);
				parent=parent.getParent();
			}
		}
		// Set any necessary permissions from root to file's immediate parent while looking for symbolic links
		while(!parents.isEmpty()) {
			UnixFile parent = parents.pop();
			Stat parentStat = parent.getStat();
			long statMode = parentStat.getRawMode();
			if(isSymLink(statMode)) throw new IOException("Symbolic link found in path: "+parent.path);
			int uid=parentStat.getUid();
			int gid=parentStat.getGid();
			if(
				uid >= uid_min
				|| gid >= gid_min
				|| (statMode&(OTHER_WRITE|SET_GID|SET_UID))!=0
			) {
				parentsChanged.add(new SecuredDirectory(parent, statMode, uid, gid));
				parent
					.setMode(statMode&(NOT_OTHER_WRITE & NOT_SET_GID & NOT_SET_UID))
					.chown(
						uid >= uid_min ? ROOT_UID : uid,
						gid >= gid_min ? ROOT_GID : gid
					)
				;
			}
		}
	}

	/**
	 * Java 1.8: Can do this in a pure Java way
	 */
	final public void restoreParents(List<SecuredDirectory> parentsChanged) throws IOException {
		for(int c=parentsChanged.size()-1;c>=0;c--) {
			SecuredDirectory directory=parentsChanged.get(c);
			directory.directory.chown(directory.uid, directory.gid).setMode(directory.mode);
		}
	}

	/**
	 * Securely deletes this file entry and all files below it while not following symbolic links.  This method must be called with
	 * root privileges to properly avoid race conditions.  If not running with root privileges, use <code>deleteRecursive</code> instead.<br>
	 * <br>
	 * In order to avoid race conditions, all directories above this directory will have their permissions set
	 * so that regular users cannot modify the directories.  After each parent directory has its permissions set
	 * it will then check for symbolic links.  Once all of the parent directories have been completed, the filesystem
	 * will recursively have its permissions reset, scans for symlinks, and deletes performed in such a way all
	 * race conditions are avoided.  Finally, the parent directory permissions that were modified will be restored.
	 *
	 * Java 1.8: Can do this in a pure Java way
	 *
	 * @see  #secureDeleteRecursive(com.aoindustries.io.unix.UnixFile)
	 */
	final public void secureDeleteRecursive(int uid_min, int gid_min) throws IOException {
		List<SecuredDirectory> parentsChanged=new ArrayList<>();
		try {
			secureParents(parentsChanged, uid_min, gid_min);
			secureDeleteRecursive(this);
		} finally {
			restoreParents(parentsChanged);
		}
	}

	/**
	 * @see  #secureDeleteRecursive(int, int)
	 */
	private static void secureDeleteRecursive(UnixFile file) throws IOException {
		try {
			Stat stat = file.getStat();
			long mode=stat.getRawMode();
			// Race condition does not exist because the parents have been secured already
			if(!isSymLink(mode) && isDirectory(mode)) {
				// Secure the current directory before the recursive calls
				if(stat.getUid()!=ROOT_UID || stat.getGid()!=ROOT_GID) file.chown(ROOT_UID, ROOT_GID);
				if(stat.getMode()!=0700) file.setMode(0700);
				String[] list = file.list();
				if (list != null) {
					int len = list.length;
					for (int c = 0; c < len; c++) secureDeleteRecursive(new UnixFile(file, list[c]));
				}
			}
			file.delete();
		} catch(IOException err) {
			System.err.println("Error recursively delete: "+file.path);
			throw err;
		}
	}

	/**
	 * Determines if a file exists, a symbolic link with an invalid destination
	 * is still considered to exist.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).exists()
	 */
	@Deprecated
	final public boolean exists() throws IOException {
		return getStat().exists();
	}

	/**
	 * Gets the last access to this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getAccessTime()
	 */
	@Deprecated
	final public long getAccessTime() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getAccessTime();
	}

	/**
	 * Gets the block count for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(State).getBlockCount()
	 */
	@Deprecated
	final public long getBlockCount() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getBlockCount();
	}

	/**
	 * Gets the block size for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getBlockSize()
	 */
	@Deprecated
	final public int getBlockSize() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getBlockSize();
	}

	/**
	 * Gets the change time of this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getChangeTime()
	 */
	@Deprecated
	final public long getChangeTime() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getChangeTime();
	}

	/**
	 * Gets the device for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getDevice()
	 */
	@Deprecated
	final public long getDevice() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getDevice();
	}

	/**
	 * Gets the device identifier for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getDeviceIdentifier()
	 */
	@Deprecated
	final public long getDeviceIdentifier() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getDeviceIdentifier();
	}

	/**
	 * Gets the extension for this file.
	 */
	final public String getExtension() {
		return FileUtils.getExtension(path);
	}

	/**
	 * Gets the <code>File</code> for this <code>UnixFile</code>.
	 * Not synchronized because multiple instantiation is acceptable.
	 */
	final public File getFile() {
		if (file == null) file = new File(path);
		return file;
	}

	/**
	 * Gets the path for this <code>UnixFile</code>.
	 *
	 * @deprecated  the use of the word <code>filename</code> is misleading since it represents the entire path, please use <code>getPath()</code> instead.
	 * @see  #getPath()
	 */
	@Deprecated
	final public String getFilename() {
		return path;
	}

	/**
	 * Gets the path for this <code>UnixFile</code>.
	 */
	final public String getPath() {
		return path;
	}

	/**
	 * Gets the group ID for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getGid()
	 */
	@Deprecated
	final public int getGid() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getGid();
	}

	/**
	 * Gets the inode for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getInode()
	 */
	@Deprecated
	final public long getInode() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getInode();
	}

	/**
	 * Gets the link count for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getNumberLinks()
	 */
	@Deprecated
	final public int getLinkCount() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getNumberLinks();
	}

	/**
	 * Gets the permission bits of the mode of this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getMode()
	 */
	@Deprecated
	final public long getMode() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getMode();
	}

	/**
	 * Gets a String representation of a mode similar to the output of the Unix ls command.
	 */
	public static String getModeString(long mode) {
		StringBuilder SB=new StringBuilder(10);
		if(isFifo(mode)) SB.append('p');
		else if(isCharacterDevice(mode)) SB.append('c');
		else if(isDirectory(mode)) SB.append('d');
		else if(isBlockDevice(mode)) SB.append('b');
		else if(isRegularFile(mode)) SB.append('-');
		else if(isSymLink(mode)) SB.append('l');
		else if(isSocket(mode)) SB.append('s');
		else throw new IllegalArgumentException("Unknown mode type: "+Long.toOctalString(mode));

		return SB
			.append((mode&USER_READ)!=0?'r':'-')
			.append((mode&USER_WRITE)!=0?'w':'-')
			.append((mode&USER_EXECUTE)!=0?((mode&SET_UID)!=0?'s':'x'):((mode&SET_UID)!=0?'S':'-'))
			.append((mode&GROUP_READ)!=0?'r':'-')
			.append((mode&GROUP_WRITE)!=0?'w':'-')
			.append((mode&GROUP_EXECUTE)!=0?((mode&SET_GID)!=0?'s':'x'):((mode&SET_GID)!=0?'S':'-'))
			.append((mode&OTHER_READ)!=0?'r':'-')
			.append((mode&OTHER_WRITE)!=0?'w':'-')
			.append((mode&OTHER_EXECUTE)!=0?((mode&SAVE_TEXT_IMAGE)!=0?'t':'x'):((mode&SAVE_TEXT_IMAGE)!=0?'T':'-'))
			.toString()
		;
	}

	/**
	 * Gets a String representation of the mode of this file similar to the output of the Unix ls command.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getModeString()
	 */
	@Deprecated
	final public String getModeString() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getModeString();
	}

	/**
	 * Securely gets a <code>FileInputStream</code> to this file, temporarily performing permission
	 * changes and ensuring that no symbolic links are anywhere in the path.
	 *
	 * Java 1.8: Can do this in a pure Java way
	 */
	final public FileInputStream getSecureInputStream(int uid_min, int gid_min) throws IOException {
		List<SecuredDirectory> parentsChanged=new ArrayList<>();
		try {
			secureParents(parentsChanged, uid_min, gid_min);

			// Make sure the file does not exist
			if(!getStat().isRegularFile()) throw new IOException("Not a regular file: "+path);

			// Create the new file with the correct owner and permissions
			return new FileInputStream(getFile());
		} finally {
			restoreParents(parentsChanged);
		}
	}

	/**
	 * Securely gets a <code>FileOutputStream</code> to this file, temporarily performing permission
	 * changes and ensuring that no symbolic links are anywhere in the path.
	 *
	 * TODO: Consider the impact of using mktemp instead of secureParents/restoreParents because there
	 *       is the possibility that permissions may not be restored if the JVM is shutdown at that moment.
	 *
	 * Java 1.8: Can do this in a pure Java way
	 */
	final public FileOutputStream getSecureOutputStream(int uid, int gid, long mode, boolean overwrite, int uid_min, int gid_min) throws IOException {
		List<SecuredDirectory> parentsChanged=new ArrayList<>();
		try {
			secureParents(parentsChanged, uid_min, gid_min);

			// Make sure the file does not exist
			Stat stat = getStat();
			if(overwrite) {
				if(stat.exists() && !stat.isRegularFile()) throw new IOException("Not a regular file: "+path);
			} else {
				if(stat.exists()) throw new IOException("File already exists: "+path);
			}

			// Create the new file with the correct owner and permissions
			FileOutputStream out=new FileOutputStream(getFile());
			chown(uid, gid).setMode(mode);
			return out;
		} finally {
			restoreParents(parentsChanged);
		}
	}

	/**
	 * Securely gets a <code>RandomAccessFile</code> to this file, temporarily performing permission
	 * changes and ensuring that no symbolic links are anywhere in the path.
	 *
	 * Java 1.8: Can do this in a pure Java way
	 */
	final public RandomAccessFile getSecureRandomAccessFile(String mode, int uid_min, int gid_min) throws IOException {
		List<SecuredDirectory> parentsChanged=new ArrayList<>();
		try {
			secureParents(parentsChanged, uid_min, gid_min);

			// Make sure the file does not exist
			if(!getStat().isRegularFile()) throw new IOException("Not a regular file: "+path);

			// Create the new file with the correct owner and permissions
			return new RandomAccessFile(getFile(), mode);
		} finally {
			restoreParents(parentsChanged);
		}
	}

	/**
	 * Gets the parent of this file or <code>null</code> if it doesn't have a parent.
	 * Not synchronized because multiple instantiation is acceptable.
	 */
	final public UnixFile getParent() {
		if(_parent==null) {
			File parentFile = getFile().getParentFile();
			if(parentFile!=null) _parent = new UnixFile(parentFile);
		}
		return _parent;
	}

	/**
	 * Gets the complete mode of the file, including the bits representing the
	 * file type.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getRawMode()
	 */
	@Deprecated
	final public long getStatMode() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getRawMode();
	}

	/**
	 * Gets the modification time of the file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getModifyTime()
	 */
	@Deprecated
	final public long getModifyTime() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getModifyTime();
	}

	/**
	 * Gets the size of the file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getSize()
	 */
	@Deprecated
	final public long getSize() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getSize();
	}

	/**
	 * Securely creates a temporary file, not deleting on exit.  In order to be secure, though, the directory
	 * needs to be secure, or at least have the sticky bit set.
	 *
	 * This method will follow symbolic links in the path but not final links.
	 *
	 * @see  #mktemp(String,boolean)
	 */
	public static UnixFile mktemp(String template) throws IOException {
		try {
			String path = template + "XXXXXXXXXX";
			checkWrite(path);
			loadLibrary();
			return new UnixFile(mktemp0(path));
		} catch(IOException err) {
			System.err.println("UnixFile.mktemp: IOException: template="+template);
			throw err;
		}
	}

	/**
	 * Securely creates a temporary file.  In order to be secure, though, the directory
	 * needs to be secure, or at least have the sticky bit set.
	 *
	 * This method will follow symbolic links in the path but not final links.
	 *
	 * @deprecated  Please use <a href="https://aoindustries.com/ao-tempfiles/apidocs/com/aoindustries/tempfiles/TempFileContext.html">TempFileContext</a>
	 *              as {@link File#deleteOnExit()} is prone to memory leaks in long-running applications.
	 */
	@Deprecated
	public static UnixFile mktemp(String template, boolean deleteOnExit) throws IOException {
		UnixFile uf = mktemp(template);
		if(deleteOnExit) uf.getFile().deleteOnExit();
		return uf;
	}

	private static native String mktemp0(String template) throws IOException;

	/**
	 * Gets the user ID of the file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getUid()
	 */
	@Deprecated
	public final int getUid() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getUid();
	}

	/**
	 * Determines if a specific mode represents a block device.
	 */
	public static boolean isBlockDevice(long mode) {
		return (mode & TYPE_MASK) == IS_BLOCK_DEVICE;
	}

	/**
	 * Determines if this file represents a block device.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).isBlockDevice()
	 */
	@Deprecated
	final public boolean isBlockDevice() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isBlockDevice();
	}

	/**
	 * Determines if a specific mode represents a character device.
	 */
	public static boolean isCharacterDevice(long mode) {
		return (mode & TYPE_MASK) == IS_CHARACTER_DEVICE;
	}

	/**
	 * Determines if this file represents a character device.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).isCharacterDevice()
	 */
	@Deprecated
	final public boolean isCharacterDevice() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isCharacterDevice();
	}

	/**
	 * Determines if a specific mode represents a directory.
	 */
	public static boolean isDirectory(long mode) {
		return (mode & TYPE_MASK) == IS_DIRECTORY;
	}

	/**
	 * Determines if this file represents a directory.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).isDirectory()
	 */
	@Deprecated
	final public boolean isDirectory() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isDirectory();
	}

	/**
	 * Determines if a specific mode represents a FIFO.
	 */
	public static boolean isFifo(long mode) {
		return (mode & TYPE_MASK) == IS_FIFO;
	}

	/**
	 * Determines if this file represents a FIFO.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).isFifo()
	 */
	@Deprecated
	final public boolean isFifo() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isFifo();
	}

	/**
	 * Determines if a specific mode represents a regular file.
	 */
	public static boolean isRegularFile(long mode) {
		return
			(mode & TYPE_MASK) == IS_REGULAR_FILE
			|| (mode & TYPE_MASK) == 0
		;
	}

	/**
	 * Determines if this file represents a regular file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).isRegularFile()
	 */
	@Deprecated
	final public boolean isRegularFile() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isRegularFile();
	}

	/**
	 * Determines if this file is the root directory.
	 */
	final public boolean isRootDirectory() {
		return path.equals("/");
	}

	/**
	 * Determines if a specific mode represents a socket.
	 */
	public static boolean isSocket(long mode) {
		return (mode & TYPE_MASK) == IS_SOCKET;
	}

	/**
	 * Determines if this file represents a socket.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).isSocket()
	 */
	@Deprecated
	final public boolean isSocket() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isSocket();
	}

	/**
	 * Determines if a specific mode represents a symbolic link.
	 */
	public static boolean isSymLink(long mode) {
		return (mode & TYPE_MASK) == IS_SYM_LINK;
	}

	/**
	 * Determines if this file represents a sybolic link.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).isSymLink()
	 */
	@Deprecated
	final public boolean isSymLink() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isSymLink();
	}

	/**
	 * Lists the contents of the directory.
	 *
	 * This method will follow symbolic links in the path, including a final symbolic link.
	 *
	 * @see java.io.File#list
	 */
	final public String[] list() {
		return getFile().list();
	}

	/**
	 * Creates a directory.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile mkdir() throws IOException {
		if(!getFile().mkdir()) throw new IOException("Unable to make directory: " + path);
		return this;
	}

	/**
	 * Creates a directory and sets its permissions, optionally creating all the parent directories if they
	 * do not exist.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile mkdir(boolean makeParents, long mode) throws IOException {
		if(makeParents) {
			UnixFile dir=getParent();
			Stack<UnixFile> neededParents=new Stack<>();
			while(!dir.isRootDirectory() && !dir.getStat().exists()) {
				neededParents.push(dir);
				dir=dir.getParent();
			}
			while(!neededParents.isEmpty()) {
				dir=neededParents.pop();
				dir.mkdir().setMode(mode);
			}
		}
		return mkdir().setMode(mode);
	}

	/**
	 * Creates a directory and sets its permissions, optionally creating all the parent directories if they
	 * do not exist.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile mkdir(boolean makeParents, long mode, int uid, int gid) throws IOException {
		if(makeParents) {
			UnixFile dir=getParent();
			Stack<UnixFile> neededParents=new Stack<>();
			while(!dir.isRootDirectory() && !dir.getStat().exists()) {
				neededParents.push(dir);
				dir=dir.getParent();
			}
			while(!neededParents.isEmpty()) {
				dir=neededParents.pop();
				dir.mkdir().setMode(mode).chown(uid, gid);
			}
		}
		return mkdir().setMode(mode).chown(uid, gid);
	}

	/**
	 * Creates a device file.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile mknod(long mode, long device) throws IOException {
		checkWrite();
		loadLibrary();
		mknod0(path, mode, device);
		return this;
	}

	private static native void mknod0(String path, long mode, long device) throws IOException;

	/**
	 * Creates a FIFO.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile mkfifo(long mode) throws IOException {
		checkWrite();
		loadLibrary();
		mkfifo0(path, mode&PERMISSION_MASK);
		return this;
	}

	private static native void mkfifo0(String path, long mode) throws IOException;

	/**
	 * Sets the access time for this file.
	 *
	 * This method will follow symbolic links in the path.
	 *
	 * @deprecated  This method internally performs an extra stat.  Please try to use utime(long,long) directly to avoid this extra stat.
	 */
	@Deprecated
	final public UnixFile setAccessTime(long atime) throws IOException {
		checkWrite();
		// getStat does loadLibrary already: loadLibrary();
		long mtime = getStat().getModifyTime();
		utime0(path, atime, mtime);
		return this;
	}

	/**
	 * Sets the group ID for this file.
	 *
	 * This method will follow symbolic links in the path.
	 *
	 * @deprecated  This method internally performs an extra stat.  Please try to use chown(int,int) directly to avoid this extra stat.
	 */
	@Deprecated
	final public UnixFile setGID(int gid) throws IOException {
		checkWrite();
		// getStat does loadLibrary already: loadLibrary();
		int uid = getStat().getUid();
		chown0(path, uid, gid);
		return this;
	}

	/**
	 * Sets the permissions for this file.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile setMode(long mode) throws IOException {
		checkWrite();
		loadLibrary();
		setMode0(path, mode & PERMISSION_MASK);
		return this;
	}

	private static native void setMode0(String path, long mode) throws IOException;

	/**
	 * Sets the modification time for this file.
	 *
	 * This method will follow symbolic links in the path.
	 *
	 * @deprecated  This method internally performs an extra stat.  Please try to use utime(long,long) directly to avoid this extra stat.
	 */
	@Deprecated
	final public UnixFile setModifyTime(long mtime) throws IOException {
		checkWrite();
		// getStat does loadLibrary already: loadLibrary();
		long atime = getStat().getAccessTime();
		utime0(path, atime, mtime);
		return this;
	}

	/**
	 * Sets the user ID for this file.
	 *
	 * This method will follow symbolic links in the path.
	 *
	 * @deprecated  This method internally performs an extra stat.  Please try to use chown(int,int) directly to avoid this extra stat.
	 */
	@Deprecated
	final public UnixFile setUID(int uid) throws IOException {
		checkWrite();
		// getStat does loadLibrary already: loadLibrary();
		int gid = getStat().getGid();
		chown0(path, uid, gid);
		return this;
	}

	/**
	 * Creates a symbolic link.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile symLink(String destination) throws IOException {
		checkWrite();
		loadLibrary();
		symLink0(path, destination);
		return this;
	}

	static native private void symLink0(String path, String destination) throws IOException;

	/**
	 * Creates a hard link.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile link(UnixFile destination) throws IOException {
		return link(destination.getPath());
	}

	/**
	 * Creates a hard link.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile link(String destination) throws IOException {
		checkWrite();
		loadLibrary();
		link0(path, destination);
		return this;
	}

	static native private void link0(String path, String destination) throws IOException;

	/**
	 * Reads a symbolic link.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public String readLink() throws IOException {
		checkRead();
		loadLibrary();
		return readLink0(path);
	}

	static native private String readLink0(String path) throws IOException;

	/**
	 * Renames this file, possibly overwriting any previous file.
	 *
	 * This method will follow symbolic links in the path.
	 *
	 * @see File#renameTo(File)
	 */
	final public void renameTo(UnixFile uf) throws IOException {
		FileUtils.rename(getFile(), uf.getFile());
	}

	@Override
	final public String toString() {
		return path;
	}

	/**
	 * Sets the access and modify times for this file.
	 *
	 * This method will follow symbolic links in the path.
	 */
	final public UnixFile utime(long atime, long mtime) throws IOException {
		checkWrite();
		loadLibrary();
		utime0(path, atime, mtime);
		return this;
	}

	private static native void utime0(String path, long atime, long mtime) throws IOException;

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public boolean equals(Object O) {
		return
			O!=null
			&& (O instanceof UnixFile)
			&& ((UnixFile) O).path.equals(path)
		;
	}
}
