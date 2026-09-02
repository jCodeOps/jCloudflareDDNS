# Changelog

All notable changes to jCloudflareDDNS will be documented here.

## 0.1.1 - 2026-09-02

- Added an opt-in FreeBSD `rc.d` supervisor with an unprivileged service
  account, separated token file, bounded execution interval, syslog output,
  and package-managed configuration permissions.
- Updated the Maven Wrapper, build plugins, SBOM tooling, and GitHub Actions to
  their reviewed current versions.

## 0.1.0 - 2026-09-02

- Rejected negative IPv4 octets during public IP and DNS record validation.
- Clarified release integrity checks and documented the complete DNS record
  reconciliation behavior.
- Defaulted Cloudflare DNS updates to proxied records unless configuration explicitly disables it.
- Reconciled proxied Cloudflare records with their required automatic TTL.
- Added a FreeBSD Ports candidate for the portable archive.

- Aligned repository rules and release documentation with the implemented CLI.
- Added weekly Dependabot monitoring for Maven dependencies and GitHub Actions.
- Added reproducible CycloneDX JSON and XML SBOM files to portable distributions.
- Added runtime dependency attribution notices to portable distributions.
- Added safe `--verbose` and `--debug` CLI diagnostics, plus public bug-reporting information.
- Added a controlled, non-sensitive error boundary for unexpected CLI failures.
- Added a Linux/JDK 25 GitHub Actions verification workflow with a portable distribution smoke test.
- Normalized DNS names independently of the system locale.
- Documented portable distribution validation on Debian 13.
- Added direct CLI coverage for matching and differing DNS check results.
- Added bounded retries for transient read-only Cloudflare API failures.
- Bounded Cloudflare and public IP HTTP response bodies to limit memory use.
- Added bounded Cloudflare API pagination for zone and DNS record lookups.
- Added ordered public IP providers for IPv4 and IPv6.
- Added bounded retries for transient public IP provider failures.
- Made the retry budget explicit and immutable for future execution policies.
- Hardened YAML secret-key detection for nested configuration values.
- Documented the complete non-secret configuration schema and retry contract.
- Added multi-profile configuration inheritance and `--profile` selection.
- Added sequential `validate --all` profile validation with aggregated results.
- Added sequential `check --all` and `update --all` profile execution.
- Added bounded parallel execution for `check --all` and `update --all`.
- Added an 8-worker recommended concurrency limit with warnings for higher values.
- Added `config init` to create a secure non-secret starter configuration.
- Added per-configuration execution locking to prevent overlapping runs.
- Added portable manual installation guidance, with FreeBSD as the first validation target.
- Added portable `tar.gz` and `zip` distribution archives with a POSIX launcher.
- Added a non-destructive, prefix-configurable manual installer for FreeBSD.
- Added a FreeBSD `.pkg` archive builder without service integration.
- Established FreeBSD-first platform validation guidance and clarified the current CLI status.
- Established the Stage 0 Maven, documentation, metadata, and test baseline.
