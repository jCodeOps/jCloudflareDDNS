/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

import com.proactiveidea.jcloudflareddns.api.AuthenticationException;
import com.proactiveidea.jcloudflareddns.api.CloudflareApiClient;
import com.proactiveidea.jcloudflareddns.api.CloudflareApiException;
import com.proactiveidea.jcloudflareddns.api.CloudflareHttpClient;
import com.proactiveidea.jcloudflareddns.api.DnsRecord;
import com.proactiveidea.jcloudflareddns.api.EnvironmentApiTokenProvider;
import com.proactiveidea.jcloudflareddns.api.Zone;
import com.proactiveidea.jcloudflareddns.network.IpAddress;
import com.proactiveidea.jcloudflareddns.network.IpVersion;
import com.proactiveidea.jcloudflareddns.network.PublicIpException;
import com.proactiveidea.jcloudflareddns.network.PublicIpProviders;
import com.proactiveidea.jcloudflareddns.network.PublicIpResolver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Checks configured DNS records against their current public IP addresses. */
@Command(name = "check", description = "Check the current DNS state.")
public final class CheckCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, required = true, description = "Path to the YAML configuration file.")
    private Path configPath;

    @Option(names = "--profile", description = "Named configuration profile to execute.")
    private String profile;

    @Option(names = "--all", description = "Check every named profile sequentially.")
    private boolean all;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        try {
            if (all && profile != null) {
                errorOut("Options --all and --profile cannot be used together.");
                return ExitCodes.USAGE_ERROR;
            }
            if (all) {
                return checkAll();
            }
            return checkOne(new ConfigurationLoader().load(configPath, profile), null);
        } catch (ConfigurationException exception) {
            errorOut("Error: " + exception.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        }
    }

    private int checkAll() throws ConfigurationException {
        Map<String, Configuration> profiles = new ConfigurationLoader().loadAll(configPath);
        int result = ExitCodes.SUCCESS;
        for (Map.Entry<String, Configuration> entry : profiles.entrySet()) {
            result = combine(result, checkOne(entry.getValue(), entry.getKey()));
        }
        return result;
    }

    private int checkOne(Configuration configuration, String profileName) {
        try {
            var validationErrors = new ConfigurationValidator().validate(configuration);
            if (!validationErrors.isEmpty()) {
                validationErrors.forEach(error -> errorOut(profileName, "Error: " + error));
                return ExitCodes.VALIDATION_ERROR;
            }
            IpVersion ipVersion = IpVersion.fromConfiguration(configuration.ipVersion());
            PublicIpResolver ipResolver = new PublicIpResolver(
                    java.net.http.HttpClient.newHttpClient(),
                    PublicIpProviders.select(configuration.ipProviderUrls(),
                            configuration.useDefaultIpProviders(), ipVersion), ipVersion);
            CloudflareApiClient cloudflare = new CloudflareHttpClient(
                    new EnvironmentApiTokenProvider(configuration.tokenEnv()));
            IpAddress publicIp = ipResolver.resolve();
            Zone zone = cloudflare.findZone(configuration.zone());
            var records = cloudflare.listRecords(zone.id(), configuration.record(), ipVersion.recordType());
            if (records.isEmpty()) {
                errorOut(profileName, "No DNS record was found for the configured hostname.");
                return ExitCodes.API_ERROR;
            }
            if (records.size() > 1) {
                errorOut(profileName, "Multiple DNS records were found for the configured hostname.");
                return ExitCodes.API_ERROR;
            }
            DnsRecord record = records.getFirst();
            if (publicIp.value().equals(IpAddress.parse(record.content(), ipVersion).value())) {
                output(profileName, "DNS record is up to date: " + publicIp.value());
                return ExitCodes.SUCCESS;
            }
            output(profileName, "DNS record differs. Public IP: " + publicIp.value()
                    + "; DNS record: " + record.content());
            return ExitCodes.FAILURE;
        } catch (AuthenticationException exception) {
            errorOut(profileName, "Authentication error: " + exception.getMessage());
            return ExitCodes.AUTHENTICATION_ERROR;
        } catch (PublicIpException exception) {
            errorOut(profileName, "Network error: " + exception.getMessage());
            return ExitCodes.NETWORK_ERROR;
        } catch (CloudflareApiException exception) {
            if (exception.statusCode() == 401 || exception.statusCode() == 403) {
                errorOut(profileName, "Authentication error: Cloudflare rejected the API token.");
                return ExitCodes.AUTHENTICATION_ERROR;
            }
            errorOut(profileName, "Cloudflare API error: " + exception.getMessage());
            return exception.statusCode() == 0 ? ExitCodes.NETWORK_ERROR : ExitCodes.API_ERROR;
        } catch (IllegalArgumentException exception) {
            errorOut(profileName, "Cloudflare API returned an invalid IP record.");
            return ExitCodes.API_ERROR;
        }
    }

    private static int combine(int current, int next) {
        return current == ExitCodes.SUCCESS ? next : current;
    }

    private void output(String profileName, String message) {
        if (profileName == null) {
            spec.commandLine().getOut().println(message);
        } else {
            spec.commandLine().getOut().printf("Profile '%s': %s%n", profileName, message);
        }
    }

    private void errorOut(String profileName, String message) {
        if (profileName == null) {
            errorOut(message);
        } else {
            errorOut("Profile '" + profileName + "': " + message);
        }
    }

    private void errorOut(String message) {
        spec.commandLine().getErr().println(message);
    }
}
