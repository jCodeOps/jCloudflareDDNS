/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.proactiveidea.jcloudflareddns.api.AuthenticationException;
import com.proactiveidea.jcloudflareddns.api.CloudflareApiClient;
import com.proactiveidea.jcloudflareddns.api.CloudflareApiException;
import com.proactiveidea.jcloudflareddns.api.DnsRecord;
import com.proactiveidea.jcloudflareddns.api.DnsRecordUpdate;
import com.proactiveidea.jcloudflareddns.api.Zone;
import com.proactiveidea.jcloudflareddns.network.PublicIpResolver;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import picocli.CommandLine;

class CheckCommandTest {

    @TempDir
    Path temporaryDirectory;

    private HttpServer server;

    @BeforeEach
    void startIpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ip", this::respondWithIp);
        server.start();
    }

    @AfterEach
    void stopIpServer() {
        server.stop(0);
    }

    @Test
    void succeedsWhenTheDnsRecordMatchesThePublicIp() throws Exception {
        CliResult result = execute("198.51.100.11");

        assertEquals(ExitCodes.SUCCESS, result.exitCode());
        assertTrue(result.stdout().contains("DNS record is up to date"));
    }

    @Test
    void reportsFailureWhenTheDnsRecordDiffersFromThePublicIp() throws Exception {
        CliResult result = execute("198.51.100.10");

        assertEquals(ExitCodes.FAILURE, result.exitCode());
        assertTrue(result.stdout().contains("DNS record differs"));
    }

    @Test
    void reportsFailureWhenTheProxySettingDiffers() throws Exception {
        CliResult result = execute(new DnsRecord(
                "record-id", "host.example.com", "A", "198.51.100.11", 300, false));

        assertEquals(ExitCodes.FAILURE, result.exitCode());
        assertTrue(result.stdout().contains("configured proxied: true"));
        assertTrue(result.stdout().contains("DNS proxied: false"));
    }

    @Test
    void acceptsCloudflareAutoTtlForAProxiedRecord() throws Exception {
        CliResult result = execute(new DnsRecord(
                "record-id", "host.example.com", "A", "198.51.100.11", 1, true));

        assertEquals(ExitCodes.SUCCESS, result.exitCode());
    }

    @Test
    void identifiesInvalidCloudflareRecordContent() throws Exception {
        CliResult result = execute("not-an-ip-address");

        assertEquals(ExitCodes.API_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("host.example.com"));
        assertTrue(result.stderr().contains("not-an-ip-address"));
    }

    private CliResult execute(String currentContent) throws Exception {
        return execute(new DnsRecord("record-id", "host.example.com", "A", currentContent, 1, true));
    }

    private CliResult execute(DnsRecord record) throws Exception {
        Path config = Files.writeString(temporaryDirectory.resolve("config.yml"), """
                zone: example.com
                record: host.example.com
                ttl: 300
                tokenEnv: CLOUDFLARE_API_TOKEN
                """, StandardCharsets.UTF_8);
        PublicIpResolver resolver = new PublicIpResolver(
                java.net.http.HttpClient.newHttpClient(),
                URI.create("http://localhost:" + server.getAddress().getPort() + "/ip"));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = new CommandLine(new CheckCommand(resolver, new StaticCloudflareClient(record)))
                .setOut(new PrintWriter(stdout, true))
                .setErr(new PrintWriter(stderr, true))
                .execute("--config", config.toString());
        return new CliResult(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private void respondWithIp(HttpExchange exchange) throws IOException {
        byte[] body = "198.51.100.11\n".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private record CliResult(int exitCode, String stdout, String stderr) {
    }

    private static final class StaticCloudflareClient implements CloudflareApiClient {

        private final DnsRecord record;

        private StaticCloudflareClient(DnsRecord record) {
            this.record = record;
        }

        @Override
        public Zone findZone(String name) {
            return new Zone("zone-id", name, "active");
        }

        @Override
        public List<DnsRecord> listRecords(String zoneId, String name, String type) {
            return List.of(record);
        }

        @Override
        public DnsRecord updateRecord(String zoneId, String recordId, DnsRecordUpdate update)
                throws CloudflareApiException, AuthenticationException {
            throw new UnsupportedOperationException("check does not update DNS records");
        }
    }
}
