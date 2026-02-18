# Contributing to RuneTale

Thanks for contributing to RuneTale. This guide covers local setup, build commands, and coding standards.

## Contributor License Agreement (CLA)

By submitting any contribution (code, documentation, assets, or other materials) to this repository, you agree that:
- You have the legal right to submit the contribution.
- You grant RuneTale maintainers a non-exclusive, irrevocable, worldwide, perpetual, royalty-free license to use, reproduce, modify, adapt, publish, distribute, sublicense, and otherwise make the contribution available as part of this project and related distributions.
- You understand that contributions are public and may be redistributed under this project's license terms.

If you do not agree to these terms, please do not submit a contribution.

## Development Environment

### Prerequisites
- Git
- JDK 25 (Temurin recommended)
- A Java IDE (IntelliJ IDEA recommended)

### Initial Setup
1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd RuneTale
   ```
2. Verify Java and Gradle wrapper:
   ```bash
   java -version
   ./gradlew --version
   ```

### Notes
- Use the Gradle wrapper (`./gradlew`) instead of a system Gradle installation.
- The project is a multi-module Gradle build.

## Building and Testing

### Build everything
```bash
./gradlew build
```

### Run all tests
```bash
./gradlew test
```

### Build one module
```bash
./gradlew :plugins:<module>:build
```

### Test one module
```bash
./gradlew :plugins:<module>:test
```

### Clean build outputs
```bash
./gradlew clean
```

## Coding Standards

### Java Style
- Use 4-space indentation.
- Keep opening braces on the same line.
- Always use braces, including single-line `if` blocks.
- Do not use wildcard imports.

### Import Order
Use this order for imports:
1. `com.hypixel.hytale.*`
2. `org.runetale.*`
3. `java.*` / `javax.*`

### Strings and Locale
- Use `Locale.ROOT` for case conversion and locale-sensitive formatting:
  - `toLowerCase(Locale.ROOT)`
  - `toUpperCase(Locale.ROOT)`
  - `String.format(Locale.ROOT, ...)`

### Nullability and API Contracts
- Annotate public method parameters and return types with `@Nonnull` / `@Nullable`.
- Prefer explicit success/failure returns for expected validation outcomes.

### Naming Conventions
- Use clear, role-based suffixes such as:
  - `*Service`
  - `*System`
  - `*Command`
  - `*Event`
  - `*Result`
- Use `fromString()` for strict parsing (throws on invalid input).
- Use `tryParse*()` for forgiving parsing (returns `null` on invalid input).

### Logging
- Prefer structured logging with appropriate levels (`info`, `warning`, `severe`, etc.).
- Keep log messages actionable and concise.

### Testing
- Use JUnit 5.
- Name test classes with `*Test`.
- Test one behavior per test method.
- Prefer direct tests of pure logic without unnecessary mocks.

## Before Opening a PR

Run at least:
```bash
./gradlew build test
```

Make sure changes are scoped, readable, and consistent with the standards above.
