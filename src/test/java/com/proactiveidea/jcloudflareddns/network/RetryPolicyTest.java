/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void exposesBoundedDefaults() {
        RetryPolicy policy = RetryPolicy.defaults();

        assertEquals(3, policy.maxTotalAttempts());
        assertEquals(2, policy.maxAttemptsPerProvider());
        assertEquals(Duration.ofMillis(250), policy.retryDelay());
    }

    @Test
    void rejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(0, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(2, 3, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(1, 1, Duration.ofMillis(-1)));
    }
}
