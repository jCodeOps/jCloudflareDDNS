# Release Readiness

## Current status

jCloudflareDDNS 0.1.0-RC1 is an unpublished release candidate. Its FreeBSD
Ports candidate passed `make stage`, `stage-qa`, `check-plist`, `package`,
`install`, `deinstall`, `portlint -A`, and `poudriere testport` on FreeBSD
15.1-RELEASE-p3 with OpenJDK 25. No stable version or published release asset
exists yet.

## Stable release gates

- Use a non-snapshot version and create a public, immutable release asset.
- Generate the final archive only after freezing release documentation and
  record its exact SHA-256 in the matching Ports `distinfo`; do not reuse an
  artifact after release-content changes.
- Publish the archive SHA-256 alongside the release. Verify it before using
  the manual installer, which is intentionally separate from the Ports
  framework checksum verification.
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
- Run `make package` as a normal user for the frozen release artifact.

The manual installer and local package builder are development tools. They are
not substitutes for a submitted FreeBSD Port.
