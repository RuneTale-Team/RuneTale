# Testing Framework (PoC)

This repository now includes a generic, reusable testing framework for all plugin modules.

## Scope

- Included: unit tests, contract tests, shared test utilities, ECS fakes.
- Deferred: server smoke tests and end-to-end gameplay automation.

## Modules

### `:platform:testing-core`

Shared test helpers that are not tied to Hytale ECS internals.

- `TestPlayerIds` for deterministic UUID generation
- `TestConstructors` for reflection-based fixture creation

### `:platform:testing-junit`

Reusable JUnit extensions and annotations.

- `@WithDeterministicEnvironment` sets locale/timezone to deterministic defaults
- `@WithHytaleLogger` configures the `java.util.logging.manager` property for Hytale logger compatibility
- `@ContractTest` tags tests with `contract` for selective execution

### `:platform:testing-ecs`

Test doubles for ECS-facing code.

- `RecordingComponentAccessor` captures entity/world event invocations
- `InMemoryComponentAccessor` adds in-memory component storage and factory-backed `ensureAndGetComponent`

## Plugin Conventions

All `:plugins:*` projects automatically receive:

- JUnit 5 (`junit-bom:5.14.2` + Jupiter)
- Mockito (`mockito-core`, `mockito-junit-jupiter`)
- AssertJ (`assertj-core:3.27.7`)
- Test dependencies on `:platform:testing-core`, `:platform:testing-junit`, and `:platform:testing-ecs`
- Hytale server API on test classpaths (`testCompileOnly` + `testRuntimeOnly`)
- JUnit platform enabled for `Test` tasks
- Log manager JVM property set to `com.hypixel.hytale.logger.backend.HytaleLogManager`

## Current Real Contract Coverage (Skills)

- XP dispatch contracts (`SkillXpDispatchServiceContractTest`)
- XP progression contracts (`SkillProgressionServiceContractTest`)
- Dispatch-to-progression pipeline contract (`SkillXpPipelineContractTest`)
- Resource-backed node lookup contracts (`SkillNodeLookupServiceContractTest`)
- Tool requirement contracts (`ToolRequirementEvaluatorContractTest`)
- XP math contracts (`XpServiceContractTest`)

## Running Tests

```bash
./gradlew test
```

Run only Skills tests:

```bash
./gradlew :plugins:skills:test
```

Run only framework module tests:

```bash
./gradlew :platform:testing-core:test :platform:testing-junit:test :platform:testing-ecs:test
```

Run only contract tests:

```bash
./gradlew contractTest
./gradlew :plugins:skills:contractTest
./gradlew :platform:testing-core:contractTest :platform:testing-junit:contractTest :platform:testing-ecs:contractTest
```
