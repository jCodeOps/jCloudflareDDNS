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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class CloudflareHttpClientTest {

    private HttpServer server;
    private AtomicReference<String> authorization;
    private AtomicReference<String> requestBody;

    @BeforeEach
    void startServer() throws IOException {
        authorization = new AtomicReference<>();
        requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/client/v4", this::handleRequest);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void findsZoneWithBearerTokenAndQueryParameter() throws Exception {
        CloudflareApiClient client = client();

        Zone zone = client.findZone("example.com");

        assertEquals("zone-id", zone.id());
        assertEquals("example.com", zone.name());
        assertEquals("active", zone.status());
        assertEquals("Bearer test-token", authorization.get());
    }

    @Test
    void findsZonesWithoutDnsCaseSensitivity() throws Exception {
        Zone zone = client().findZone("EXAMPLE.COM");

        assertEquals("zone-id", zone.id());
    }

    @Test
    void listsRecordsUsingTheRequestedFilters() throws Exception {
        CloudflareApiClient client = client();

        var records = client.listRecords("zone-id", "host.example.com", "A");

        assertEquals(1, records.size());
        assertEquals("record-id", records.getFirst().id());
        assertEquals("198.51.100.10", records.getFirst().content());
    }

    @Test
    void findsAnExactZoneAcrossPages() throws Exception {
        List<String> requestedPages = new ArrayList<>();
        server.removeContext("/client/v4");
        server.createContext("/client/v4", exchange -> {
            requestedPages.add(exchange.getRequestURI().getQuery());
            if (exchange.getRequestURI().getQuery().contains("page=1")) {
                respond(exchange, 200, """
                        {"success":true,"result":[{"id":"other-zone","name":"other.example","status":"active"}],
                        "result_info":{"page":1,"total_pages":2}}
                        """);
            } else {
                respond(exchange, 200, """
                        {"success":true,"result":[{"id":"zone-id","name":"example.com","status":"active"}],
                        "result_info":{"page":2,"total_pages":2}}
                        """);
            }
        });

        Zone zone = client().findZone("example.com");

        assertEquals("zone-id", zone.id());
        assertEquals(2, requestedPages.size());
        assertTrue(requestedPages.getFirst().contains("page=1"));
        assertTrue(requestedPages.getLast().contains("page=2"));
    }

    @Test
    void listsMatchingRecordsAcrossPages() throws Exception {
        List<String> requestedPages = new ArrayList<>();
        server.removeContext("/client/v4");
        server.createContext("/client/v4", exchange -> {
            requestedPages.add(exchange.getRequestURI().getQuery());
            if (exchange.getRequestURI().getQuery().contains("page=1")) {
                respond(exchange, 200, """
                        {"success":true,"result":[{"id":"record-first","name":"host.example.com","type":"A","content":"198.51.100.10","ttl":300,"proxied":false}],
                        "result_info":{"page":1,"total_pages":2}}
                        """);
            } else {
                respond(exchange, 200, """
                        {"success":true,"result":[{"id":"record-second","name":"host.example.com","type":"A","content":"198.51.100.11","ttl":300,"proxied":false}],
                        "result_info":{"page":2,"total_pages":2}}
                        """);
            }
        });

        List<DnsRecord> records = client().listRecords("zone-id", "host.example.com", "A");

        assertEquals(List.of("record-first", "record-second"),
                records.stream().map(DnsRecord::id).toList());
        assertEquals(2, requestedPages.size());
        assertTrue(requestedPages.getFirst().contains("page=1"));
        assertTrue(requestedPages.getLast().contains("page=2"));
    }

    @Test
    void updatesAnExistingRecordWithJsonPayload() throws Exception {
        CloudflareApiClient client = client();

        DnsRecord record = client.updateRecord(
                "zone-id",
                "record-id",
                new DnsRecordUpdate("host.example.com", "A", "198.51.100.11", 300, false));

        assertEquals("198.51.100.11", record.content());
        assertTrue(requestBody.get().contains("\"content\":\"198.51.100.11\""));
        assertEquals("Bearer test-token", authorization.get());
    }

    @Test
    void doesNotIncludeResponseBodyInApiExceptions() throws Exception {
        server.removeContext("/client/v4");
        server.createContext("/client/v4", exchange -> respond(
                exchange, 403, "{\"success\":false,\"errors\":[{\"message\":\"secret response\"}]}"));

        CloudflareApiException exception = assertThrows(
                CloudflareApiException.class,
                () -> client().findZone("example.com"));

        assertEquals(403, exception.statusCode());
        assertTrue(!exception.getMessage().contains("secret response"));
    }

    @Test
    void rejectsAnOversizedResponseWithoutIncludingItsBody() throws Exception {
        server.removeContext("/client/v4");
        String oversizedValue = "x".repeat(1_048_577);
        server.createContext("/client/v4", exchange -> respond(
                exchange, 200, "{\"success\":true,\"result\":\"" + oversizedValue + "\"}"));

        CloudflareApiException exception = assertThrows(
                CloudflareApiException.class,
                () -> client().findZone("example.com"));

        assertTrue(exception.getMessage().contains("exceeded the maximum size"));
        assertTrue(!exception.getMessage().contains(oversizedValue));
    }

    @Test
    void retriesTransientGetResponses() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.removeContext("/client/v4");
        server.createContext("/client/v4", exchange -> {
            if (requests.incrementAndGet() < 3) {
                respond(exchange, 503, "{\"success\":false,\"errors\":[]}");
            } else {
                respond(exchange, 200,
                        "{\"success\":true,\"result\":[{\"id\":\"zone-id\",\"name\":\"example.com\",\"status\":\"active\"}]}");
            }
        });

        Zone zone = client(new CloudflareRetryPolicy(3, java.time.Duration.ZERO)).findZone("example.com");

        assertEquals("zone-id", zone.id());
        assertEquals(3, requests.get());
    }

    @Test
    void retriesRateLimitedGetResponsesUsingRetryAfter() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.removeContext("/client/v4");
        server.createContext("/client/v4", exchange -> {
            if (requests.incrementAndGet() == 1) {
                exchange.getResponseHeaders().add("Retry-After", "0");
                respond(exchange, 429, "{\"success\":false,\"errors\":[]}");
            } else {
                respond(exchange, 200,
                        "{\"success\":true,\"result\":[{\"id\":\"zone-id\",\"name\":\"example.com\",\"status\":\"active\"}]}");
            }
        });

        Zone zone = client(new CloudflareRetryPolicy(2, java.time.Duration.ZERO)).findZone("example.com");

        assertEquals("zone-id", zone.id());
        assertEquals(2, requests.get());
    }

    @Test
    void defersLongRateLimitDelaysToTheNextExecution() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.removeContext("/client/v4");
        server.createContext("/client/v4", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Retry-After", "6");
            respond(exchange, 429, "{\"success\":false,\"errors\":[]}");
        });

        assertThrows(CloudflareApiException.class, () -> client(new CloudflareRetryPolicy(3, java.time.Duration.ZERO))
                .findZone("example.com"));

        assertEquals(1, requests.get());
    }

    @Test
    void doesNotRetryPatchResponses() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.removeContext("/client/v4");
        server.createContext("/client/v4", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 503, "{\"success\":false,\"errors\":[]}");
        });

        assertThrows(CloudflareApiException.class, () -> client(new CloudflareRetryPolicy(3, java.time.Duration.ZERO))
                .updateRecord("zone-id", "record-id",
                        new DnsRecordUpdate("host.example.com", "A", "198.51.100.11", 300, false)));

        assertEquals(1, requests.get());
    }

    private CloudflareApiClient client() {
        return client(CloudflareRetryPolicy.defaults());
    }

    private CloudflareApiClient client(CloudflareRetryPolicy retryPolicy) {
        URI baseUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/client/v4");
        return new CloudflareHttpClient(
                HttpClient.newHttpClient(),
                baseUri,
                () -> "test-token".toCharArray(),
                new ObjectMapper(),
                retryPolicy);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes()));
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/zones") && "GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 200, "{\"success\":true,\"result\":[{\"id\":\"zone-id\",\"name\":\"example.com\",\"status\":\"active\",\"paused\":false}]}");
        } else if (path.endsWith("/dns_records") && "GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 200, "{\"success\":true,\"result\":[{\"id\":\"record-id\",\"name\":\"host.example.com\",\"type\":\"A\",\"content\":\"198.51.100.10\",\"ttl\":300,\"proxied\":false,\"comment\":\"managed externally\"}]}");
        } else if (path.endsWith("/record-id") && "PATCH".equals(exchange.getRequestMethod())) {
            respond(exchange, 200, "{\"success\":true,\"result\":{\"id\":\"record-id\",\"name\":\"host.example.com\",\"type\":\"A\",\"content\":\"198.51.100.11\",\"ttl\":300,\"proxied\":false}}");
        } else {
            respond(exchange, 404, "{\"success\":false,\"errors\":[]}");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
