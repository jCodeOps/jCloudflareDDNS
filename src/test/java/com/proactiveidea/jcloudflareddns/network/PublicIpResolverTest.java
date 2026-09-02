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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class PublicIpResolverTest {

    private HttpServer server;
    private int responseStatus;
    private String responseBody;
    private String fallbackResponseBody;
    private int responseCount;

    @BeforeEach
    void startServer() throws IOException {
        responseStatus = 200;
        responseBody = "198.51.100.10\n";
        fallbackResponseBody = "203.0.113.20\n";
        responseCount = 0;
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ip", this::respond);
        server.createContext("/fallback", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void resolvesAndTrimsAValidIpv4Response() throws Exception {
        IpAddress address = resolver().resolve();

        assertEquals("198.51.100.10", address.value());
    }

    @Test
    void resolvesAndNormalizesAValidIpv6Response() throws Exception {
        responseBody = "2001:0db8:0:0:0:0:0:1\n";

        IpAddress address = new PublicIpResolver(
                java.net.http.HttpClient.newHttpClient(),
                java.net.URI.create("http://localhost:" + server.getAddress().getPort() + "/ip"),
                IpVersion.IPV6).resolve();

        assertEquals("2001:db8::1", address.value());
    }

    @Test
    void rejectsAnInvalidIpv4Response() {
        responseBody = "not-an-ip\n";

        assertThrows(PublicIpException.class, () -> resolver().resolve());
    }

    @Test
    void rejectsAnUnsuccessfulProviderResponse() {
        responseStatus = 503;

        assertThrows(PublicIpException.class, () -> resolver().resolve());
    }

    @Test
    void retriesTransientProviderFailuresWithinTheBound() {
        responseStatus = 503;

        assertThrows(PublicIpException.class, () -> resolver().resolve());
        assertEquals(2, responseCount);
    }

    @Test
    void fallsBackToTheNextProviderAfterAInvalidResponse() throws Exception {
        responseBody = "not-an-ip\n";
        IpAddress address = new PublicIpResolver(
                java.net.http.HttpClient.newHttpClient(),
                List.of(
                        URI.create("http://localhost:" + server.getAddress().getPort() + "/ip"),
                        URI.create("http://localhost:" + server.getAddress().getPort() + "/fallback")),
                IpVersion.IPV4).resolve();

        assertEquals("203.0.113.20", address.value());
        assertEquals(2, responseCount);
    }

    @Test
    void fallsBackAfterAnOversizedProviderResponse() throws Exception {
        responseBody = "x".repeat(1_025);
        IpAddress address = new PublicIpResolver(
                java.net.http.HttpClient.newHttpClient(),
                List.of(
                        URI.create("http://localhost:" + server.getAddress().getPort() + "/ip"),
                        URI.create("http://localhost:" + server.getAddress().getPort() + "/fallback")),
                IpVersion.IPV4).resolve();

        assertEquals("203.0.113.20", address.value());
        assertEquals(2, responseCount);
    }

    @Test
    void validatesIpv4WithoutDnsResolution() {
        assertEquals("0.0.0.0", IpAddress.parse("0.0.0.0").value());
        assertThrows(IllegalArgumentException.class, () -> IpAddress.parse("01.2.3.4"));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.parse("-1.2.3.4"));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.parse("256.2.3.4"));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.parse("2001:db8::1"));
        assertEquals("2001:db8::1", IpAddress.parse("2001:0DB8:0:0:0:0:0:1", IpVersion.IPV6).value());
        assertThrows(IllegalArgumentException.class,
                () -> IpAddress.parse("2001:db8:0:0:0:0:0", IpVersion.IPV6));
    }

    private PublicIpResolver resolver() {
        return new PublicIpResolver(java.net.http.HttpClient.newHttpClient(),
                java.net.URI.create("http://localhost:" + server.getAddress().getPort() + "/ip"));
    }

    private void respond(HttpExchange exchange) throws IOException {
        responseCount++;
        String body = exchange.getHttpContext().getPath().equals("/fallback")
                ? fallbackResponseBody : responseBody;
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
