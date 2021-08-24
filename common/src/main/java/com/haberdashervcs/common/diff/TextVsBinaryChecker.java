package com.haberdashervcs.common.diff;

import java.nio.charset.StandardCharsets;
import java.util.Optional;


public final class TextVsBinaryChecker {

    // TODO: Something more rigorous
    public static Optional<String> convertToString(byte[] contents) {
        try {
            String utf8 = new String(contents, StandardCharsets.UTF_8);
            return Optional.of(utf8);
        } catch (Exception ex) {
            // Continue
        }

        try {
            String utf16 = new String(contents, StandardCharsets.UTF_16);
            return Optional.of(utf16);
        } catch (Exception ex) {
            // Continue
        }

        return Optional.empty();
    }
}
