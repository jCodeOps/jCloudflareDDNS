/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Resolves a public IP using ordered providers and bounded transient-failure retries. */
public final class PublicIpResolver {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RETRY_DELAY = Duration.ofMillis(250);
    private static final int MAX_TOTAL_ATTEMPTS = 3;
    private static final int MAX_ATTEMPTS_PER_PROVIDER = 2;

    private final HttpClient httpClient;
    private final List<URI> providerUris;
    private final IpVersion ipVersion;

    public PublicIpResolver(URI providerUri) {
        this(providerUri, IpVersion.IPV4);
    }

    public PublicIpResolver(URI providerUri, IpVersion ipVersion) {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(), List.of(providerUri), ipVersion);
    }

    public PublicIpResolver(HttpClient httpClient, URI providerUri) {
        this(httpClient, List.of(providerUri), IpVersion.IPV4);
    }

    public PublicIpResolver(HttpClient httpClient, URI providerUri, IpVersion ipVersion) {
        this(httpClient, List.of(providerUri), ipVersion);
    }

    public PublicIpResolver(HttpClient httpClient, List<URI> providerUris, IpVersion ipVersion) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        if (providerUris == null || providerUris.isEmpty()) {
            throw new IllegalArgumentException("At least one public IP provider is required.");
        }
        this.providerUris = providerUris.stream()
                .map(Objects::requireNonNull)
                .map(PublicIpResolver::validateUri)
                .toList();
        this.ipVersion = Objects.requireNonNull(ipVersion, "ipVersion");
    }

    public IpAddress resolve() throws PublicIpException {
        List<Exception> failures = new ArrayList<>();
        int attempts = 0;
        for (URI providerUri : providerUris) {
            for (int providerAttempt = 1;
                    providerAttempt <= MAX_ATTEMPTS_PER_PROVIDER && attempts < MAX_TOTAL_ATTEMPTS;
                    providerAttempt++) {
                attempts++;
                try {
                    return request(providerUri);
                } catch (RetryableProviderException exception) {
                    failures.add(exception);
                    if (attempts < MAX_TOTAL_ATTEMPTS && providerAttempt < MAX_ATTEMPTS_PER_PROVIDER) {
                        pauseBeforeRetry();
                    }
                } catch (NonRetryableProviderException exception) {
                    failures.add(exception);
                    break;
                }
            }
        }
        PublicIpException failure = new PublicIpException(
                "All public IP providers failed after " + attempts + " attempt(s).");
        failures.forEach(failure::addSuppressed);
        throw failure;
    }

    private IpAddress request(URI providerUri)
            throws RetryableProviderException, NonRetryableProviderException {
        HttpRequest request = HttpRequest.newBuilder(providerUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/plain")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 500 && response.statusCode() < 600) {
                throw new RetryableProviderException("Provider returned a server error.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NonRetryableProviderException("Provider returned an unsuccessful response.");
            }
            try {
                return IpAddress.parse(response.body(), ipVersion);
            } catch (IllegalArgumentException exception) {
                throw new NonRetryableProviderException("Provider returned an invalid IP address.", exception);
            }
        } catch (IOException exception) {
            throw new RetryableProviderException("Provider request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NonRetryableProviderException("Provider request was interrupted.", exception);
        }
    }

    private static void pauseBeforeRetry() throws PublicIpException {
        try {
            Thread.sleep(RETRY_DELAY);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicIpException("Public IP provider retry was interrupted.", exception);
        }
    }

    private static URI validateUri(URI uri) {
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Public IP provider URI must use HTTP or HTTPS.");
        }
        return uri;
    }

    private static final class RetryableProviderException extends Exception {
        private static final long serialVersionUID = 1L;

        private RetryableProviderException(String message) { super(message); }
        private RetryableProviderException(String message, Throwable cause) { super(message, cause); }
    }

    private static final class NonRetryableProviderException extends Exception {
        private static final long serialVersionUID = 1L;

        private NonRetryableProviderException(String message) { super(message); }
        private NonRetryableProviderException(String message, Throwable cause) { super(message, cause); }
    }
}
