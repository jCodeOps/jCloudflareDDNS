# Security Policy

## Reporting a vulnerability

Please report suspected vulnerabilities privately to `jcabrerav@proactiveidea.com` with a description, impact, reproduction details, and any proposed mitigation. Do not disclose an issue publicly until it has been assessed and a coordinated response is possible.

Never include real Cloudflare API Tokens, Global API Keys, credentials, or other secrets in GitHub issues, pull requests, logs, or example configurations.

## Secret handling

Use least-privilege Cloudflare API Tokens and keep secrets separate from non-sensitive configuration. Store local secrets in environment variables or ignored local files. The application must never print or log secrets.

Configuration files may contain the name of a token environment variable, such as `CLOUDFLARE_API_TOKEN`, but must not contain the token value itself. Configuration validation rejects common embedded secret fields.

## Dependency maintenance

Dependabot checks Maven dependencies and GitHub Actions weekly. Review each update
with the normal Maven verification and distribution checks before merging.

## Supported versions

Version 0.1.0 is currently supported with security fixes. Development builds
and release candidates are not supported releases. After a future stable
release, only the latest stable release will be supported.
