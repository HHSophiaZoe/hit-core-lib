# Shared-library working rules

These instructions apply to this repository and its starters.

## Discover and reuse

- Read [docs/starters.md](docs/starters.md) before adding infrastructure or choosing
  a starter. Inspect existing implementations and callers before changing a public API.
- Generic utilities belong in `core-common-starter`; Spring, JPA, cache, messaging,
  storage and WebSocket mechanisms belong in their corresponding starters.
- Keep application workflows and business policy in consumers. Library APIs expose reusable
  mechanisms with explicit inputs and documented defaults.
- Reuse existing utility methods and base abstractions instead of adding duplicates.
  Keep documentation in `docs/starters.md` consistent with public behavior changes.

## Starter integration

- Public contracts depend only on declared library dependencies. Consumers provide their
  application-specific data and policy through explicit contracts.
- Before adding a capability, define its responsibility, minimal contract, dependencies and
  consumer usage. Derive the mechanism from a real need without encoding one caller's workflow
  or inventing extension points for hypothetical requirements.
- Verify new capabilities in an isolated consumer/context using public APIs and declared
  dependencies only. Existing application tests supplement, but do not replace, this check.
- The parent manages dependency/plugin versions; each starter declares the dependencies
  it actually uses. Do not make every consumer inherit unrelated runtime dependencies.
- Register auto-configuration through
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  Consumers should not need to component-scan the entire `com.hit` package.
- Follow the starter's existing conditional bean/configuration conventions so
  applications can supply their own implementation where supported.
- Follow the existing dependency policy: Java 25, Spring Boot 4, Jackson 3 and Log4j2.
  Do not reintroduce explicitly banned legacy dependencies; inspect the POM/enforcer.

## Verification

- Check Java and Maven against the POM requirements. From the repository root, use
  `mvn -pl <starter> -am test`; use a checked-in Maven Wrapper when available.
- Verify affected starters and inspect known consumers for public API compatibility.

## Writing

- Write usage documentation in Vietnamese; agent instructions may use English.
- Before editing, identify the document's audience and scope. Keep consumer setup in
  consumer documentation and design requirements in contributor instructions.
- Review added prose before completion: retain only accurate, actionable information
  needed by that reader. Remove conversational context, repetition and unrelated names;
  rewrite or remove misplaced content rather than appending explanations.
