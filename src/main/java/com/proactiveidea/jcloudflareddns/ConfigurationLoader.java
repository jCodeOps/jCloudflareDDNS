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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/** Loads non-secret configuration from YAML. */
public final class ConfigurationLoader {

    private final ObjectMapper mapper;

    public ConfigurationLoader() {
        mapper = new ObjectMapper(new YAMLFactory())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public Configuration load(Path path) throws ConfigurationException {
        return load(path, null);
    }

    public Configuration load(Path path, String profile) throws ConfigurationException {
        if (path == null) {
            throw new ConfigurationException("Configuration path is required.");
        }
        if (!Files.isRegularFile(path)) {
            throw new ConfigurationException("Configuration file does not exist: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new ConfigurationException("Configuration file is not readable: " + path);
        }

        try {
            String yaml = Files.readString(path, StandardCharsets.UTF_8);
            JsonNode document = mapper.readTree(yaml);
            rejectSecretKeys(document);
            if (document == null || document.isNull()) {
                throw new ConfigurationException("Configuration file is empty.");
            }
            if (!document.has("profiles") && !document.has("defaults") && !document.has("execution")) {
                if (profile != null) {
                    throw new ConfigurationException("--profile requires a multi-profile configuration.");
                }
                return mapper.treeToValue(document, Configuration.class);
            }
            validateExecution(document.get("execution"));
            return resolveProfile(document, profile);
        } catch (ConfigurationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ConfigurationException("Configuration file could not be parsed.", exception);
        }
    }

    private Configuration resolveProfile(JsonNode document, String profile) throws IOException, ConfigurationException {
        ConfigurationOverride defaults = document.has("defaults")
                ? mapper.treeToValue(document.get("defaults"), ConfigurationOverride.class)
                : new ConfigurationOverride(null, null, null, null, null, null, null, null);
        JsonNode profilesNode = document.get("profiles");
        if (profilesNode == null || !profilesNode.isObject() || profilesNode.isEmpty()) {
            throw new ConfigurationException("profiles must contain at least one named profile.");
        }
        if (profile == null || profile.isBlank()) {
            throw new ConfigurationException("A profile name is required for a multi-profile configuration.");
        }
        JsonNode selectedNode = profilesNode.get(profile);
        if (selectedNode == null) {
            throw new ConfigurationException("Unknown configuration profile: " + profile);
        }
        ConfigurationOverride selected = mapper.treeToValue(selectedNode, ConfigurationOverride.class);
        return merge(defaults, selected);
    }

    private void validateExecution(JsonNode executionNode) throws IOException, ConfigurationException {
        if (executionNode == null) {
            return;
        }
        ExecutionConfiguration execution = mapper.treeToValue(executionNode, ExecutionConfiguration.class);
        if (!execution.mode().equals("sequential") && !execution.mode().equals("parallel")) {
            throw new ConfigurationException("execution.mode must be sequential or parallel.");
        }
        if (execution.maxConcurrency() != null
                && (execution.maxConcurrency() < 1 || execution.maxConcurrency() > 16)) {
            throw new ConfigurationException("execution.maxConcurrency must be between 1 and 16.");
        }
        if (execution.mode().equals("sequential")
                && execution.maxConcurrency() != null && execution.maxConcurrency() != 1) {
            throw new ConfigurationException(
                    "execution.maxConcurrency must be 1 when execution.mode is sequential.");
        }
    }

    private static Configuration merge(ConfigurationOverride defaults, ConfigurationOverride selected) {
        return new Configuration(
                first(selected.zone(), defaults.zone()),
                first(selected.record(), defaults.record()),
                first(selected.ttl(), defaults.ttl()),
                first(selected.proxied(), defaults.proxied()),
                first(selected.tokenEnv(), defaults.tokenEnv()),
                first(selected.ipProviderUrls(), defaults.ipProviderUrls()),
                first(selected.useDefaultIpProviders(), defaults.useDefaultIpProviders()),
                first(selected.ipVersion(), defaults.ipVersion()));
    }

    private static <T> T first(T selected, T fallback) {
        return selected != null ? selected : fallback;
    }

    private static void rejectSecretKeys(JsonNode node) throws ConfigurationException {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (isSecretKey(field)) {
                    throw new ConfigurationException(
                            "Configuration must not contain secret values; use tokenEnv instead.");
                }
                rejectSecretKeys(node.get(field));
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                rejectSecretKeys(item);
            }
        }
    }

    private static boolean isSecretKey(String key) {
        return key.equalsIgnoreCase("token")
                || key.equalsIgnoreCase("apiToken")
                || key.equalsIgnoreCase("globalApiKey")
                || key.equalsIgnoreCase("password")
                || key.equalsIgnoreCase("secret");
    }
}
