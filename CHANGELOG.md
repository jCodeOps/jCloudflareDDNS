# Changelog

All notable changes to jCloudflareDDNS will be documented here.

## Unreleased

- Added ordered public IP providers for IPv4 and IPv6.
- Added bounded retries for transient public IP provider failures.
- Made the retry budget explicit and immutable for future execution policies.
- Added portable manual installation guidance, with FreeBSD as the first validation target.
- Added portable `tar.gz` and `zip` distribution archives with a POSIX launcher.
- Added a non-destructive, prefix-configurable manual installer for FreeBSD.
- Added a FreeBSD `.pkg` archive builder without service integration.
- Established FreeBSD-first platform validation guidance and clarified the current CLI status.
- Established the Stage 0 Maven, documentation, metadata, and test baseline.
