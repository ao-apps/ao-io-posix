/*
 * ao-io-posix - Java interface to native POSIX filesystem objects.
 * Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2015, 2016, 2017, 2018, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoapps.lang.io.FileUtils;
import com.aoapps.lang.io.IoUtils;
import com.aoapps.lang.util.BufferManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access and modify all the POSIX-specific file attributes.  These updates are made using
 * a Linux shared library provided as a resource.  The source code is also supplied.
 * <p>
 * Note: The JVM must be in a single-byte locale, such as "C", "POSIX", or
 * "en_US".  PosixFile makes this assumption in its JNI implementation.
 * </p>
 *
 * @author  AO Industries, Inc.
 */
public class PosixFile {

	private static final Logger logger = Logger.getLogger(PosixFile.class.getName());

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
	private static volatile boolean loaded=false;
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

	private File file; // TODO: volatile?
	private PosixFile _parent; // TODO: volatile?

	private static String checkPath(String path) {
		if(path.indexOf(0) != -1) {
			throw new IllegalArgumentException("Must not contain the NULL character: " + path);
		}
		return path;
	}

	/**
	 * Strictly requires the parent to be a directory if it exists.
	 *
	 * @deprecated  Please call #PosixFile(PosixFile,String,boolean) to explicitly control whether strict parent checking is performed
	 */
	@Deprecated // Java 9: (forRemoval = false)
	public PosixFile(PosixFile parent, String path) throws IOException {
		this(parent, path, true);
	}

	/**
	 * When strictly checking, a parent must be a directory if it exists.
	 */
	public PosixFile(PosixFile parent, String path, boolean strict) throws IOException {
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

	public PosixFile(File file) {
		if(file==null) throw new NullPointerException("file is null");
		this.path = checkPath(file.getPath());
		this.file=file;
	}

	public PosixFile(File parent, String filename) {
		this(parent.getPath(), filename);
	}

	public PosixFile(String path) {
		this.path = checkPath(path);
	}

	public PosixFile(String parent, String filename) {
		if(parent.equals("/")) {
			this.path = checkPath(parent+filename);
		} else {
			this.path = checkPath(parent + '/' + filename);
		}
	}

	/**
	 * Ensures that the calling thread is allowed to read this
	 * <code>PosixFile</code> in any way.
	 */
	public final void checkRead() throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkRead(getFile().getCanonicalPath());
	}

	/**
	 * Ensures that the calling thread is allowed to read this
	 * <code>path</code> in any way.
	 */
	public static void checkRead(String path) throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkRead(new File(path).getCanonicalPath());
	}

	/**
	 * Ensures that the calling thread is allowed to write to or modify this
	 * <code>PosixFile</code> in any way.
	 */
	public final void checkWrite() throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkWrite(getFile().getCanonicalPath());
	}

	/**
	 * Ensures that the calling thread is allowed to write to or modify this
	 * <code>path</code> in any way.
	 */
	public static void checkWrite(String path) throws IOException {
		SecurityManager security=System.getSecurityManager();
		if(security!=null) security.checkWrite(new File(path).getCanonicalPath());
	}

	/**
	 * Changes both the owner and group for a file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 */
	public final PosixFile chown(int uid, int gid) throws IOException {
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
	public boolean contentEquals(PosixFile otherUF) throws IOException {
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
	 * TODO: Java 1.8: Can do this in a pure Java way
	 */
	public boolean secureContentEquals(PosixFile otherUF, int uid_min, int gid_min) throws IOException {
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
	 * TODO: Java 1.8: Can do this in a pure Java way
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

	/**
	 * Note: This is not a {@linkplain SecureRandom#getInstanceStrong() strong instance} to avoid blocking.
	 */
	private static final SecureRandom secureRandom = new SecureRandom();

	/**
	 * Copies one filesystem object to another.  It supports block devices, directories, fifos, regular files, and symbolic links.  Directories are not
	 * copied recursively.
	 *
	 * This method will follow both path symbolic links and a final symbolic link.
	 */
	public void copyTo(PosixFile otherUF, boolean overwrite) throws IOException {
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
		@Deprecated // Java 9: (forRemoval = false)
		DES("", 2),

		/**
		 * @deprecated As of glibc 2.7, prefer the stronger {@link #SHA256} and {@link #SHA512} alternatives.
		 */
		@Deprecated // Java 9: (forRemoval = false)
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
		// TODO: Use Key from ao-security
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
	 * Hashes a password using the MD5 crypt algorithm and a default {@link SecureRandom} instance, which is not a
	 * {@linkplain SecureRandom#getInstanceStrong() strong instance} to avoid blocking.
	 *
	 * @deprecated  Please provide the algorithm and call {@link #crypt(java.lang.String, com.aoapps.io.posix.PosixFile.CryptAlgorithm)} instead.
	 */
	@Deprecated // Java 9: (forRemoval = true)
	// TODO: Take Password instances from ao-security instead?
	public static String crypt(String password) {
		return crypt(password, CryptAlgorithm.MD5, secureRandom);
	}

	/**
	 * Hashes a password using the MD5 crypt algorithm and the provided random source.
	 *
	 * @deprecated  Please provide the algorithm and call {@link #crypt(java.lang.String, com.aoapps.io.posix.PosixFile.CryptAlgorithm, java.security.SecureRandom)} instead.
	 */
	@Deprecated // Java 9: (forRemoval = true)
	// TODO: Take Password instances from ao-security instead?
	public static String crypt(String password, SecureRandom secureRandom) {
		return crypt(password, CryptAlgorithm.MD5, secureRandom);
	}

	/**
	 * Hashes a password using the provided crypt algorithm and a default {@link SecureRandom} instance, which is not a
	 * {@linkplain SecureRandom#getInstanceStrong() strong instance} to avoid blocking.
	 */
	// TODO: Take Password instances from ao-security instead?
	public static String crypt(String password, CryptAlgorithm algorithm) {
		return crypt(password, algorithm, secureRandom);
	}

	/**
	 * Hashes a password using the provided crypt algorithm and the provided random source.
	 */
	// TODO: Take Password instances from ao-security instead?
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
	// TODO: Take Password instances from ao-security instead?
	public static String crypt(String password, String salt) {
		loadLibrary();
		// crypt is not thread safe due to static data in the return value
		synchronized(cryptLock) {
			return crypt0(password, salt);
		}
	}

	// TODO: Take Password instances from ao-security instead?
	private static native String crypt0(String password, String salt);

	/**
	 * Deletes this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @see  java.io.File#delete
	 */
	public final void delete() throws IOException {
		Files.delete(getFile().toPath());
	}

	/**
	 * Deletes this file and if it is a directory, all files below it.
	 *
	 * Due to a race conditition, this method will follow symbolic links.  Please use
	 * <code>secureDeleteRecursive</code> instead.
	 *
	 * @see  #deleteRecursive(com.aoapps.io.posix.PosixFile)
	 */
	public final void deleteRecursive() throws IOException {
		deleteRecursive(this);
	}

	/**
	 * @see  #deleteRecursive()
	 */
	private static void deleteRecursive(PosixFile file) throws IOException {
		try {
			Stat stat = file.getStat();
			// This next line matches directories specifically to avoid listing and recursing into symlink references
			if(stat.isDirectory()) {
				// TODO: Race condition between getStat and list(), how can we avoid this from pure Java???
				String[] list = file.list();
				if (list != null) {
					int len = list.length;
					for (int c = 0; c < len; c++) {
						deleteRecursive(new PosixFile(file, list[c], false));
					}
				}
			}
			file.delete();
		} catch(FileNotFoundException err) {
			// OK if it was deleted while we're trying to deleteRecursive it
		} catch(IOException err) {
			if(logger.isLoggable(Level.FINER)) logger.finer("Error recursively delete: " + file.path);
			throw err;
		}
	}

	/**
	 * TODO: Java 1.8: Can do this in a pure Java way
	 */
	public static class SecuredDirectory {
		private final PosixFile directory;
		private final long mode;
		private final int uid;
		private final int gid;

		private SecuredDirectory(PosixFile directory, long mode, int uid, int gid) {
			this.directory=directory;
			this.mode=mode;
			this.uid=uid;
			this.gid=gid;
		}
	}

	/**
	 * TODO: Java 1.8: Can do this in a pure Java way
	 */
	public final void secureParents(
		List<SecuredDirectory> parentsChanged,
		int uid_min,
		int gid_min
	) throws IOException {
		// Build a stack of all parents
		Stack<PosixFile> parents=new Stack<>();
		{
			PosixFile parent=getParent();
			while(!parent.isRootDirectory()) {
				parents.push(parent);
				parent=parent.getParent();
			}
		}
		// Set any necessary permissions from root to file's immediate parent while looking for symbolic links
		while(!parents.isEmpty()) {
			PosixFile parent = parents.pop();
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
	 * TODO: Java 1.8: Can do this in a pure Java way
	 */
	public final void restoreParents(List<SecuredDirectory> parentsChanged) throws IOException {
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
	 * TODO: Java 1.8: Can do this in a pure Java way
	 *
	 * @see  #secureDeleteRecursive(com.aoapps.io.posix.PosixFile)
	 */
	public final void secureDeleteRecursive(int uid_min, int gid_min) throws IOException {
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
	private static void secureDeleteRecursive(PosixFile file) throws IOException {
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
					for (int c = 0; c < len; c++) {
						secureDeleteRecursive(new PosixFile(file, list[c]));
					}
				}
			}
			file.delete();
		} catch(IOException err) {
			if(logger.isLoggable(Level.FINER)) logger.finer("Error recursively delete: " + file.path);
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean exists() throws IOException {
		return getStat().exists();
	}

	/**
	 * Gets the last access to this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getAccessTime()
	 */
	@Deprecated // Java 9: (forRemoval = true)
	public final long getAccessTime() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getBlockCount() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final int getBlockSize() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getChangeTime() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getDevice() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getDeviceIdentifier() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getDeviceIdentifier();
	}

	/**
	 * Gets the extension for this file.
	 */
	public final String getExtension() {
		return FileUtils.getExtension(path);
	}

	/**
	 * Gets the <code>File</code> for this <code>PosixFile</code>.
	 * Not synchronized because multiple instantiation is acceptable.
	 */
	public final File getFile() {
		if (file == null) file = new File(path);
		return file;
	}

	/**
	 * Gets the path for this <code>PosixFile</code>.
	 *
	 * @deprecated  the use of the word <code>filename</code> is misleading since it represents the entire path, please use <code>getPath()</code> instead.
	 * @see  #getPath()
	 */
	@Deprecated // Java 9: (forRemoval = true)
	public final String getFilename() {
		return path;
	}

	/**
	 * Gets the path for this <code>PosixFile</code>.
	 */
	public final String getPath() {
		return path;
	}

	/**
	 * Gets the group ID for this file.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getGid()
	 */
	@Deprecated // Java 9: (forRemoval = true)
	public final int getGid() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getInode() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final int getLinkCount() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getMode() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getMode();
	}

	/**
	 * Gets a String representation of a mode similar to the output of the POSIX <code>ls</code> command.
	 */
	public static String getModeString(long mode) {
		StringBuilder sb = new StringBuilder(10);
		if(isFifo(mode)) sb.append('p');
		else if(isCharacterDevice(mode)) sb.append('c');
		else if(isDirectory(mode)) sb.append('d');
		else if(isBlockDevice(mode)) sb.append('b');
		else if(isRegularFile(mode)) sb.append('-');
		else if(isSymLink(mode)) sb.append('l');
		else if(isSocket(mode)) sb.append('s');
		else throw new IllegalArgumentException("Unknown mode type: "+Long.toOctalString(mode));

		return sb
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
	 * Gets a String representation of the mode of this file similar to the output of the POSIX <code>ls</code> command.
	 *
	 * This method will follow symbolic links in the path but not a final symbolic link.
	 *
	 * @deprecated  Please use getStat(Stat).getModeString()
	 */
	@Deprecated // Java 9: (forRemoval = true)
	public final String getModeString() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.getModeString();
	}

	/**
	 * Securely gets a <code>FileInputStream</code> to this file, temporarily performing permission
	 * changes and ensuring that no symbolic links are anywhere in the path.
	 *
	 * TODO: Java 1.8: Can do this in a pure Java way
	 */
	public final FileInputStream getSecureInputStream(int uid_min, int gid_min) throws IOException {
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
	 * TODO: Java 1.8: Can do this in a pure Java way
	 */
	public final FileOutputStream getSecureOutputStream(int uid, int gid, long mode, boolean overwrite, int uid_min, int gid_min) throws IOException {
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
	 * TODO: Java 1.8: Can do this in a pure Java way
	 */
	public final RandomAccessFile getSecureRandomAccessFile(String mode, int uid_min, int gid_min) throws IOException {
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
	public final PosixFile getParent() {
		if(_parent==null) {
			File parentFile = getFile().getParentFile();
			if(parentFile!=null) _parent = new PosixFile(parentFile);
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getStatMode() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getModifyTime() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final long getSize() throws IOException {
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
	 *
	 * @deprecated  Please use {@link Files#createTempFile(java.lang.String, java.lang.String, java.nio.file.attribute.FileAttribute...)}.
	 */
	@Deprecated // Java 9: (forRemoval = true)
	public static PosixFile mktemp(String template) throws IOException {
		try {
			String path = template + "XXXXXXXXXX";
			checkWrite(path);
			loadLibrary();
			return new PosixFile(mktemp0(path));
		} catch(IOException err) {
			if(logger.isLoggable(Level.FINER)) logger.finer("PosixFile.mktemp: IOException: template=" + template);
			throw err;
		}
	}

	/**
	 * Securely creates a temporary file.  In order to be secure, though, the directory
	 * needs to be secure, or at least have the sticky bit set.
	 *
	 * This method will follow symbolic links in the path but not final links.
	 *
	 * @deprecated  Please use {@link Files#createTempFile(java.lang.String, java.lang.String, java.nio.file.attribute.FileAttribute...)}
	 *              or <a href="https://oss.aoapps.com/tempfiles/apidocs/com.aoapps.tempfiles/com/aoapps/tempfiles/TempFileContext.html">TempFileContext</a>
	 *              as {@link File#deleteOnExit()} is prone to memory leaks in long-running applications.
	 */
	@Deprecated // Java 9: (forRemoval = true)
	public static PosixFile mktemp(String template, boolean deleteOnExit) throws IOException {
		PosixFile uf = mktemp(template);
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
	@Deprecated // Java 9: (forRemoval = true)
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean isBlockDevice() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean isCharacterDevice() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean isDirectory() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean isFifo() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean isRegularFile() throws IOException {
		Stat stat = getStat();
		if(!stat.exists()) throw new FileNotFoundException(path);
		return stat.isRegularFile();
	}

	/**
	 * Determines if this file is the root directory.
	 */
	public final boolean isRootDirectory() {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean isSocket() throws IOException {
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
	@Deprecated // Java 9: (forRemoval = true)
	public final boolean isSymLink() throws IOException {
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
	public final String[] list() {
		return getFile().list();
	}

	/**
	 * Creates a directory.
	 *
	 * This method will follow symbolic links in the path.
	 */
	public final PosixFile mkdir() throws IOException {
		if(!getFile().mkdir()) throw new IOException("Unable to make directory: " + path);
		return this;
	}

	/**
	 * Creates a directory and sets its permissions, optionally creating all the parent directories if they
	 * do not exist.
	 *
	 * This method will follow symbolic links in the path.
	 */
	public final PosixFile mkdir(boolean makeParents, long mode) throws IOException {
		if(makeParents) {
			PosixFile dir=getParent();
			Stack<PosixFile> neededParents=new Stack<>();
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
	public final PosixFile mkdir(boolean makeParents, long mode, int uid, int gid) throws IOException {
		if(makeParents) {
			PosixFile dir=getParent();
			Stack<PosixFile> neededParents=new Stack<>();
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
	public final PosixFile mknod(long mode, long device) throws IOException {
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
	public final PosixFile mkfifo(long mode) throws IOException {
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
	@Deprecated // Java 9: (forRemoval = false)
	public final PosixFile setAccessTime(long atime) throws IOException {
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
	@Deprecated // Java 9: (forRemoval = false)
	public final PosixFile setGID(int gid) throws IOException {
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
	public final PosixFile setMode(long mode) throws IOException {
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
	@Deprecated // Java 9: (forRemoval = false)
	public final PosixFile setModifyTime(long mtime) throws IOException {
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
	@Deprecated // Java 9: (forRemoval = false)
	public final PosixFile setUID(int uid) throws IOException {
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
	public final PosixFile symLink(String destination) throws IOException {
		checkWrite();
		loadLibrary();
		symLink0(path, destination);
		return this;
	}

	private static native void symLink0(String path, String destination) throws IOException;

	/**
	 * Creates a hard link.
	 *
	 * This method will follow symbolic links in the path.
	 */
	public final PosixFile link(PosixFile destination) throws IOException {
		return link(destination.getPath());
	}

	/**
	 * Creates a hard link.
	 *
	 * This method will follow symbolic links in the path.
	 */
	public final PosixFile link(String destination) throws IOException {
		checkWrite();
		loadLibrary();
		link0(path, destination);
		return this;
	}

	private static native void link0(String path, String destination) throws IOException;

	/**
	 * Reads a symbolic link.
	 *
	 * This method will follow symbolic links in the path.
	 */
	public final String readLink() throws IOException {
		checkRead();
		loadLibrary();
		return readLink0(path);
	}

	private static native String readLink0(String path) throws IOException;

	/**
	 * Renames this file, possibly overwriting any previous file.
	 *
	 * This method will follow symbolic links in the path.
	 *
	 * @see File#renameTo(File)
	 */
	public final void renameTo(PosixFile uf) throws IOException {
		FileUtils.rename(getFile(), uf.getFile());
	}

	@Override
	public final String toString() {
		return path;
	}

	/**
	 * Sets the access and modify times for this file.
	 *
	 * This method will follow symbolic links in the path.
	 */
	public final PosixFile utime(long atime, long mtime) throws IOException {
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
	public boolean equals(Object obj) {
		return
			(obj instanceof PosixFile)
			&& ((PosixFile)obj).path.equals(path)
		;
	}
}
