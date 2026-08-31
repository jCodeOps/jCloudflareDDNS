# Contributing

## Prerequisites

- JDK 25
- The included Maven Wrapper (`./mvnw`)

Run the baseline with `./mvnw clean verify`. Keep Java code under the project package, use four-space indentation, and write all code, documentation, configuration names, logs, commits, and CLI text in English.

Use feature branches and focused imperative commit messages. Behavioral changes must include automated tests. Do not commit tokens, credentials, local configuration, generated output, or IDE metadata. Keep the project plain Java and compatible with GraalVM Native Image, Linux, and FreeBSD. Do not add features outside the active Stage without agreement.

Pull requests should explain the change, list verification commands and results, and identify security or configuration implications.
