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

class UpdateCommandTest {

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
    void dryRunDoesNotUpdateTheRecord() throws Exception {
        RecordingCloudflareClient cloudflare = new RecordingCloudflareClient("198.51.100.10");
        CliResult result = execute(configuration(), cloudflare, true);

        assertEquals(ExitCodes.SUCCESS, result.exitCode());
        assertTrue(result.stdout().contains("Dry run: would update"));
        assertEquals(0, cloudflare.updateCalls);
    }

    @Test
    void updateChangesOnlyTheRecordContentAndUsesConfiguredSettings() throws Exception {
        RecordingCloudflareClient cloudflare = new RecordingCloudflareClient("198.51.100.10");
        CliResult result = execute(configuration(), cloudflare, false);

        assertEquals(ExitCodes.SUCCESS, result.exitCode());
        assertTrue(result.stdout().contains("Updated host.example.com"));
        assertEquals(1, cloudflare.updateCalls);
        assertEquals("198.51.100.11", cloudflare.lastUpdate.content());
        assertEquals(300, cloudflare.lastUpdate.ttl());
        assertTrue(!cloudflare.lastUpdate.proxied());
    }

    @Test
    void updateIsSkippedWhenTheRecordAlreadyMatches() throws Exception {
        RecordingCloudflareClient cloudflare = new RecordingCloudflareClient("198.51.100.11");
        CliResult result = execute(configuration(), cloudflare, false);

        assertEquals(ExitCodes.SUCCESS, result.exitCode());
        assertTrue(result.stdout().contains("already up to date"));
        assertEquals(0, cloudflare.updateCalls);
    }

    private Path configuration() throws IOException {
        return Files.writeString(temporaryDirectory.resolve("config.yml"), """
                zone: example.com
                record: host.example.com
                ttl: 300
                proxied: false
                tokenEnv: CLOUDFLARE_API_TOKEN
                ipProviderUrl: https://example.com/ip
                """, StandardCharsets.UTF_8);
    }

    private CliResult execute(Path config, RecordingCloudflareClient cloudflare, boolean dryRun) {
        PublicIpResolver resolver = new PublicIpResolver(
                java.net.http.HttpClient.newHttpClient(),
                URI.create("http://localhost:" + server.getAddress().getPort() + "/ip"));
        UpdateCommand command = new UpdateCommand(resolver, cloudflare);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(command);
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
        String[] args = dryRun
                ? new String[]{"--config", config.toString()}
                : new String[]{"--config", config.toString(), "--apply"};
        int exitCode = commandLine.execute(args);
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

    private static final class RecordingCloudflareClient implements CloudflareApiClient {

        private final String currentContent;
        private int updateCalls;
        private DnsRecordUpdate lastUpdate;

        private RecordingCloudflareClient(String currentContent) {
            this.currentContent = currentContent;
        }

        @Override
        public Zone findZone(String name) {
            return new Zone("zone-id", name, "active");
        }

        @Override
        public List<DnsRecord> listRecords(String zoneId, String name, String type) {
            return List.of(new DnsRecord("record-id", name, type, currentContent, 120, false));
        }

        @Override
        public DnsRecord updateRecord(String zoneId, String recordId, DnsRecordUpdate update)
                throws CloudflareApiException, AuthenticationException {
            updateCalls++;
            lastUpdate = update;
            return new DnsRecord(recordId, update.name(), update.type(), update.content(), update.ttl(), update.proxied());
        }
    }
}
