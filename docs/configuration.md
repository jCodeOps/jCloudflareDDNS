# Configuration

jCloudflareDDNS reads a non-secret YAML configuration file. The API Token is
never stored in this file; `tokenEnv` names the environment variable from which
the one-shot CLI obtains it.

## Fields

```yaml
zone: example.com
record: host.example.com
ttl: 300
proxied: false
tokenEnv: CLOUDFLARE_API_TOKEN
useDefaultIpProviders: true
ipProviderUrls:
  - https://my-company.example/public-ip
ipVersion: ipv4
```

- `zone`: Cloudflare zone name.
- `record`: fully qualified DNS record name inside the zone.
- `ttl`: TTL in seconds, from 1 through 86400.
- `proxied`: whether the Cloudflare record is proxied; defaults to `false`.
- `tokenEnv`: uppercase environment variable name containing the API Token.
- `useDefaultIpProviders`: uses the two built-in HTTPS providers for the selected
  IP family when `true` or omitted.
- `ipProviderUrls`: optional ordered HTTPS providers. They are appended after the
  defaults; set `useDefaultIpProviders: false` to use only these URLs.
- `ipVersion`: `ipv4` or `ipv6`; defaults to `ipv4`.

Run validation without making network or Cloudflare requests:

```sh
./mvnw package
tar -xzf target/jcloudflareddns-0.1.0-SNAPSHOT-distribution.tar.gz
./jcloudflareddns-0.1.0-SNAPSHOT/bin/jcloudflareddns validate --config config.yml
```

## Retry behavior

Each execution uses a bounded retry budget: at most three provider requests in
total, at most two attempts per provider, with a 250 ms delay between transient
retries. Network failures and HTTP 5xx responses may be retried. Invalid IP
responses, HTTP 4xx responses, and configuration errors are not retried. A
future system timer should schedule the next one-shot execution rather than
relying on an unbounded in-process loop.

Never put API Tokens, Global API Keys, passwords, or other secrets in YAML,
source code, command arguments, logs, or issue reports.
