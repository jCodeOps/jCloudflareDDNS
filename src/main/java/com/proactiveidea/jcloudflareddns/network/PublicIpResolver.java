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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/** Resolves the public IPv4 address from an explicit HTTPS provider. */
public final class PublicIpResolver {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final URI providerUri;
    private final IpVersion ipVersion;

    public PublicIpResolver(URI providerUri) {
        this(providerUri, IpVersion.IPV4);
    }

    public PublicIpResolver(URI providerUri, IpVersion ipVersion) {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(), providerUri, ipVersion);
    }

    public PublicIpResolver(HttpClient httpClient, URI providerUri) {
        this(httpClient, providerUri, IpVersion.IPV4);
    }

    public PublicIpResolver(HttpClient httpClient, URI providerUri, IpVersion ipVersion) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.providerUri = validateUri(Objects.requireNonNull(providerUri, "providerUri"));
        this.ipVersion = Objects.requireNonNull(ipVersion, "ipVersion");
    }

    public IpAddress resolve() throws PublicIpException {
        HttpRequest request = HttpRequest.newBuilder(providerUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/plain")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PublicIpException("Public IP provider returned an unsuccessful response.");
            }
            try {
                return IpAddress.parse(response.body(), ipVersion);
            } catch (IllegalArgumentException exception) {
                throw new PublicIpException("Public IP provider returned an invalid IPv4 address.", exception);
            }
        } catch (IOException exception) {
            throw new PublicIpException("Public IP provider request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicIpException("Public IP provider request was interrupted.", exception);
        }
    }

    private static URI validateUri(URI uri) {
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Public IP provider URI must use HTTP or HTTPS.");
        }
        return uri;
    }
}
