# FreeBSD packaging

FreeBSD is the first platform validation target for jCloudflareDDNS.

No native package, service script, or daemon integration exists yet. Until packaging is introduced, run the one-shot CLI directly with Java 25 and the Maven Wrapper. Future packaging must preserve portable paths, explicit permissions, API Token isolation, and the CLI's exit-code contract.

The current manual installation procedure is documented in [`docs/installation.md`](../../docs/installation.md). A future FreeBSD package may provide an installed launcher and fixed filesystem layout, but it must not require Linux utilities or systemd.

The Stage 9 distribution archive includes a POSIX launcher under `bin/`. It is suitable for direct testing on FreeBSD and Linux without relying on GNU-specific shell features.

Stage 10 adds `install.sh` for manual installation. It requires root, accepts an optional prefix, refuses to overwrite existing files, and installs no service definition. Example:

```sh
sh packaging/freebsd/install.sh jcloudflareddns-0.1.0-SNAPSHOT-distribution.tar.gz /usr/local
```
