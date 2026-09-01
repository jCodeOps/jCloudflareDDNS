/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PublicIpProvidersTest {

    @Test
    void suppliesTwoDefaultsForEachAddressFamily() {
        assertEquals(2, PublicIpProviders.defaults(IpVersion.IPV4).size());
        assertEquals(2, PublicIpProviders.defaults(IpVersion.IPV6).size());
    }

    @Test
    void appendsConfiguredProvidersAfterDefaults() {
        List<java.net.URI> providers = PublicIpProviders.select(
                List.of("https://custom.example/ip"), true, IpVersion.IPV4);

        assertEquals("https://custom.example/ip", providers.getLast().toString());
        assertEquals(3, providers.size());
    }

    @Test
    void canUseOnlyConfiguredProviders() {
        List<java.net.URI> providers = PublicIpProviders.select(
                List.of("https://custom.example/ip"), false, IpVersion.IPV6);

        assertEquals(List.of(java.net.URI.create("https://custom.example/ip")), providers);
    }
}
