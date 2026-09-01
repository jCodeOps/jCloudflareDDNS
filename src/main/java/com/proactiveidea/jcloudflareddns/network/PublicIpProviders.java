/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns.network;

import java.net.URI;
import java.util.List;

/** Built-in public IP endpoints, selected for the requested address family. */
public final class PublicIpProviders {

    private static final List<URI> IPV4 = List.of(
            URI.create("https://api.ipify.org"),
            URI.create("https://ipv4.icanhazip.com"));
    private static final List<URI> IPV6 = List.of(
            URI.create("https://api6.ipify.org"),
            URI.create("https://ipv6.icanhazip.com"));

    private PublicIpProviders() {
    }

    public static List<URI> defaults(IpVersion version) {
        return version == IpVersion.IPV4 ? IPV4 : IPV6;
    }

    public static List<URI> select(List<String> configuredUrls, boolean useDefaults, IpVersion version) {
        List<URI> providers = new java.util.ArrayList<>();
        if (useDefaults) {
            providers.addAll(defaults(version));
        }
        configuredUrls.stream().map(URI::create).forEach(providers::add);
        return List.copyOf(providers);
    }
}
