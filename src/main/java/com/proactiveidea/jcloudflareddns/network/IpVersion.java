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

package com.proactiveidea.jcloudflareddns.network;

/** Supported public IP and DNS record families. */
public enum IpVersion {
    IPV4("ipv4", "A"),
    IPV6("ipv6", "AAAA");

    private final String configurationName;
    private final String recordType;

    IpVersion(String configurationName, String recordType) {
        this.configurationName = configurationName;
        this.recordType = recordType;
    }

    public static IpVersion fromConfiguration(String value) {
        for (IpVersion version : values()) {
            if (version.configurationName.equalsIgnoreCase(value)) {
                return version;
            }
        }
        throw new IllegalArgumentException("Unsupported IP version.");
    }

    public String recordType() {
        return recordType;
    }
}
