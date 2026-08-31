# jCloudflareDDNS

> Early development — Stage 1 CLI foundation. Functional DDNS behavior is not implemented yet.

jCloudflareDDNS is planned as a secure, lightweight, cross-platform Dynamic DNS client for Cloudflare, written in modern Java. It will focus on updating Cloudflare DNS records safely when a host's public IP changes, while keeping configuration explicit and credentials protected.

The project is intentionally CLI-oriented and will begin with one-shot execution. Linux and FreeBSD are first-class target operating systems, with native-image-friendly design and planned packaging for both platforms, including systemd and Debian-oriented Linux packaging.

## Technical direction

- Java 25 and Maven
- Java standard library first, including `java.net.http.HttpClient`
- GraalVM Native Image compatibility as a first-class requirement
- Cloudflare API Tokens with least privilege; Global API Keys will not be used in new functionality
- No Spring, web server, database, or dependency-injection framework

## CLI foundation

The current CLI exposes `--help`, `--version`, and the commands `check`, `update`, and `validate`. `validate` checks a local YAML configuration file, and `check` resolves the public IPv4 address and queries the configured Cloudflare record. `update` remains a placeholder; no command updates DNS records automatically.

An example non-secret configuration is:

```yaml
zone: example.com
record: host.example.com
ttl: 300
proxied: false
tokenEnv: CLOUDFLARE_API_TOKEN
ipProviderUrl: https://api.ipify.org
```

The configuration can be validated with `jcloudflareddns validate --config config.yml` when running the CLI. The `tokenEnv` value names an environment variable for future stages; Stage 2 does not read or use that variable. Never place a token in the YAML file.

Future stages may add automatic DNS updates, IPv6 support, logging, scheduling, and packaging. None of those features should be assumed available in this release.

## Development

Install JDK 25, then run `./mvnw clean verify`. The minimal entry point can be checked with `java -cp target/classes com.proactiveidea.jcloudflareddns.JCloudflareDdnsApplication`.

## License and independence

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026 Proactive Idea. Author: Jenny Cabrera Varona.

jCloudflareDDNS is an independent project and is not affiliated with or endorsed by Cloudflare, Inc.
