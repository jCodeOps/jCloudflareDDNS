# FreeBSD packaging

FreeBSD is the first platform validation target for jCloudflareDDNS.

No native executable, service script, or daemon integration exists yet. The project provides a portable distribution archive, a manual installer, and a development FreeBSD `.pkg` builder; all require Java 25. Future packaging must preserve portable paths, explicit permissions, API Token isolation, and the CLI's exit-code contract.

The current manual installation procedure is documented in [`docs/installation.md`](../../docs/installation.md). A future FreeBSD package may provide an installed launcher and fixed filesystem layout, but it must not require Linux utilities or systemd.

The Stage 9 distribution archive includes a POSIX launcher under `bin/`. It is suitable for direct testing on FreeBSD and Linux without relying on GNU-specific shell features.

Stage 10 adds `install.sh` for manual installation. It requires root, accepts an optional prefix, refuses to overwrite existing files, and installs no service definition. Example:

```sh
sh packaging/freebsd/install.sh jcloudflareddns-0.1.0-SNAPSHOT-distribution.tar.gz /usr/local
```

Stage 12 adds `create-package.sh`, which creates a FreeBSD `.pkg` archive from the distribution. It requires the FreeBSD `pkg` command and does not install anything or create a service:

```sh
sh packaging/freebsd/create-package.sh \
    jcloudflareddns-0.1.0-SNAPSHOT-distribution.tar.gz packages
pkg add packages/jcloudflareddns-0.1.0.pkg
```

The package requires Java 25, installs the launcher under `/usr/local/bin`, and keeps the versioned application under `/usr/local/share`.
Both FreeBSD scripts verify that the archive has the expected single-root layout before extracting it.
