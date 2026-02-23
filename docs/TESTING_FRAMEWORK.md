# Testing Framework

This repository includes a reusable testing framework for all `:plugins:*` modules.

## Scope

- Included: unit tests and contract tests.
- Included: shared JUnit extensions, ECS test doubles, and common fixture helpers.
- Deferred: full runtime smoke tests and end-to-end gameplay automation.

## Versions

- JUnit 5 BOM: `5.14.3`
- Mockito: `5.21.0`

## Modules

### `:platform:testing-core`

Shared test helpers that are not tied to ECS internals.

- `TestPlayerIds`: deterministic UUID generation for stable test actors.
- `TestConstructors`: reflective constructor helper for private/no-arg fixtures.

### `:platform:testing-junit`

Reusable JUnit annotations and extensions.

- `@ContractTest`: maps to JUnit tag `contract`.
- `@WithDeterministicEnvironment`: forces `Locale.ROOT` and `UTC` during each test.
- `@WithHytaleLogger`: sets `java.util.logging.manager` to Hytale log manager during each test.

### `:platform:testing-ecs`

ECS-facing test doubles for plugin logic.

- `RecordingComponentAccessor<ECS_TYPE>`:
  - records entity/world event dispatches,
  - useful for contract tests that verify event flow.
- `InMemoryComponentAccessor<ECS_TYPE>`:
  - extends recording behavior,
  - adds in-memory component storage,
  - supports factory-based `ensureAndGetComponent(...)` behavior.

## Plugin Test Conventions

All `:plugins:*` modules automatically receive:

- Hytale Server API on test classpaths (`testCompileOnly` + `testRuntimeOnly`).
- Shared framework dependencies:
  - `:platform:testing-core`
  - `:platform:testing-junit`
  - `:platform:testing-ecs`
- Test dependencies:
  - JUnit Jupiter
  - Mockito core + JUnit Jupiter integration
  - AssertJ

## Test Task Model

All Java subprojects expose:

- `test`: unit suite (`contract` tag excluded)
- `contractTest`: contract suite (`contract` tag included)

Root aggregate tasks:

- `unitTest`: runs all module `test` tasks
- `contractTest`: runs all module `contractTest` tasks
- `verifyTests`: runs `unitTest` then `contractTest`

## How To Write Contract Tests

Annotate class or method with `@ContractTest`:

```java
import org.runetale.testing.junit.ContractTest;

@ContractTest
class SkillXpDispatchServiceContractTest {
    // ...
}
```

Use Mockito for behavioral seams and `testing-ecs` accessors for ECS interactions.

## Commands

Run all unit tests:

```bash
./gradlew unitTest
```

Run all contract tests:

```bash
./gradlew contractTest
```

Run both suites:

```bash
./gradlew verifyTests
```

Run one plugin module:

```bash
./gradlew :plugins:skills-api:test
./gradlew :plugins:skills-api:contractTest
./gradlew :plugins:skills:test
./gradlew :plugins:skills:contractTest
./gradlew :plugins:skills-gathering:test
./gradlew :plugins:skills-gathering:contractTest
./gradlew :plugins:skills-crafting:test
./gradlew :plugins:skills-crafting:contractTest
./gradlew :plugins:skills-combat:test
./gradlew :plugins:skills-combat:contractTest
```
