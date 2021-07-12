package com.haberdashervcs.common.diff;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;


// TODO: Configurable? OOP?
public final class HdHasher {

    private HdHasher() {}


    public static final class ContentsAndHash {
        private final byte[] contents;
        private final byte[] hash;

        private ContentsAndHash(byte[] contents, byte[] hash) {
            this.contents = contents;
            this.hash = hash;
        }

        public byte[] getContents() {
            return contents;
        }

        public byte[] getHash() {
            return hash;
        }

        // TODO! This produces a long string... is it right??
        public String hashString() {
            return String.format("%064x", new BigInteger(1, hash));
        }
    }


    public static ContentsAndHash readLocalFile(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream dis = new DigestInputStream(Files.newInputStream(path), digest);
            byte[] contents = dis.readAllBytes();
            byte[] hash = digest.digest();
            return new ContentsAndHash(contents, hash);

        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public static ContentsAndHash readBytes(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return new ContentsAndHash(bytes, hash);

        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
}
