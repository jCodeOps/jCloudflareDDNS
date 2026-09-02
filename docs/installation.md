# Installation

## Current status

No native installer or operating-system package is published yet. The supported installation method during early development is a source checkout built with JDK 25 and the Maven Wrapper. The application remains a one-shot CLI; the FreeBSD Ports candidate adds an optional operating-system supervisor.

The `package` goal also produces a portable distribution archive. It contains the application, runtime dependencies, and a POSIX launcher; it does not install files into system directories.

## FreeBSD

FreeBSD is the first platform validation target. Install OpenJDK 25 using the system's supported package method, then build the project:

```sh
git clone https://github.com/jCodeOps/jCloudflareDDNS.git
cd jCloudflareDDNS
./mvnw clean verify
./mvnw package
```

After extracting the generated `target/jcloudflareddns-*-distribution.tar.gz`, run `bin/jcloudflareddns --help` from the extracted directory. The launcher uses `JAVA_HOME` when set, otherwise it resolves `java` from `PATH`.

The current distribution has been smoke-tested on FreeBSD 15.1-RELEASE-p3
amd64 with OpenJDK 25.0.3. Stable support will require repeatable validation
across the documented release workflow.

For a published release, verify the archive against the official SHA-256 value before running any installer as root. For a FreeBSD installation under a chosen prefix, run the repository installer as root:

```sh
sh packaging/freebsd/install.sh target/jcloudflareddns-*-distribution.tar.gz /usr/local
```

The installer refuses to overwrite an existing version or launcher. It validates the archive layout, installs it under `share/`, creates a launcher under `bin/`, and creates an empty configuration directory with restrictive permissions. It never creates or copies a configuration file or API Token.

On FreeBSD, a package archive can be created from the distribution with `packaging/freebsd/create-package.sh`. Package creation is separate from installation and does not require Cloudflare credentials. The package is intended for the current development baseline and requires Java 25 on the target host.

### FreeBSD service candidate

The Ports candidate installs an optional `rc.d` service, disabled by default.
Copy and edit the sample files under `/usr/local/etc/jcloudflareddns`, then keep
them owned by `root:jcloudflareddns` with mode `0640`. `config.yml` contains
only non-secret settings; `tokens.env` contains `NAME=value` assignments that
match each profile's `tokenEnv`.

When upgrading from 0.1.0, `pkg` deliberately preserves the ownership of
existing operator files. Before enabling the new service, grant its account
read access without making either file writable by that account:

```sh
chown root:jcloudflareddns \
    /usr/local/etc/jcloudflareddns/config.yml \
    /usr/local/etc/jcloudflareddns/tokens.env
chmod 0640 \
    /usr/local/etc/jcloudflareddns/config.yml \
    /usr/local/etc/jcloudflareddns/tokens.env
```

Use exactly one uppercase `NAME=value` assignment per line in `tokens.env`.
Blank lines and lines beginning with `#` are accepted. Do not use `export`,
shell quoting, command substitution, or variable expansion: the file is parsed
as literal data and is never sourced as a shell script. Never store tokens in
`rc.conf`.

Validate a one-shot execution before enabling the supervisor:

```sh
jcloudflareddns validate \
    --config /usr/local/etc/jcloudflareddns/config.yml --all
sysrc jcloudflareddns_enable=YES
service jcloudflareddns start
```

FreeBSD `daemon(8)` invokes `update --apply --all` as the unprivileged
`jcloudflareddns` user and sends output to syslog. It waits 300 seconds after
each completed execution by default. Set `jcloudflareddns_interval` in
`rc.conf` to a value from 60 through 31536000 seconds to change the interval.
The application's configuration lock remains
the final protection against overlapping invocations. The package installs the
default lock file with service-user ownership while keeping the configuration
directory and credential files non-writable by that account. When overriding
`jcloudflareddns_config`, create its sibling `.jcloudflareddns.lock` file with
mode `0600` and ownership `jcloudflareddns:jcloudflareddns` before starting the
service.

Run the application from the compiled classes as documented in the root README. Keep the checkout in a directory owned by the operator or a dedicated service account. Do not place API Tokens in the checkout.

## Linux

The same source-build procedure is intended for supported Linux distributions. Distribution packages, systemd units, and native executables are future work and are not required for the current CLI.

The portable distribution has been smoke-tested on Debian 13.1 amd64 with
OpenJDK 25.0.4.1. Extract the generated archive and run the included POSIX
launcher as described for FreeBSD; no system service is required.

## Configuration permissions

Configuration files must contain only non-secret settings. Store the API Token in the environment variable named by `tokenEnv`. If a future deployment stores configuration under a system directory, it must use restrictive ownership and permissions and must not embed credentials.

## Native Image

GraalVM Native Image remains a future distribution option. The project avoids operating-system-specific runtime assumptions so a future native build can target FreeBSD and Linux independently.
