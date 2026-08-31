/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.proactiveidea.jcloudflareddns.api;

/** Raised for an HTTP, transport, or unsuccessful Cloudflare API response. */
public final class CloudflareApiException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public CloudflareApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CloudflareApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
