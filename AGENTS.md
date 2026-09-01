# Repository Guidelines

## Permanent Project Rules

- Target Java 25 and Maven; use the Maven Wrapper for reproducible commands.
- Keep the application plain Java and CLI-oriented: no Spring Boot, Spring Framework, web server, database, or dependency-injection framework.
- Build one-shot CLI behavior first. Do not add Cloudflare API calls, DDNS logic, public-IP resolution, scheduling, daemons, packaging implementations, or other features outside the current Stage.
- GraalVM Native Image compatibility is required. Prefer standard Java APIs, immutable data, records, and minimal, non-reflection-heavy dependencies.
- Linux and FreeBSD are first-class targets; keep filesystem, process, and networking assumptions portable.
- Cloudflare API Tokens are the supported authentication mechanism. Never use Global API Keys in new functionality.
- Keep configuration and secrets separate. Never commit, print, expose, or log API tokens or other secrets.
- Review and update `THIRD-PARTY-NOTICES.md` when changing packaged runtime dependencies.
- All code, comments, documentation, configuration names, logs, commits, and user-facing CLI text must be in English.

## Structure and Quality

Production code is under `src/main/java/com/proactiveidea/jcloudflareddns/`, tests mirror it under `src/test/java/`, documentation belongs in `docs/`, and future packaging placeholders belong under `packaging/`. Use four-space indentation and conventional Java names. Tests are required for meaningful behavior, and exceptions must not be silently swallowed.

Before considering work complete, run `./mvnw clean verify`, perform relevant checks, inspect `git diff`, and review `git status`. Keep commits focused with concise imperative messages and describe behavior, tests, and security/configuration impact in pull requests.
