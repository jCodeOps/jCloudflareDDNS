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

package com.proactiveidea.jcloudflareddns;

/** Immutable non-secret application configuration. */
public record Configuration(
        String zone,
        String record,
        Integer ttl,
        Boolean proxied,
        String tokenEnv) {

    public Configuration {
        zone = trimToNull(zone);
        record = trimToNull(record);
        tokenEnv = trimToNull(tokenEnv);
        proxied = proxied == null ? Boolean.FALSE : proxied;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
