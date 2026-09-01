/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns.api;

import java.time.Duration;
import java.util.Objects;

/** Immutable retry bounds for read-only Cloudflare API requests. */
public record CloudflareRetryPolicy(int maxAttempts, Duration initialDelay) {

    public CloudflareRetryPolicy {
        if (maxAttempts < 1 || maxAttempts > 5) {
            throw new IllegalArgumentException("maxAttempts must be between 1 and 5.");
        }
        Objects.requireNonNull(initialDelay, "initialDelay");
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay must not be negative.");
        }
    }

    /** Returns the exponential delay for a retry number starting at one. */
    public Duration delayForRetry(int retryNumber) {
        if (retryNumber < 1 || retryNumber >= maxAttempts) {
            throw new IllegalArgumentException("retryNumber must identify a pending retry.");
        }
        return initialDelay.multipliedBy(1L << (retryNumber - 1));
    }

    public static CloudflareRetryPolicy defaults() {
        return new CloudflareRetryPolicy(3, Duration.ofMillis(250));
    }
}
