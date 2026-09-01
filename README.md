# jCloudflareDDNS

> Early development — Stage 12 complete. The one-shot CLI foundation and initial DNS update flow are under active development.

jCloudflareDDNS is planned as a secure, lightweight, cross-platform Dynamic DNS client for Cloudflare, written in modern Java. It will focus on updating Cloudflare DNS records safely when a host's public IP changes, while keeping configuration explicit and credentials protected.

The project is intentionally CLI-oriented and uses one-shot execution. FreeBSD is the first platform validation target, followed by Linux distributions. Native-image-friendly design and future packaging for both platforms remain part of the roadmap.

## Technical direction

- Java 25 and Maven
- Java standard library first, including `java.net.http.HttpClient`
- GraalVM Native Image compatibility as a first-class requirement
- Cloudflare API Tokens with least privilege; Global API Keys will not be used in new functionality
- No Spring, web server, database, or dependency-injection framework

## CLI foundation

The current CLI exposes `--help`, `--version`, and the commands `check`, `update`, and `validate`. `validate` checks a local YAML configuration file, `check` resolves the configured public IP version and queries the matching Cloudflare record, and `update` performs a one-shot DNS update when the addresses differ. `update` is a dry run by default; pass `--apply` to write to Cloudflare.

An example non-secret configuration is:

```yaml
zone: example.com
record: host.example.com
ttl: 300
proxied: false
tokenEnv: CLOUDFLARE_API_TOKEN
useDefaultIpProviders: true
# Optional additional providers, tried after the built-in defaults:
# ipProviderUrls:
#   - https://my-company.example/public-ip
ipVersion: ipv4
```

By default, the resolver uses two HTTPS providers for the selected address family.
Set `useDefaultIpProviders: false` to use only the ordered URLs in `ipProviderUrls`.
Configured URLs are useful for adding an organization-controlled provider or for
fully replacing the defaults. Provider failures are bounded and transient network
failures are retried briefly; a future timer remains responsible for later runs.

The configuration can be validated with `jcloudflareddns validate --config config.yml` when running the CLI. The `tokenEnv` value names the environment variable used to obtain the API Token; never place a token in the YAML file.

See the [configuration guide](docs/configuration.md) for the complete schema and retry behavior.

To create a starter multi-profile configuration without storing a token, run
`jcloudflareddns config init --output config.yml`.

For multiple DNS targets, one YAML file may define shared `defaults` and named
`profiles`. Select a profile with `--profile`, for example:

```sh
jcloudflareddns validate --config config.yml --profile home
```

`check --all` and `update --all` process every profile using the configured
execution mode. Sequential mode is the default; parallel mode uses the bounded
`maxConcurrency` value.

Configuration validation can inspect every profile sequentially with `--all`.

Future stages may add logging, scheduling, daemon mode, and packaging. None of those features should be assumed available in this release.

## Development

Install JDK 25 and follow the [installation guide](docs/installation.md), then run `./mvnw clean verify`. `./mvnw package` also creates portable `tar.gz` and `zip` distribution archives under `target/`. GitHub Actions repeats this verification on Linux with JDK 25 and smoke-tests the portable `tar.gz` archive.

## Diagnostics and support

Normal CLI output is concise. Use `--verbose` before the command for safe error
classification, or `--debug` for the exception and cause types. These modes do
not print stack traces, API Tokens, environment values, HTTP authorization
headers, or response bodies. Report reproducible problems to
[jcabrerav@proactiveidea.com](mailto:jcabrerav@proactiveidea.com), including
the safe diagnostic output and the application version.

## License and independence

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026 Proactive Idea. Author: Jenny Cabrera Varona.

Portable distributions include runtime dependency notices in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
They also include CycloneDX SBOM files, `jcloudflareddns-sbom.json` and
`jcloudflareddns-sbom.xml`, for the packaged runtime components.

jCloudflareDDNS is an independent project and is not affiliated with or endorsed by Cloudflare, Inc.
