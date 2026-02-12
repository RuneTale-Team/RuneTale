# Contract Test Playbook (Non-Test-Engineer Friendly)

This guide explains how to add contract tests in this repo without needing a testing background.

## What Is a Contract Test?

A contract test checks that a class keeps its behavior promises.

Think of it like this:

- Input goes in.
- The system decides what should happen.
- We assert that the right things happened (and wrong things did not).

We use contract tests to protect gameplay logic from regressions.

## Why We Built This

Hytale modding has limited automated runtime test options right now. So we built a fast local testing setup that lets us test most gameplay rules without booting a full server.

In plain English:

- We split "brain" logic from "engine wiring" where possible.
- We use fakes/mocks for engine-facing APIs.
- We verify behavior with deterministic tests.

## What We Already Added

- Shared test modules:
  - `:platform:testing-core`
  - `:platform:testing-junit`
  - `:platform:testing-ecs`
  - `:platform:testing-fixtures`
- Contract test tag: `@ContractTest`
- Dedicated task: `./gradlew contractTest`

## The 5-Step Recipe

1. Pick one behavior contract
- Example: "When XP gain is zero, no feedback is sent."

2. Test the decision point
- Prefer testing a policy/service class first.
- If logic lives inside a system, add a seam (interface) for side effects.

3. Use test doubles
- Mockito mocks for collaborators and side effects.
- `RecordingComponentAccessor` / `InMemoryComponentAccessor` for ECS-like behavior.

4. Assert both positive and negative behavior
- Positive: expected method/event happens.
- Negative: unrelated actions do not happen.

5. Mark and run
- Annotate class with `@ContractTest`.
- Run `./gradlew contractTest`.

## Copy/Paste Template

```java
package org.runetale.example;

import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import static org.mockito.Mockito.*;

@ContractTest
class ExampleContractTest {

    @Test
    void doesExpectedThing() {
        // Arrange
        Dependency dependency = mock(Dependency.class);
        Subject subject = new Subject(dependency);

        // Act
        subject.run();

        // Assert
        verify(dependency).expectedCall();
        verify(dependency, never()).unexpectedCall();
    }
}
```

## Commands You Will Actually Use

```bash
# all tests
./gradlew test

# all contract tests
./gradlew contractTest

# only Skills contract tests
./gradlew :plugins:skills:contractTest
```

## How To Choose The Next Test (Simple Priority Rule)

Pick in this order:

1. High gameplay impact + high change risk
- XP grants, level-up flow, combat XP routing, requirement gates.

2. Multi-branch decision logic
- Many `if` paths means high bug risk.

3. Side effects that players notice
- Cancellation, notifications, toasts, event dispatches.

## Common Gotchas

- Hytale logger setup
  - The build already sets `java.util.logging.manager` for tests.
- Static/global runtime lookups
  - Add a seam interface and inject it. Test the seam wiring with mocks.
- Over-mocking
  - Prefer testing pure policy logic directly, then keep system tests focused on orchestration.

## What "Good" Looks Like

A good contract test class usually has:

- 2-5 focused test methods,
- clear names (`returnsEarlyWhen...`, `dispatchesWhen...`),
- both success and failure-path assertions,
- no unnecessary runtime setup.

If you follow this playbook, future changes are much safer and faster.
