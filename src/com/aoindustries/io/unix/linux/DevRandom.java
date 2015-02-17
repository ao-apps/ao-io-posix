/*
 * aocode-public - Reusable Java library of general tools with minimal external dependencies.
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011  AO Industries, Inc.
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
package com.aoindustries.io.unix.linux;

import com.aoindustries.io.unix.UnixFile;
import com.aoindustries.util.ErrorPrinter;
import com.aoindustries.util.WrappedException;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * This class acts as a direct source of Random data from <code>/dev/random</code> on Linux
 * platforms.  Many of these calls are platform specific and use the included <code>libaocode.so</code>
 * Linux shared library provided as a resource.  The source code is also supplied.
 * Please note that reading will block if random data is not available.  Use only
 * where the highest quality random data is required and possible delays are acceptable.
 *
 * @author  AO Industries, Inc.
 */
public class DevRandom extends Random {

    private static final long serialVersionUID = 4190090095484650210L;

    /**
     * The device file path used to obtain and add random data.
     */
    public static final String DEV_RANDOM_PATH="/dev/random";
    
    /**
     * The device file used to obtain and add random data.
     */
    public static final UnixFile devRandomUF=new UnixFile(DEV_RANDOM_PATH);

    /**
     * The access to the device file.
     */
    private static FileInputStream devRandomIn;
    private static final Object devRandomInLock=new Object();

    /**
     * Opens the <code>FileInputStream</code> that reads from <code>/dev/random</code>.
     */
    public static FileInputStream openDevRandomIn() throws IOException {
        synchronized(devRandomInLock) {
            if(devRandomIn==null) devRandomIn=new FileInputStream(devRandomUF.getFile());
            return devRandomIn;
        }
    }
    
    /**
     * Closes the <code>FileInputStream</code> that reads from <code>/dev/random</code>.
     */
    public static void closeDevRandomIn() throws IOException {
        synchronized(devRandomInLock) {
            FileInputStream in=devRandomIn;
            if(in!=null) {
                devRandomIn=null;
                in.close();
            }
        }
    }

    /**
     * The device file path used to obtain the entropy statistics.
     */
    public static final String ENTROPY_AVAIL_PATH="/proc/sys/kernel/random/entropy_avail";
    
    /**
     * The device file used to obtain the entropy statistics.
     */
    public static final UnixFile entropyAvailUF=new UnixFile(ENTROPY_AVAIL_PATH);

    /**
     * Gets the number of random bits currently available in the kernel.
     */
    public static int getEntropyAvail() throws IOException {
        BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(entropyAvailUF.getFile())));
        try {
            String line=in.readLine();
            if(line==null) throw new EOFException("EOF when reading from "+ENTROPY_AVAIL_PATH);
            try {
                return Integer.parseInt(line.trim());
            } catch(NumberFormatException err) {
                IOException ioErr=new IOException("Unable to parse the output of "+ENTROPY_AVAIL_PATH+": "+line);
                ioErr.initCause(err);
                throw ioErr;
            }
        } finally {
            in.close();
        }
    }

    /**
     * The device file path used to obtain the pool size.
     */
    public static final String POOL_SIZE_PATH="/proc/sys/kernel/random/poolsize";
    
    /**
     * The device file used to obtain the pool size.
     */
    public static final UnixFile poolSizeUF=new UnixFile(POOL_SIZE_PATH);

    /**
     * Gets the number of bits in the random pool in the kernel.
     */
    public static int getPoolSize() throws IOException {
        BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(poolSizeUF.getFile())));
        try {
            String line=in.readLine();
            if(line==null) throw new EOFException("EOF when reading from "+POOL_SIZE_PATH);
            try {
                return Integer.parseInt(line.trim())*8;
            } catch(NumberFormatException err) {
                IOException ioErr=new IOException("Unable to parse the output of "+POOL_SIZE_PATH+": "+line);
                ioErr.initCause(err);
                throw ioErr;
            }
        } finally {
            in.close();
        }
    }

    /**
     * Entropy addition is serialized.
     */
    private static final Object addEntropyLock=new Object();
    
    /**
     * Adds random entropy to the kernel.
     */
    public static void addEntropy(byte[] randomData) throws IOException {
        SecurityManager security=System.getSecurityManager();
        if(security!=null) security.checkRead(DEV_RANDOM_PATH);
        UnixFile.loadLibrary();
        synchronized(addEntropyLock) {
            addEntropy0(randomData);
        }
    }

    private static native void addEntropy0(byte[] randomData) throws IOException;

    public DevRandom() {
    }

    /**
     * This class does not use this seed value.
     */
    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);
    }

    @Override
    protected int next(int bits) {
        try {
            int result=0;
            if(bits>=8) {
                FileInputStream in=openDevRandomIn();
                while(bits>=8) {
                    int next=in.read();
                    if(next==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
                    result=(result<<8)|next;
                    bits-=8;
                }
            }
            while(bits>=1) {
                result=(result<<1)|(nextBoolean0()?1:0);
                bits--;
            }
            return result;
        } catch(IOException err) {
            try {
                closeDevRandomIn();
            } catch(IOException err2) {
                // Ignore since we already have an exception we are throwing
            }
            throw new WrappedException(err);
        }
    }

    @Override
    public boolean nextBoolean() {
        try {
            return nextBoolean0();
        } catch(IOException err) {
            try {
                closeDevRandomIn();
            } catch(IOException err2) {
                // Ignore since we already have an exception we are throwing
            }
            throw new WrappedException(err);
        }
    }

    /**
     * The extra bits read from the random source are stored here temporarily.
     */
    private int extraBits=0;
    private int numExtraBits=0;
    private final Object extraBitsLock=new Object();

    private boolean nextBoolean0() throws IOException {
        synchronized(extraBitsLock) {
            if(numExtraBits<=0) {
                FileInputStream in=openDevRandomIn();
                int next=in.read();
                if(next==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
                extraBits=next;
                numExtraBits=8;
            }
            boolean result=(extraBits&1)!=0;
            extraBits>>>=1;
            numExtraBits--;
            return result;
        }
    }

    @Override
    public void nextBytes(byte[] bytes) {
        nextBytesStatic(bytes, 0, bytes.length);
    }

    public void nextBytes(byte[] bytes, int off, int len) {
        nextBytesStatic(bytes, off, len);
    }

    public static void nextBytesStatic(byte[] bytes) {
        nextBytesStatic(bytes, 0, bytes.length);
    }

    public static void nextBytesStatic(byte[] bytes, int off, int len) {
        try {
            if(len>0) {
                FileInputStream in=openDevRandomIn();
                while(len>0) {
                    int ret=in.read(bytes, off, len);
                    if(ret==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
                    off+=ret;
                    len-=ret;
                }
            }
        } catch(IOException err) {
            try {
                closeDevRandomIn();
            } catch(IOException err2) {
                // Ignore since we already have an exception we are throwing
            }
            throw new WrappedException(err);
        }
    }

    @Override
    public int nextInt() {
        try {
            FileInputStream in=openDevRandomIn();
            int b1=in.read();
            if(b1==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            int b2=in.read();
            if(b2==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            int b3=in.read();
            if(b3==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            int b4=in.read();
            if(b4==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            return
                (b1<<24)
                | (b2<<16)
                | (b3<<8)
                | b4
            ;
        } catch(IOException err) {
            try {
                closeDevRandomIn();
            } catch(IOException err2) {
                // Ignore since we already have an exception we are throwing
            }
            throw new WrappedException(err);
        }
    }

    @Override
    public long nextLong() {
        try {
            FileInputStream in=openDevRandomIn();
            long b1=in.read();
            if(b1==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            long b2=in.read();
            if(b2==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            long b3=in.read();
            if(b3==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            long b4=in.read();
            if(b4==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            long b5=in.read();
            if(b5==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            long b6=in.read();
            if(b6==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            long b7=in.read();
            if(b7==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            long b8=in.read();
            if(b8==-1) throw new EOFException("EOF when reading from "+DEV_RANDOM_PATH);
            return
                (b1<<56)
                | (b2<<48)
                | (b3<<40)
                | (b4<<32)
                | (b5<<24)
                | (b6<<16)
                | (b7<<8)
                | b8
            ;
        } catch(IOException err) {
            try {
                closeDevRandomIn();
            } catch(IOException err2) {
                // Ignore since we already have an exception we are throwing
            }
            throw new WrappedException(err);
        }
    }
    
    /**
     * Manually adds entropy to the kernel, reads from standard in.
     */
    public static void main(String[] args) {
        try {
            byte[] buff=new byte[16];
            int ret;
            while((ret=System.in.read(buff, 0, 16))!=-1) {
                if(ret!=16) {
                    byte[] newBuff=new byte[ret];
                    System.arraycopy(buff, 0, newBuff, 0, ret);
                    DevRandom.addEntropy(newBuff);
                } else DevRandom.addEntropy(buff);
            }
        } catch(IOException err) {
            ErrorPrinter.printStackTraces(err);
        }
    }
}