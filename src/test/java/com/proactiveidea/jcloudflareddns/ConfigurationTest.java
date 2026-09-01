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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsAndValidatesAValidConfiguration() throws Exception {
        Path path = write("""
                zone: example.com
                record: host.example.com
                ttl: 300
                proxied: false
                tokenEnv: CLOUDFLARE_API_TOKEN
                ipProviderUrls:
                  - https://api.ipify.org
                """);

        Configuration configuration = new ConfigurationLoader().load(path);
        List<String> errors = new ConfigurationValidator().validate(configuration);

        assertEquals("example.com", configuration.zone());
        assertEquals("host.example.com", configuration.record());
        assertEquals(300, configuration.ttl());
        assertFalse(configuration.proxied());
        assertTrue(errors.isEmpty());
    }

    @Test
    void reportsInvalidConfigurationFields() throws Exception {
        Path path = write("""
                zone: example.com
                record: other.net
                ttl: 0
                tokenEnv: lowercase-name
                ipProviderUrls:
                  - https://api.ipify.org
                """);

        Configuration configuration = new ConfigurationLoader().load(path);
        List<String> errors = new ConfigurationValidator().validate(configuration);

        assertEquals(3, errors.size());
        assertTrue(errors.contains("record must belong to zone."));
        assertTrue(errors.contains("ttl must be between 1 and 86400 seconds."));
        assertTrue(errors.contains("tokenEnv must be a valid uppercase environment variable name."));
    }

    @Test
    void rejectsAnEmbeddedSecretWithoutIncludingItsValue() throws Exception {
        Path path = write("""
                zone: example.com
                record: host.example.com
                ttl: 300
                token: super-secret-token
                tokenEnv: CLOUDFLARE_API_TOKEN
                """);

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> new ConfigurationLoader().load(path));

        assertTrue(exception.getMessage().contains("must not contain secret values"));
        assertFalse(exception.getMessage().contains("super-secret-token"));
    }

    @Test
    void rejectsNestedSecretsWithoutIncludingTheirValues() throws Exception {
        Path path = write("""
                zone: example.com
                record: host.example.com
                ttl: 300
                tokenEnv: CLOUDFLARE_API_TOKEN
                metadata:
                  credentials:
                    apiToken: nested-secret
                """);

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> new ConfigurationLoader().load(path));

        assertTrue(exception.getMessage().contains("must not contain secret values"));
        assertFalse(exception.getMessage().contains("nested-secret"));
    }

    @Test
    void rejectsUnknownConfigurationProperties() throws Exception {
        Path path = write("""
                zone: example.com
                record: host.example.com
                ttl: 300
                tokenEnv: CLOUDFLARE_API_TOKEN
                ipProviderUrls:
                  - https://api.ipify.org
                unsupported: true
                """);

        assertThrows(ConfigurationException.class, () -> new ConfigurationLoader().load(path));
    }

    @Test
    void acceptsMultipleConfiguredProviders() throws Exception {
        Path path = write("""
                zone: example.com
                record: host.example.com
                ttl: 300
                tokenEnv: CLOUDFLARE_API_TOKEN
                useDefaultIpProviders: false
                ipProviderUrls:
                  - https://one.example/ip
                  - https://two.example/ip
                """);

        Configuration configuration = new ConfigurationLoader().load(path);

        assertEquals(List.of("https://one.example/ip", "https://two.example/ip"),
                configuration.ipProviderUrls());
        assertTrue(new ConfigurationValidator().validate(configuration).isEmpty());
    }

    @Test
    void requiresAConfiguredProviderWhenDefaultsAreDisabled() throws Exception {
        Path path = write("""
                zone: example.com
                record: host.example.com
                ttl: 300
                tokenEnv: CLOUDFLARE_API_TOKEN
                useDefaultIpProviders: false
                """);

        Configuration configuration = new ConfigurationLoader().load(path);

        assertTrue(new ConfigurationValidator().validate(configuration).contains(
                "At least one ipProviderUrls entry is required when useDefaultIpProviders is false."));
    }

    @Test
    void resolvesProfileValuesOverDefaults() throws Exception {
        Path path = write("""
                defaults:
                  ttl: 300
                  proxied: false
                  tokenEnv: CLOUDFLARE_DEFAULT_TOKEN
                  ipVersion: ipv4
                profiles:
                  office:
                    zone: example.com
                    record: office.example.com
                    tokenEnv: CLOUDFLARE_OFFICE_TOKEN
                    ipVersion: ipv6
                """);

        Configuration configuration = new ConfigurationLoader().load(path, "office");

        assertEquals("example.com", configuration.zone());
        assertEquals("office.example.com", configuration.record());
        assertEquals(300, configuration.ttl());
        assertEquals("CLOUDFLARE_OFFICE_TOKEN", configuration.tokenEnv());
        assertEquals("ipv6", configuration.ipVersion());
        assertTrue(new ConfigurationValidator().validate(configuration).isEmpty());
    }

    @Test
    void validatesExecutionModeAndSequentialConcurrency() throws Exception {
        Path path = write("""
                execution:
                  mode: sequential
                  maxConcurrency: 2
                profiles:
                  home:
                    zone: example.com
                    record: home.example.com
                    ttl: 300
                    tokenEnv: CLOUDFLARE_HOME_TOKEN
                """);

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> new ConfigurationLoader().load(path, "home"));

        assertTrue(exception.getMessage().contains("must be 1 when execution.mode is sequential"));
    }

    @Test
    void loadsParallelExecutionSettings() throws Exception {
        Path path = write("""
                execution:
                  mode: parallel
                  maxConcurrency: 2
                profiles:
                  home:
                    zone: example.com
                    record: home.example.com
                    ttl: 300
                    tokenEnv: CLOUDFLARE_HOME_TOKEN
                """);

        ExecutionConfiguration execution = new ConfigurationLoader().loadExecution(path);

        assertEquals("parallel", execution.mode());
        assertEquals(2, execution.maxConcurrency());
    }

    @Test
    void validatesAllProfilesAndReportsEveryResult() throws Exception {
        Path path = write("""
                profiles:
                  home:
                    zone: example.com
                    record: home.example.com
                    ttl: 300
                    tokenEnv: CLOUDFLARE_HOME_TOKEN
                  office:
                    zone: example.net
                    record: office.example.net
                    ttl: 0
                    tokenEnv: CLOUDFLARE_OFFICE_TOKEN
                """);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int exitCode = JCloudflareDdnsApplication.execute(
                new String[]{"validate", "--config", path.toString(), "--all"},
                new PrintWriter(output, true), new PrintWriter(errors, true));

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Profile 'home' is valid."));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Profile 'office': Error:"));
    }

    @Test
    void validateCommandReturnsSuccessForValidConfiguration() throws Exception {
        Path path = write("""
                zone: example.com
                record: host.example.com
                ttl: 300
                tokenEnv: CLOUDFLARE_API_TOKEN
                ipProviderUrls:
                  - https://api.ipify.org
                """);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = JCloudflareDdnsApplication.execute(
                new String[]{"validate", "--config", path.toString()},
                new PrintWriter(output, true),
                new PrintWriter(new ByteArrayOutputStream(), true));

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Configuration is valid."));
    }

    @Test
    void validateCommandReturnsValidationErrorForInvalidConfiguration() throws Exception {
        Path path = write("""
                zone: example.com
                record: other.net
                ttl: 0
                tokenEnv: invalid-name
                ipProviderUrls:
                  - https://api.ipify.org
                """);

        int exitCode = JCloudflareDdnsApplication.execute(
                new String[]{"validate", "--config", path.toString()},
                new PrintWriter(new ByteArrayOutputStream(), true),
                new PrintWriter(new ByteArrayOutputStream(), true));

        assertEquals(ExitCodes.VALIDATION_ERROR, exitCode);
    }

    private Path write(String content) throws Exception {
        Path path = temporaryDirectory.resolve("config.yml");
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}
