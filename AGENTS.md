# Shared-library working rules

These instructions apply to this repository and its starters. When working inside
the trading-system workspace, also follow the workspace root `AGENTS.md`.

## Discover and reuse

- Read [docs/starters.md](docs/starters.md) before adding infrastructure or choosing
  a starter. Inspect existing implementations and callers before changing a public API.
- Generic utilities belong in `core-common-starter`; Spring, JPA, cache, messaging,
  storage and WebSocket mechanisms belong in their corresponding starters.
- Keep trading-specific models, indicators, broker protocols and application workflows
  outside this library. In the trading workspace, shared trading logic belongs in
  `hit-trading-common`, and provider adapters belong in the services.
- Reuse existing utility methods and base abstractions instead of adding duplicates.
  Keep documentation in `docs/starters.md` consistent with public behavior changes.

## Starter integration

- The parent manages dependency/plugin versions; each starter declares the dependencies
  it actually uses. Do not make every consumer inherit unrelated runtime dependencies.
- Register auto-configuration through
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  Consumers should not need to component-scan the entire `com.hit` package.
- Follow the starter's existing conditional bean/configuration conventions so
  applications can supply their own implementation where supported.
- Follow the existing dependency policy: Java 25, Spring Boot 4, Jackson 3 and Log4j2.
  Do not reintroduce explicitly banned legacy dependencies; inspect the POM/enforcer.

## Verification and repository boundaries

- Check `java -version` and use the checked-in Maven Wrapper. From the trading
  workspace root, verify a starter with
  `./mvnw -f hit-core-lib/pom.xml -pl <starter> -am test`.
- For a standalone checkout, use its checked-in wrapper if available; if absent,
  report the missing build tooling instead of guessing a wrapper or JDK path.
- Verify affected starters; for shared API changes in the trading workspace, also
  inspect and verify affected securities/crypto consumers using the workspace reactor.
- This is a separate Git repository. Commit/merge library changes here before updating
  the parent workspace's submodule pointer; do not include unrelated local changes.
