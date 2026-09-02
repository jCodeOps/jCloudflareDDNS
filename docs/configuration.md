# Configuration

jCloudflareDDNS reads a non-secret YAML configuration file. The API Token is
never stored in this file; `tokenEnv` names the environment variable from which
the one-shot CLI obtains it.

Create a starter file without storing a token:

```sh
jcloudflareddns config init --output config.yml
```

Use `--profile`, `--zone`, `--record`, and `--token-env` to customize the
generated profile. Existing files are never overwritten.

## Fields

```yaml
zone: example.com
record: host.example.com
ttl: 300
proxied: true
tokenEnv: CLOUDFLARE_API_TOKEN
useDefaultIpProviders: true
ipProviderUrls:
  - https://my-company.example/public-ip
ipVersion: ipv4
```

- `zone`: Cloudflare zone name.
- `record`: fully qualified DNS record name inside the zone.
- `ttl`: TTL in seconds, from 1 through 86400 for unproxied records. Cloudflare
  requires `Auto` (API value `1`) for proxied records, so the configured TTL is
  ignored while `proxied` is `true`.
- `proxied`: whether the Cloudflare record is proxied; defaults to `true`.
- `tokenEnv`: uppercase environment variable name containing the API Token.
- `useDefaultIpProviders`: uses the two built-in HTTPS providers for the selected
  IP family when `true` or omitted.
- `ipProviderUrls`: optional ordered HTTPS providers. They are appended after the
  defaults; set `useDefaultIpProviders: false` to use only these URLs.
- `ipVersion`: `ipv4` or `ipv6`; defaults to `ipv4`.

Run validation without making network or Cloudflare requests:

```sh
./mvnw package
tar -xzf target/jcloudflareddns-0.1.0-RC1-distribution.tar.gz
./jcloudflareddns-0.1.0-RC1/bin/jcloudflareddns validate --config config.yml
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

## Multiple profiles

One file may define shared defaults and named profiles:

```yaml
defaults:
  ttl: 300
  proxied: true
  ipVersion: ipv4

profiles:
  home:
    zone: example.com
    record: home.example.com
    tokenEnv: CLOUDFLARE_HOME_TOKEN
  office:
    zone: example.net
    record: office.example.net
    tokenEnv: CLOUDFLARE_OFFICE_TOKEN
    ipVersion: ipv6
```

Run a selected profile with `--profile home`, or validate every profile with
`--all`. Profile values override defaults; omitted values inherit them. A
multi-profile file must name the profile explicitly unless `--all` is used for
validation. Network execution of all profiles supports sequential mode and
bounded parallel mode.

```yaml
execution:
  mode: sequential
```

Use `mode: parallel` with an optional `maxConcurrency` from 1 through 16.
Values from 1 through 8 are recommended. Values from 9 through 16 are allowed
with a warning because they may increase resource usage. When omitted, parallel
mode uses two workers. Sequential mode does not need a `maxConcurrency` value.

Each command acquires a lock next to the configuration file for the duration
of its execution. A second invocation using the same configuration exits with
a controlled error instead of overlapping the first run. The lock file is
`<config>.jcloudflareddns.lock` and contains no credentials.
