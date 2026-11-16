package com.pcr.dataserver;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

final class Digests {
    static String hash(String algorithm, byte[] value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(value)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static String sha256(String value) { return hash("SHA-256", value.getBytes(StandardCharsets.UTF_8)); }
}
