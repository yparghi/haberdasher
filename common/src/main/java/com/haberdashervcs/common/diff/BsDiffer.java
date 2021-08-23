package com.haberdashervcs.common.diff;

import java.io.ByteArrayOutputStream;

import io.sigpipe.jbsdiff.Diff;


public final class BsDiffer {

    private BsDiffer() {}  // Do not instantiate


    /**
     * @return The bytes of a bzip2-compressed bsdiff patch
     */
    // TODO Do I really want to bzip2 compress the output? (that's the default in jbsdiff)
    // TODO: Streams, for large files
    public static byte[] diff(byte[] oldBytes, byte[] newBytes) throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        Diff.diff(oldBytes, newBytes, byteOut);
        return byteOut.toByteArray();
    }
}
