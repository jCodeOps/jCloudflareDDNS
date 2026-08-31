# FreeBSD packaging

FreeBSD is the first platform validation target for jCloudflareDDNS.

No native package, service script, or daemon integration exists yet. Until packaging is introduced, run the one-shot CLI directly with Java 25 and the Maven Wrapper. Future packaging must preserve portable paths, explicit permissions, API Token isolation, and the CLI's exit-code contract.

The current manual installation procedure is documented in [`docs/installation.md`](../../docs/installation.md). A future FreeBSD package may provide an installed launcher and fixed filesystem layout, but it must not require Linux utilities or systemd.
