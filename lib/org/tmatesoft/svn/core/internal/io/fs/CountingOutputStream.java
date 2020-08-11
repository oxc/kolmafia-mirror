/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import org.tmatesoft.svn.core.internal.io.fs.index.FSFnv1aOutputStream;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class CountingOutputStream extends FilterOutputStream {

    private long myPosition;
    private FSFnv1aOutputStream myFnv1ChecksumOutputStream;

    public CountingOutputStream(OutputStream stream, long offset) {
        super(stream);
        myPosition = offset >= 0 ? offset : 0;
        myFnv1ChecksumOutputStream = new FSFnv1aOutputStream(SVNFileUtil.DUMMY_OUT);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        myFnv1ChecksumOutputStream.write(b, off, len);
        out.write(b, off, len);
        myPosition += len;
    }

    public void write(int b) throws IOException {
        myFnv1ChecksumOutputStream.write(b);
        out.write(b);
        myPosition++;
    }

    public void write(byte[] b) throws IOException {
        myFnv1ChecksumOutputStream.write(b);
        out.write(b);
        myPosition += b.length;
    }

    public void resetChecksum() {
        myFnv1ChecksumOutputStream.resetChecksum();
    }

    public int finalizeChecksum() {
        return myFnv1ChecksumOutputStream.finalizeChecksum();
    }

    public long getPosition() {
        return myPosition;
    }
}
