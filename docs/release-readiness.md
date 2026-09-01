# Release Readiness

## Current status

jCloudflareDDNS 0.1.0-RC1 is an unpublished release candidate. The CLI and
portable archives are validated, but no stable version, release tag, or
published package exists yet.

## Stable release gates

- Use a non-snapshot version and create a public, immutable release asset.
- Run the full Maven verification and distribution smoke test.
- Perform controlled end-to-end Cloudflare tests for IPv4 and IPv6 with
  least-privilege tokens that are never stored in the repository.
- Review documentation, changelog, SBOM, third-party notices, and security
  policy for the exact release version.
- Test the archive on the supported FreeBSD and Linux target matrix.

## FreeBSD Ports gates

- Build from a versioned upstream release asset without network access after
  the Ports `fetch` phase.
- Provide a Ports skeleton with `Makefile`, `distinfo`, `pkg-descr`, plist, and
  any required wrapper or sample configuration files.
- Use the Ports Java framework and install the application below the standard
  Java share directory with a launcher below `${PREFIX}/bin`.
- Keep configuration non-secret; do not install tokens or create a service.
- Pass `make stage`, `stage-qa`, `package`, `install`, `deinstall`, and
  `portlint -A`, then validate with `poudriere testport` in clean jails.

The manual installer and local package builder are development tools. They are
not substitutes for a submitted FreeBSD Port.
