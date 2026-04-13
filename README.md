# fluidsim

Minimal Java Maven project pinned for reproducible development.
Configured for Java Swing/AWT desktop app development.

## Requirements

- JDK 25
- Use the Maven Wrapper committed in this repository

## Quick Start

1. Verify Java version:
   - `java -version`
2. Run full validation build:
   - `./mvnw -B -ntp clean verify`
3. Run the desktop app:
   - `./mvnw -B -ntp exec:java`

## Reproducibility Rules

- Always run Maven through `./mvnw` (not system `mvn`).
- Build/test locale and timezone are fixed in `pom.xml` (`en_US`, `UTC`, `UTF-8`).
- Toolchain is enforced by Maven Enforcer:
  - Java: `[25,26)`
  - Maven: `[3.9.14,)`

## Useful Commands

- Run tests: `./mvnw -B -ntp test`
- Build artifacts + coverage report: `./mvnw -B -ntp clean verify`
- Run Swing app: `./mvnw -B -ntp exec:java`
- Build runnable jar: `./mvnw -B -ntp clean package && java -jar target/fluidsim-1.0-SNAPSHOT.jar`
- JaCoCo HTML report: `target/site/jacoco/index.html`

## Swing/AWT Notes

- The app entrypoint is `com.benliebkemann.App` and starts UI on the EDT.
- Tests run with `java.awt.headless=true` for CI/local determinism.
- Keep rendering and simulation logic decoupled from Swing components so core logic can be unit-tested.

## CI

A GitHub Actions workflow is included at `.github/workflows/ci.yml` and runs:

- Java 25 setup
- Wrapper-based `clean verify` on every push and pull request
