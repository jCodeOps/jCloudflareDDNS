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

import java.util.List;

/** Immutable non-secret application configuration. */
public record Configuration(
        String zone,
        String record,
        Integer ttl,
        Boolean proxied,
        String tokenEnv,
        List<String> ipProviderUrls,
        Boolean useDefaultIpProviders,
        String ipVersion) {

    public Configuration {
        zone = trimToNull(zone);
        record = trimToNull(record);
        tokenEnv = trimToNull(tokenEnv);
        ipProviderUrls = ipProviderUrls == null ? List.of() : ipProviderUrls.stream()
                .map(Configuration::trimToNull)
                .filter(java.util.Objects::nonNull)
                .toList();
        useDefaultIpProviders = useDefaultIpProviders == null || useDefaultIpProviders;
        ipVersion = ipVersion == null || ipVersion.isBlank() ? "ipv4" : ipVersion.trim().toLowerCase();
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
