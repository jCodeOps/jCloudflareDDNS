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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.proactiveidea.jcloudflareddns.network.IpVersion;

/** Performs deterministic validation without reading secrets or using the network. */
public final class ConfigurationValidator {

    private static final int MAX_TTL = 86_400;
    private static final Pattern DNS_NAME = Pattern.compile(
            "(?=.{1,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}");
    private static final Pattern ENVIRONMENT_VARIABLE = Pattern.compile("[A-Z_][A-Z0-9_]*");

    public List<String> validate(Configuration configuration) {
        List<String> errors = new ArrayList<>();
        if (configuration == null) {
            errors.add("Configuration is required.");
            return List.copyOf(errors);
        }
        validateDomain("zone", configuration.zone(), errors);
        validateDomain("record", configuration.record(), errors);
        if (configuration.zone() != null && configuration.record() != null
                && !configuration.record().equals(configuration.zone())
                && !configuration.record().endsWith("." + configuration.zone())) {
            errors.add("record must belong to zone.");
        }
        if (configuration.ttl() == null) {
            errors.add("ttl is required.");
        } else if (configuration.ttl() < 1 || configuration.ttl() > MAX_TTL) {
            errors.add("ttl must be between 1 and 86400 seconds.");
        }
        if (configuration.tokenEnv() == null) {
            errors.add("tokenEnv is required.");
        } else if (!ENVIRONMENT_VARIABLE.matcher(configuration.tokenEnv()).matches()) {
            errors.add("tokenEnv must be a valid uppercase environment variable name.");
        }
        validateIpProviderUrl(configuration.ipProviderUrl(), errors);
        try {
            IpVersion.fromConfiguration(configuration.ipVersion());
        } catch (IllegalArgumentException exception) {
            errors.add("ipVersion must be either ipv4 or ipv6.");
        }
        return List.copyOf(errors);
    }

    private static void validateIpProviderUrl(String value, List<String> errors) {
        if (value == null) {
            errors.add("ipProviderUrl is required.");
            return;
        }
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                errors.add("ipProviderUrl must be an HTTPS URL without a query or fragment.");
            }
        } catch (IllegalArgumentException exception) {
            errors.add("ipProviderUrl must be a valid HTTPS URL.");
        }
    }

    private static void validateDomain(String field, String value, List<String> errors) {
        if (value == null) {
            errors.add(field + " is required.");
        } else if (!DNS_NAME.matcher(value).matches()) {
            errors.add(field + " must be a valid fully qualified domain name.");
        }
    }
}
