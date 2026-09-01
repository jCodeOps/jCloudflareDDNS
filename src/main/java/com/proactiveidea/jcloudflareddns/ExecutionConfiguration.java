/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

/** Non-secret execution settings for multi-profile runs. */
public record ExecutionConfiguration(String mode, Integer maxConcurrency) {

    public static final int RECOMMENDED_MAX_CONCURRENCY = 8;
    public static final int ABSOLUTE_MAX_CONCURRENCY = 16;

    public ExecutionConfiguration {
        mode = mode == null || mode.isBlank() ? "sequential" : mode.trim().toLowerCase();
    }

    public int workerCount() {
        return mode.equals("sequential") ? 1 : maxConcurrency == null ? 2 : maxConcurrency;
    }

    public boolean exceedsRecommendedConcurrency() {
        return mode.equals("parallel") && workerCount() > RECOMMENDED_MAX_CONCURRENCY;
    }
}
