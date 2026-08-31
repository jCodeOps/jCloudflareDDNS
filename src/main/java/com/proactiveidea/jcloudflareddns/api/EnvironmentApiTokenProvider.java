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

import java.util.Objects;

/** Reads a token from one named environment variable. */
public final class EnvironmentApiTokenProvider implements ApiTokenProvider {

    private final String environmentVariable;

    public EnvironmentApiTokenProvider(String environmentVariable) {
        this.environmentVariable = Objects.requireNonNull(environmentVariable, "environmentVariable");
    }

    @Override
    public char[] token() throws AuthenticationException {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            throw new AuthenticationException(
                    "Cloudflare API Token environment variable is missing or empty.");
        }
        return value.toCharArray();
    }
}
