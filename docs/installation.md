# Installation

## Current status

No native installer or operating-system package is published yet. The supported installation method during early development is a source checkout built with JDK 25 and the Maven Wrapper. The application is a one-shot CLI and does not require a service manager.

## FreeBSD

FreeBSD is the first platform validation target. Install OpenJDK 25 using the system's supported package method, then build the project:

```sh
git clone https://github.com/jCodeOps/jCloudflareDDNS.git
cd jCloudflareDDNS
./mvnw clean verify
```

Run the application from the compiled classes as documented in the root README. Keep the checkout in a directory owned by the operator or a dedicated service account. Do not place API Tokens in the checkout.

## Linux

The same source-build procedure is intended for supported Linux distributions. Distribution packages, systemd units, and native executables are future work and are not required for the current CLI.

## Configuration permissions

Configuration files must contain only non-secret settings. Store the API Token in the environment variable named by `tokenEnv`. If a future deployment stores configuration under a system directory, it must use restrictive ownership and permissions and must not embed credentials.

## Native Image

GraalVM Native Image remains a future distribution option. The project avoids operating-system-specific runtime assumptions so a future native build can target FreeBSD and Linux independently.
