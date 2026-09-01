/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

/** Non-secret execution settings for future multi-profile runs. */
public record ExecutionConfiguration(String mode, Integer maxConcurrency) {

    public ExecutionConfiguration {
        mode = mode == null || mode.isBlank() ? "sequential" : mode.trim().toLowerCase();
    }
}
