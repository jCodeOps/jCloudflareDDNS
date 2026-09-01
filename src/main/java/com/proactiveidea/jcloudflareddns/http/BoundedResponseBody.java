/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;

/** Reads a bounded HTTP response body without retaining an unbounded payload. */
public final class BoundedResponseBody {

    private BoundedResponseBody() {
    }

    /** Reads at most {@code maxBytes} bytes and decodes them using {@code charset}. */
    public static String read(InputStream input, int maxBytes, Charset charset) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(charset, "charset");
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes must be positive.");
        }
        byte[] bytes = input.readNBytes(maxBytes + 1);
        if (bytes.length > maxBytes) {
            throw new ResponseTooLargeException(maxBytes);
        }
        return new String(bytes, charset);
    }

    /** Indicates that an HTTP response exceeded the configured maximum size. */
    public static final class ResponseTooLargeException extends IOException {

        private static final long serialVersionUID = 1L;

        private ResponseTooLargeException(int maxBytes) {
            super("HTTP response exceeded " + maxBytes + " bytes.");
        }
    }
}
