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
import com.proactiveidea.jcloudflareddns.api.DnsRecordUpdate;
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

/** Updates configured DNS records when their public IP addresses change. */
@Command(name = "update", description = "Update a DNS record.")
public final class UpdateCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, required = true, description = "Path to the YAML configuration file.")
    private Path configPath;

    @Option(names = "--profile", description = "Named configuration profile to execute.")
    private String profile;

    @Option(names = "--all", description = "Update every named profile using the configured execution mode.")
    private boolean all;

    @Option(names = "--dry-run", description = "Show the update without changing Cloudflare (the default).")
    private boolean dryRun;

    @Option(names = "--apply", description = "Apply the DNS update to Cloudflare.")
    private boolean apply;

    @Spec
    private CommandSpec spec;

    private final PublicIpResolver injectedIpResolver;
    private final CloudflareApiClient injectedCloudflareClient;

    public UpdateCommand() {
        this(null, null);
    }

    UpdateCommand(PublicIpResolver ipResolver, CloudflareApiClient cloudflareClient) {
        injectedIpResolver = ipResolver;
        injectedCloudflareClient = cloudflareClient;
    }

    @Override
    public Integer call() {
        try (ExecutionLock executionLock = ExecutionLock.acquire(configPath)) {
            executionLock.isHeld();
            if (dryRun && apply) {
                errorOut("Options --dry-run and --apply cannot be used together.");
                return ExitCodes.USAGE_ERROR;
            }
            if (all && profile != null) {
                errorOut("Options --all and --profile cannot be used together.");
                return ExitCodes.USAGE_ERROR;
            }
            if (all) {
                return updateAll();
            }
            return updateOne(new ConfigurationLoader().load(configPath, profile), null);
        } catch (ConfigurationException exception) {
            errorOut("Error: " + exception.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        }
    }

    private int updateAll() throws ConfigurationException {
        Map<String, Configuration> profiles = new ConfigurationLoader().loadAll(configPath);
        ExecutionConfiguration execution = new ConfigurationLoader().loadExecution(configPath);
        if (execution.exceedsRecommendedConcurrency()) {
            errorOut("Warning: execution.maxConcurrency above 8 may increase resource usage.");
        }
        return new ProfileExecutor().execute(profiles, execution,
                (profileName, configuration) -> updateOne(configuration, profileName));
    }

    private int updateOne(Configuration configuration, String profileName) {
        try {
            var validationErrors = new ConfigurationValidator().validate(configuration);
            if (!validationErrors.isEmpty()) {
                validationErrors.forEach(error -> errorOut(profileName, "Error: " + error));
                return ExitCodes.VALIDATION_ERROR;
            }
            IpVersion ipVersion = IpVersion.fromConfiguration(configuration.ipVersion());
            PublicIpResolver ipResolver = injectedIpResolver != null
                    ? injectedIpResolver
                    : new PublicIpResolver(
                            java.net.http.HttpClient.newHttpClient(),
                            PublicIpProviders.select(configuration.ipProviderUrls(),
                                    configuration.useDefaultIpProviders(), ipVersion), ipVersion);
            CloudflareApiClient cloudflare = injectedCloudflareClient != null
                    ? injectedCloudflareClient
                    : new CloudflareHttpClient(new EnvironmentApiTokenProvider(configuration.tokenEnv()));
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
                output(profileName, "DNS record is already up to date: " + publicIp.value());
                return ExitCodes.SUCCESS;
            }
            if (!apply) {
                output(profileName, "Dry run: would update " + record.name() + " from "
                        + record.content() + " to " + publicIp.value() + ".");
                return ExitCodes.SUCCESS;
            }
            cloudflare.updateRecord(zone.id(), record.id(), new DnsRecordUpdate(
                    record.name(), ipVersion.recordType(), publicIp.value(),
                    configuration.ttl(), configuration.proxied()));
            output(profileName, "Updated " + record.name() + " from " + record.content()
                    + " to " + publicIp.value() + ".");
            return ExitCodes.SUCCESS;
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
        synchronized (spec.commandLine()) {
            spec.commandLine().getErr().println(message);
        }
    }
}
