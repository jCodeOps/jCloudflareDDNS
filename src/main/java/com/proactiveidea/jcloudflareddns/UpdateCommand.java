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

import java.net.URI;
import java.nio.file.Path;
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
import com.proactiveidea.jcloudflareddns.network.PublicIpResolver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Updates the configured A record when its public IPv4 address changes. */
@Command(name = "update", description = "Update a DNS record.")
public final class UpdateCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, required = true, description = "Path to the YAML configuration file.")
    private Path configPath;

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
        try {
            if (dryRun && apply) {
                errorOut("Options --dry-run and --apply cannot be used together.");
                return ExitCodes.USAGE_ERROR;
            }
            Configuration configuration = new ConfigurationLoader().load(configPath);
            var validationErrors = new ConfigurationValidator().validate(configuration);
            if (!validationErrors.isEmpty()) {
                validationErrors.forEach(error -> errorOut("Error: " + error));
                return ExitCodes.VALIDATION_ERROR;
            }

            IpVersion ipVersion = IpVersion.fromConfiguration(configuration.ipVersion());
            PublicIpResolver ipResolver = injectedIpResolver != null
                    ? injectedIpResolver
                    : new PublicIpResolver(URI.create(configuration.ipProviderUrl()), ipVersion);
            CloudflareApiClient cloudflare = injectedCloudflareClient != null
                    ? injectedCloudflareClient
                    : new CloudflareHttpClient(
                            new EnvironmentApiTokenProvider(configuration.tokenEnv()));
            IpAddress publicIp = ipResolver.resolve();
            Zone zone = cloudflare.findZone(configuration.zone());
            var records = cloudflare.listRecords(
                    zone.id(), configuration.record(), ipVersion.recordType());
            if (records.isEmpty()) {
                errorOut("No A record was found for the configured hostname.");
                return ExitCodes.API_ERROR;
            }
            if (records.size() > 1) {
                errorOut("Multiple A records were found for the configured hostname.");
                return ExitCodes.API_ERROR;
            }

            DnsRecord record = records.getFirst();
            if (publicIp.value().equals(IpAddress.parse(record.content(), ipVersion).value())) {
                spec.commandLine().getOut().printf("DNS record is already up to date: %s%n", publicIp.value());
                return ExitCodes.SUCCESS;
            }
            if (!apply) {
                spec.commandLine().getOut().printf(
                        "Dry run: would update %s from %s to %s.%n",
                        record.name(), record.content(), publicIp.value());
                return ExitCodes.SUCCESS;
            }

            cloudflare.updateRecord(
                    zone.id(),
                    record.id(),
                    new DnsRecordUpdate(
                            record.name(), ipVersion.recordType(), publicIp.value(),
                            configuration.ttl(), configuration.proxied()));
            spec.commandLine().getOut().printf(
                    "Updated %s from %s to %s.%n", record.name(), record.content(), publicIp.value());
            return ExitCodes.SUCCESS;
        } catch (ConfigurationException exception) {
            errorOut("Error: " + exception.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (AuthenticationException exception) {
            errorOut("Authentication error: " + exception.getMessage());
            return ExitCodes.AUTHENTICATION_ERROR;
        } catch (PublicIpException exception) {
            errorOut("Network error: " + exception.getMessage());
            return ExitCodes.NETWORK_ERROR;
        } catch (CloudflareApiException exception) {
            if (exception.statusCode() == 401 || exception.statusCode() == 403) {
                errorOut("Authentication error: Cloudflare rejected the API token.");
                return ExitCodes.AUTHENTICATION_ERROR;
            }
            errorOut("Cloudflare API error: " + exception.getMessage());
            return exception.statusCode() == 0 ? ExitCodes.NETWORK_ERROR : ExitCodes.API_ERROR;
        } catch (IllegalArgumentException exception) {
            errorOut("Cloudflare API returned an invalid IP record.");
            return ExitCodes.API_ERROR;
        }
    }

    private void errorOut(String message) {
        spec.commandLine().getErr().println(message);
    }
}
