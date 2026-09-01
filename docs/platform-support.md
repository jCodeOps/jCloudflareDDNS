# Platform Support

## Current policy

jCloudflareDDNS is a plain Java, one-shot CLI. FreeBSD is the first platform validation target, followed by Linux distributions. The application must use Java and portable filesystem and networking APIs rather than operating-system-specific assumptions.

The current distribution was smoke-tested on FreeBSD 15.1-RELEASE-p3 amd64
with OpenJDK 25.0.3. This confirms the portable launcher and local configuration
workflow; it is not yet a stable-release support commitment.

The same distribution was smoke-tested on Debian 13.1 amd64 with OpenJDK
25.0.4.1. This validates the portable JVM workflow on the first Linux target;
it does not introduce Debian packaging or systemd integration.

The normal build requires JDK 25 and uses the Maven Wrapper:

```sh
./mvnw clean verify
```

The same command is expected to work on FreeBSD and supported Linux distributions. The CLI must not require a shell, service manager, database, or web server.

GitHub Actions continuously verifies the Maven build and portable archive on Linux with JDK 25. FreeBSD remains validated on a dedicated VM because it is not available as a GitHub-hosted runner.

## FreeBSD validation checklist

Before a release, validate on a supported FreeBSD VM:

- JDK 25 is available and `./mvnw --version` reports Maven 3.9.11.
- `./mvnw clean verify` passes.
- `--help`, `--version`, `check`, `update`, and `validate` return the documented exit codes.
- Configuration paths work with absolute and relative POSIX paths.
- No API Token or other secret appears in output or error messages.

## Linux validation checklist

Repeat the same checks on representative Linux distributions. Distribution-specific service files and packages are future work and must not be required for one-shot execution.

## Portability constraints

Use `java.nio.file`, `java.net.http`, and other standard Java APIs. Keep platform-specific integration isolated and documented when it becomes necessary. Do not assume Linux-only paths, systemd, GNU utilities, or Linux-specific signals in core application code.
