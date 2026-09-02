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

The candidate includes an optional `rc.d` supervisor. Java remains a one-shot
process: FreeBSD `daemon(8)` runs `update --apply --all`, waits for it to exit,
and starts the next execution after the configured interval. The service is
disabled by default and runs as the dedicated `jcloudflareddns` account.

The upstream Ports `UIDs` and `GIDs` files must reserve the same numeric ID for
that account. ID 396 was free when this candidate was prepared and must be
reconfirmed immediately before submission. The proposed entries are:

```text
jcloudflareddns:*:396:396::0:0:jCloudflareDDNS service:/nonexistent:/usr/sbin/nologin
jcloudflareddns:*:396:
```
