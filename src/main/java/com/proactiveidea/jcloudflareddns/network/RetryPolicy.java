/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns.network;

import java.time.Duration;

/** Immutable bounds for public IP provider retries within one CLI execution. */
public record RetryPolicy(int maxTotalAttempts, int maxAttemptsPerProvider, Duration retryDelay) {

    public RetryPolicy {
        if (maxTotalAttempts < 1) {
            throw new IllegalArgumentException("maxTotalAttempts must be positive.");
        }
        if (maxAttemptsPerProvider < 1 || maxAttemptsPerProvider > maxTotalAttempts) {
            throw new IllegalArgumentException(
                    "maxAttemptsPerProvider must be positive and no greater than maxTotalAttempts.");
        }
        if (retryDelay == null || retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must not be negative.");
        }
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(3, 2, Duration.ofMillis(250));
    }
}
