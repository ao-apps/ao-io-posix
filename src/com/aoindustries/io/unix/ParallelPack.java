/*
 * ao-io-unix - Java interface to native Unix filesystem objects.
 * Copyright (C) 2009, 2010, 2011, 2013, 2015  AO Industries, Inc.
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

import com.aoindustries.io.CompressedDataOutputStream;
import com.aoindustries.io.FilesystemIterator;
import com.aoindustries.io.FilesystemIteratorRule;
import com.aoindustries.util.BufferManager;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

/**
 * <p>
 * Our backup directories contain parallel directories with many hard links.
 * rsync and tar both use extreme amounts of RAM to manipulate these
 * directories and often fail or become extremely slow due to excessive
 * swapping.
 * </p>
 * <p>
 * To work around this problem and be able to move directory trees from host to
 * host, this tool will combine the set of directories and write them to
 * <code>System.out</code>.  This is similar to tar.  The output is then
 * unpacked using <code>ParallelUnpack</code>, which could be a direct pipe,
 * through <code>ssh</code>, <code>nc</code>, or any other mechanism.
 * </p>
 * <p>
 * For efficiency, direct TCP communication is supported with the <code>-h</code> option.
 * </p>
 * <p>
 * It assumes that the file system is not changing, results of use on a changing
 * filesystem is not defined.
 * </p>
 * 
 * @see  ParallelUnpack
 *
 * @author  AO Industries, Inc.
 */
public class ParallelPack {

	/**
	 * The size of the verbose output queue.
	 */
	private static final int VERBOSE_QUEUE_SIZE = 1000;

	/**
	 * Make no instances.
	 */
	private ParallelPack() {}

	/**
	 * Packs multiple directories in parallel (but not concurrently).
	 */
	public static void main(String[] args) {
		if(args.length==0) {
			System.err.println("Usage: "+ParallelPack.class.getName()+" [-h host] [-p port] [-v] [--] path {path}");
			System.err.println("\t-h\tWill connect to host instead of writing to standard out");
			System.err.println("\t-p\tWill connect to port instead of port "+PackProtocol.DEFAULT_PORT);
			System.err.println("\t-v\tWrite the full path to standard error as each file is packed");
			System.err.println("\t-z\tCompress the output");
			System.err.println("\t--\tEnd options, all additional arguments will be interpreted as paths");
			System.exit(1);
		} else {
			List<UnixFile> directories = new ArrayList<UnixFile>(args.length);
			PrintStream verboseOutput = null;
			boolean compress = false;
			String host = null;
			int port = PackProtocol.DEFAULT_PORT;
			boolean optionsEnded = false;
			for(int i=0; i<args.length; i++) {
				String arg = args[i];
				if(!optionsEnded && arg.equals("-v")) verboseOutput = System.err;
				else if(!optionsEnded && arg.equals("-h")) {
					i++;
					if(i<args.length) host = args[i];
					else throw new IllegalArgumentException("Expecting host after -h");
				} else if(!optionsEnded && arg.equals("-p")) {
					i++;
					if(i<args.length) port = Integer.parseInt(args[i]);
					else throw new IllegalArgumentException("Expecting port after -p");
				} else if(!optionsEnded && arg.equals("--")) optionsEnded = true;
				else if(!optionsEnded && arg.equals("-z")) compress = true;
				else directories.add(new UnixFile(arg));
			}
			try {
				if(host!=null) {
					Socket socket = new Socket(host, port);
					try {
						OutputStream out = socket.getOutputStream();
						try {
							InputStream in = socket.getInputStream();
							try {
								parallelPack(directories, out, verboseOutput, compress);
								int resp = in.read();
								if(resp==-1) throw new EOFException("End of file while reading completion confirmation");
								if(resp!=PackProtocol.END) throw new IOException("Unexpected value while reading completion confirmation");
							} finally {
								in.close();
							}
						} finally {
							out.close();
						}
					} finally {
						socket.close();
					}
				} else {
					// System.out
					parallelPack(directories, System.out, verboseOutput, compress);
				}
			} catch(IOException err) {
				err.printStackTrace(System.err);
				System.err.flush();
				System.exit(2);
			}
		}
	}

	static class LinkAndCount {
		final long linkId;
		int linkCount;
		LinkAndCount(long linkId, int linkCount) {
			this.linkId = linkId;
			this.linkCount = linkCount;
		}
	}

	static class FilesystemIteratorAndSlot {
		final FilesystemIterator iterator;
		final int slot;
		FilesystemIteratorAndSlot(FilesystemIterator iterator, int slot) {
			this.iterator = iterator;
			this.slot = slot;
		}
	}

	/**
	 * Packs to the provided output stream.  The stream is flushed and closed.
	 */
	public static void parallelPack(List<UnixFile> directories, OutputStream out, final PrintStream verboseOutput, boolean compress) throws IOException {
		// Reused throughout method
		final Stat stat = new Stat();
		final int numDirectories = directories.size();

		// The set of next files is kept in key order so that it can scale with O(n*log(n)) for larger numbers of directories
		// as opposed to O(n^2) for a list.  This is similar to the fix for AWStats logresolvemerge provided by Dan Armstrong
		// a couple of years ago.
		final Map<String,List<FilesystemIteratorAndSlot>> nextFiles = new TreeMap<String,List<FilesystemIteratorAndSlot>>(
			new Comparator<String>() {
				@Override
				public int compare(String S1, String S2) {
					// Make sure directories are sorted after their directory contents
					int diff = S1.compareTo(S2);
					if(diff==0) return 0;
					if(S2.startsWith(S1)) return 1;
					if(S1.startsWith(S2)) return -1;
					return diff;
				}
			}
		);
		{
			int nextSlot = 0;
			final Map<String,FilesystemIteratorRule> prefixRules = Collections.emptyMap();
			for(UnixFile directory : directories) {
				directory.getStat(stat);
				if(!stat.exists()) throw new IOException("Directory not found: "+directory.getPath());
				if(!stat.isDirectory()) throw new IOException("Not a directory: "+directory.getPath());
				String path = directory.getFile().getCanonicalPath();
				Map<String,FilesystemIteratorRule> rules = Collections.singletonMap(path, FilesystemIteratorRule.OK);
				FilesystemIterator iterator = new FilesystemIterator(rules, prefixRules, path, true, true);
				File nextFile = iterator.getNextFile();
				if(nextFile!=null) {
					String relPath = getRelativePath(nextFile, iterator);
					List<FilesystemIteratorAndSlot> list = nextFiles.get(relPath);
					if(list==null) nextFiles.put(relPath, list = new ArrayList<FilesystemIteratorAndSlot>(numDirectories));
					list.add(new FilesystemIteratorAndSlot(iterator, nextSlot++));
					if(nextSlot>62) nextSlot = 0;
				}
			}
		}

		final BlockingQueue<String> verboseQueue;
		final boolean[] verboseThreadRun;
		Thread verboseThread;
		if(verboseOutput==null) {
			verboseQueue = null;
			verboseThreadRun = null;
			verboseThread = null;
		} else {
			verboseQueue = new ArrayBlockingQueue<String>(VERBOSE_QUEUE_SIZE);
			verboseThreadRun = new boolean[] {true};
			verboseThread = new Thread("ParallelPack - Verbose Thread") {
				@Override
				public void run() {
					while(true) {
						synchronized(verboseThreadRun) {
							if(!verboseThreadRun[0] && verboseQueue.isEmpty()) break;
						}
						try {
							verboseOutput.println(verboseQueue.take());
							if(verboseQueue.isEmpty()) verboseOutput.flush();
						} catch(InterruptedException err) {
							// Normal during thread shutdown
						}
					}
				}
			};

			verboseThread.start();
		}
		try {
			// Hard link management
			long nextLinkId = 1; // LinkID of 0 is reserved for no link
			// This is a mapping from device->inode->linkId
			Map<Long,Map<Long,LinkAndCount>> deviceInodeIdMap = new HashMap<Long,Map<Long,LinkAndCount>>();

			CompressedDataOutputStream compressedOut = new CompressedDataOutputStream(out);
			try {
				// Header
				for(int c=0, len=PackProtocol.HEADER.length(); c<len; c++) compressedOut.write(PackProtocol.HEADER.charAt(c));
				// Version
				compressedOut.writeInt(PackProtocol.VERSION);
				compressedOut.writeBoolean(compress);
				if(compress) compressedOut = new CompressedDataOutputStream(new GZIPOutputStream(out, PackProtocol.BUFFER_SIZE));
				// Reused in main loop
				final StringBuilder SB = new StringBuilder();
				final byte[] buffer = PackProtocol.BUFFER_SIZE==BufferManager.BUFFER_SIZE ? BufferManager.getBytes() : new byte[PackProtocol.BUFFER_SIZE];
				try {
					// Main loop, continue until nextFiles is empty
					while(true) {
						Iterator<String> iter = nextFiles.keySet().iterator();
						if(!iter.hasNext()) break;
						String relPath = iter.next();
						for(FilesystemIteratorAndSlot iteratorAndSlot : nextFiles.remove(relPath)) {
							FilesystemIterator iterator = iteratorAndSlot.iterator;
							// Get the full path on this machine
							SB.setLength(0);
							String startPath = iterator.getStartPath();
							SB.append(startPath);
							SB.append(relPath);
							String fullPath = SB.toString();
							UnixFile uf = new UnixFile(fullPath);
							// Get the pack path
							SB.setLength(0);
							int lastSlashPos = startPath.lastIndexOf(File.separatorChar);
							if(lastSlashPos==-1) SB.append(startPath);
							else SB.append(startPath, lastSlashPos, startPath.length());
							SB.append(relPath);
							String packPath = SB.toString();
							// Verbose output
							if(verboseQueue!=null) {
								try {
									verboseQueue.put(packPath);
								} catch(InterruptedException err) {
									IOException ioErr = new InterruptedIOException();
									ioErr.initCause(err);
									throw ioErr;
								}
							}

							// Handle this file
							uf.getStat(stat);
							if(stat.isRegularFile()) {
								compressedOut.writeByte(PackProtocol.REGULAR_FILE);
								compressedOut.writeCompressedUTF(packPath, iteratorAndSlot.slot);
								int numLinks = stat.getNumberLinks();
								if(numLinks==1) {
									// No hard links
									compressedOut.writeLong(0);
									compressedOut.writeInt(stat.getUid());
									compressedOut.writeInt(stat.getGid());
									compressedOut.writeLong(stat.getMode());
									compressedOut.writeLong(stat.getModifyTime());
									writeFile(uf, compressedOut, buffer);
								} else if(numLinks>1) {
									// Has hard links
									// Look for already found
									Long device = stat.getDevice();
									Long inode = stat.getInode();
									Map<Long,LinkAndCount> inodeMap = deviceInodeIdMap.get(device);
									if(inodeMap==null) deviceInodeIdMap.put(device, inodeMap = new HashMap<Long, LinkAndCount>());
									LinkAndCount linkAndCount = inodeMap.get(inode);
									if(linkAndCount!=null) {
										// Already sent, send the link ID and decrement our count
										compressedOut.writeLong(linkAndCount.linkId);
										if(--linkAndCount.linkCount<=0) {
											inodeMap.remove(inode);
											// This keeps memory tighter but can increase overhead by making many new maps:
											// if(inodeMap.isEmpty()) deviceInodeIdMap.remove(device);
										}
									} else {
										// New file, send file data
										long linkId = nextLinkId++;
										compressedOut.writeLong(linkId);
										compressedOut.writeInt(stat.getUid());
										compressedOut.writeInt(stat.getGid());
										compressedOut.writeLong(stat.getMode());
										compressedOut.writeLong(stat.getModifyTime());
										compressedOut.writeInt(numLinks);
										writeFile(uf, compressedOut, buffer);
										inodeMap.put(inode, new LinkAndCount(linkId, numLinks-1));
									}
								} else throw new IOException("Invalid link count: "+numLinks);
							} else if(stat.isDirectory()) {
								compressedOut.writeByte(PackProtocol.DIRECTORY);
								compressedOut.writeCompressedUTF(packPath, iteratorAndSlot.slot);
								compressedOut.writeInt(stat.getUid());
								compressedOut.writeInt(stat.getGid());
								compressedOut.writeLong(stat.getMode());
								compressedOut.writeLong(stat.getModifyTime());
							} else if(stat.isSymLink()) {
								compressedOut.writeByte(PackProtocol.SYMLINK);
								compressedOut.writeCompressedUTF(packPath, iteratorAndSlot.slot);
								compressedOut.writeInt(stat.getUid());
								compressedOut.writeInt(stat.getGid());
								compressedOut.writeCompressedUTF(uf.readLink(), 63);
							} else if(stat.isBlockDevice()) {
								compressedOut.writeByte(PackProtocol.BLOCK_DEVICE);
								compressedOut.writeCompressedUTF(packPath, iteratorAndSlot.slot);
								compressedOut.writeInt(stat.getUid());
								compressedOut.writeInt(stat.getGid());
								compressedOut.writeLong(stat.getMode());
								compressedOut.writeLong(stat.getDeviceIdentifier());
							} else if(stat.isCharacterDevice()) {
								compressedOut.writeByte(PackProtocol.CHARACTER_DEVICE);
								compressedOut.writeCompressedUTF(packPath, iteratorAndSlot.slot);
								compressedOut.writeInt(stat.getUid());
								compressedOut.writeInt(stat.getGid());
								compressedOut.writeLong(stat.getMode());
								compressedOut.writeLong(stat.getDeviceIdentifier());
							} else if(stat.isFifo()) {
								compressedOut.writeByte(PackProtocol.FIFO);
								compressedOut.writeCompressedUTF(packPath, iteratorAndSlot.slot);
								compressedOut.writeInt(stat.getUid());
								compressedOut.writeInt(stat.getGid());
								compressedOut.writeLong(stat.getMode());
							} else if(stat.isSocket()) {
								throw new IOException("Unable to pack socket: "+uf.getPath());
							}
							// Get the next file
							File nextFile = iterator.getNextFile();
							if(nextFile!=null) {
								String newRelPath = getRelativePath(nextFile, iterator);
								List<FilesystemIteratorAndSlot> list = nextFiles.get(newRelPath);
								if(list==null) nextFiles.put(newRelPath, list = new ArrayList<FilesystemIteratorAndSlot>(numDirectories));
								list.add(iteratorAndSlot);
							}
						}
					}
				} finally {
					if(PackProtocol.BUFFER_SIZE==BufferManager.BUFFER_SIZE) BufferManager.release(buffer, false);
				}
				compressedOut.writeByte(PackProtocol.END);
			} finally {
				compressedOut.flush();
				compressedOut.close();
			}
			// TODO: If verbose, warn for any hard links that didn't all get packed
		} finally {
			// Wait for verbose queue to be empty
			if(verboseThread!=null) {
				synchronized(verboseThreadRun) {
					verboseThreadRun[0] = false;
				}
				verboseThread.interrupt();
				try {
					verboseThread.join();
				} catch(InterruptedException err) {
					IOException ioErr = new InterruptedIOException();
					ioErr.initCause(err);
					throw ioErr;
				}
			}
		}
	}

	private static void writeFile(UnixFile uf, DataOutput out, byte[] buffer) throws IOException {
		InputStream in = new FileInputStream(uf.getFile());
		try {
			int ret;
			while((ret=in.read(buffer, 0, PackProtocol.BUFFER_SIZE))!=-1) {
				if(ret<0 || ret>Short.MAX_VALUE) throw new IOException("ret out of range: "+ret);
				out.writeShort(ret);
				out.write(buffer, 0, ret);
			}
			out.writeShort(-1);
		} finally {
			in.close();
		}
	}

	/**
	 * Gets the relative path for the provided file from the provided iterator.
	 */
	private static String getRelativePath(File file, FilesystemIterator iterator) throws IOException {
		String path = file.getPath();
		String prefix = iterator.getStartPath();
		if(!path.startsWith(prefix)) throw new IOException("path doesn't start with prefix: path=\""+path+"\", prefix=\""+prefix+"\"");
		return path.substring(prefix.length());
	}
}
