# jCloudflareDDNS

> Early development — Stage 0 foundation only. Functional DDNS behavior is not implemented yet.

jCloudflareDDNS is planned as a secure, lightweight, cross-platform Dynamic DNS client for Cloudflare, written in modern Java. It will focus on updating Cloudflare DNS records safely when a host's public IP changes, while keeping configuration explicit and credentials protected.

The project is intentionally CLI-oriented and will begin with one-shot execution. Linux and FreeBSD are first-class target operating systems, with native-image-friendly design and planned packaging for both platforms, including systemd and Debian-oriented Linux packaging.

## Technical direction

- Java 25 and Maven
- Java standard library first, including `java.net.http.HttpClient`
- GraalVM Native Image compatibility as a first-class requirement
- Cloudflare API Tokens with least privilege; Global API Keys will not be used in new functionality
- No Spring, web server, database, or dependency-injection framework

Future stages may add CLI commands, YAML configuration, Cloudflare API integration, logging, scheduling, and packaging. None of those features should be assumed available in this release.

## Development

Install JDK 25, then run `./mvnw clean verify`. The minimal entry point can be checked with `java -cp target/classes com.proactiveidea.jcloudflareddns.JCloudflareDdnsApplication`.

## License and independence

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026 Proactive Idea. Author: Jenny Cabrera Varona.

jCloudflareDDNS is an independent project and is not affiliated with or endorsed by Cloudflare, Inc.
