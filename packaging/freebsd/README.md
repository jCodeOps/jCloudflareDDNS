# FreeBSD packaging

FreeBSD is the first platform validation target for jCloudflareDDNS.

No native executable is provided yet. The project provides a portable distribution archive, a manual installer, a development FreeBSD `.pkg` builder, and an opt-in `rc.d` integration in the Ports candidate; all require Java 25. Packaging preserves portable paths, explicit permissions, API Token isolation, and the CLI's exit-code contract.

The current manual installation procedure is documented in [`docs/installation.md`](../../docs/installation.md). A future FreeBSD package may provide an installed launcher and fixed filesystem layout, but it must not require Linux utilities or systemd.

The distribution archive includes a POSIX launcher under `bin/`. It is suitable for direct testing on FreeBSD and Linux without relying on GNU-specific shell features.

`install.sh` provides manual installation. It requires root, accepts an optional prefix, refuses to overwrite existing files, and installs no service definition. Example:

```sh
sh packaging/freebsd/install.sh jcloudflareddns-0.1.1-distribution.tar.gz /usr/local
```

`create-package.sh` creates a FreeBSD `.pkg` archive from the distribution. It requires the FreeBSD `pkg` command and does not install anything or create a service:

```sh
sh packaging/freebsd/create-package.sh \
    jcloudflareddns-0.1.1-distribution.tar.gz packages
pkg add packages/jcloudflareddns-0.1.1.pkg
```

The package requires Java 25, installs the launcher under `/usr/local/bin`, and keeps the versioned application under `/usr/local/share`.
Both FreeBSD scripts verify that the archive has the expected single-root layout before extracting it.

`ports/dns/jcloudflareddns` contains the 0.1.1 candidate for the FreeBSD Ports
Collection. It packages the portable archive as a Java application and does not
use these development scripts. Its optional service uses FreeBSD `daemon(8)` to
run the one-shot CLI every 300 seconds by default; it does not keep a JVM alive.
