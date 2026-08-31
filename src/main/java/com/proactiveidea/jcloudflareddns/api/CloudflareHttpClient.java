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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** HTTP implementation of the Cloudflare API boundary. */
public final class CloudflareHttpClient implements CloudflareApiClient {

    private static final URI DEFAULT_BASE_URI = URI.create("https://api.cloudflare.com/client/v4");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final URI baseUri;
    private final ApiTokenProvider tokenProvider;
    private final ObjectMapper mapper;

    public CloudflareHttpClient(ApiTokenProvider tokenProvider) {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
                DEFAULT_BASE_URI, tokenProvider, new ObjectMapper());
    }

    public CloudflareHttpClient(
            HttpClient httpClient,
            URI baseUri,
            ApiTokenProvider tokenProvider,
            ObjectMapper mapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.baseUri = normalizeBaseUri(Objects.requireNonNull(baseUri, "baseUri"));
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Zone findZone(String name) throws CloudflareApiException, AuthenticationException {
        JsonNode result = send("GET", "/zones?name=" + queryValue(name), null);
        List<Zone> zones = new ArrayList<>();
        result.forEach(node -> zones.add(mapper.convertValue(node, Zone.class)));
        if (zones.isEmpty()) {
            throw new CloudflareApiException("Cloudflare zone was not found.", 404);
        }
        return zones.getFirst();
    }

    @Override
    public List<DnsRecord> listRecords(String zoneId, String name, String type)
            throws CloudflareApiException, AuthenticationException {
        String path = "/zones/" + pathSegment(zoneId)
                + "/dns_records?name=" + queryValue(name)
                + "&type=" + queryValue(type);
        JsonNode result = send("GET", path, null);
        List<DnsRecord> records = new ArrayList<>();
        result.forEach(node -> records.add(mapper.convertValue(node, DnsRecord.class)));
        return List.copyOf(records);
    }

    @Override
    public DnsRecord updateRecord(String zoneId, String recordId, DnsRecordUpdate update)
            throws CloudflareApiException, AuthenticationException {
        Objects.requireNonNull(update, "update");
        ObjectNode body = mapper.createObjectNode()
                .put("name", update.name())
                .put("type", update.type())
                .put("content", update.content())
                .put("ttl", update.ttl())
                .put("proxied", update.proxied());
        JsonNode result = send(
                "PATCH",
                "/zones/" + pathSegment(zoneId) + "/dns_records/" + pathSegment(recordId),
                body.toString());
        return mapper.convertValue(result, DnsRecord.class);
    }

    private JsonNode send(String method, String path, String body)
            throws CloudflareApiException, AuthenticationException {
        char[] token = tokenProvider.token();
        String tokenValue = new String(token);
        java.util.Arrays.fill(token, '\0');
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUri + path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + tokenValue);
            if (body == null) {
                request.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                request.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            HttpResponse<String> response = httpClient.send(
                    request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parseResponse(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new CloudflareApiException("Cloudflare API request failed.", 0, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CloudflareApiException("Cloudflare API request was interrupted.", 0, exception);
        } finally {
            tokenValue = "";
        }
    }

    private JsonNode parseResponse(int statusCode, String responseBody) throws CloudflareApiException {
        try {
            JsonNode root = mapper.readTree(responseBody);
            if (statusCode < 200 || statusCode >= 300 || !root.path("success").asBoolean(false)) {
                throw new CloudflareApiException(
                        "Cloudflare API returned an unsuccessful response.", statusCode);
            }
            JsonNode result = root.get("result");
            if (result == null || result.isMissingNode() || result.isNull()) {
                throw new CloudflareApiException("Cloudflare API response had no result.", statusCode);
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            throw new CloudflareApiException("Cloudflare API returned invalid JSON.", statusCode, exception);
        }
    }

    private static URI normalizeBaseUri(URI uri) {
        String value = uri.toString();
        return URI.create(value.endsWith("/") ? value.substring(0, value.length() - 1) : value);
    }

    private static String queryValue(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8);
    }

    private static String pathSegment(String value) throws CloudflareApiException {
        if (value == null || value.isBlank() || value.contains("/") || value.contains("?") || value.contains("#")) {
            throw new CloudflareApiException("Cloudflare API path identifier is invalid.", 0);
        }
        return value;
    }
}
