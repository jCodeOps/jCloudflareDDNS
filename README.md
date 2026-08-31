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
ipProviderUrl: https://api.ipify.org
ipVersion: ipv4
```

The configuration can be validated with `jcloudflareddns validate --config config.yml` when running the CLI. The `tokenEnv` value names the environment variable used to obtain the API Token; never place a token in the YAML file.

Future stages may add logging, scheduling, daemon mode, and packaging. None of those features should be assumed available in this release.

## Development

Install JDK 25 and follow the [installation guide](docs/installation.md), then run `./mvnw clean verify`. `./mvnw package` also creates portable `tar.gz` and `zip` distribution archives under `target/`.

## License and independence

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026 Proactive Idea. Author: Jenny Cabrera Varona.

jCloudflareDDNS is an independent project and is not affiliated with or endorsed by Cloudflare, Inc.
