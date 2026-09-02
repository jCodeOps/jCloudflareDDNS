# FreeBSD Ports Candidate

This directory contains the jCloudflareDDNS 0.1.0 Ports candidate. It is not a
replacement for the manual installer or local package builder.

The candidate packages the versioned portable release archive without invoking
Maven or accessing the network after the Ports `fetch` phase. Before submission,
validate it in clean FreeBSD jails with the 0.1.0 distfile preloaded in `DISTDIR`,
then publish the matching immutable upstream release asset and generate
`distinfo` with the Ports framework.

The intended final location in the Ports Collection is
`dns/jcloudflareddns`.
