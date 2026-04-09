# Contributing

## Prerequisites

- Java 21 (via [sdkman](https://sdkman.io/): `sdk install java 21-tem`)
- Git

## Building

```bash
./gradlew build
```

The shadow JAR is produced at `build/libs/last-active-players-*.jar`.

## Running tests

```bash
./gradlew test
```

JaCoCo coverage report: `build/reports/jacoco/test/html/index.html`

## Mutation tests

```bash
./gradlew pitest
```

Report: `build/reports/pitest/`

## Checkstyle

```bash
./gradlew checkstyleMain checkstyleTest
```

## Development workflow

All feature work follows the TDD cycle:

1. Cut a feature branch from `master`: `git checkout -b feature/short-description`
2. Write a failing test (red).
3. Implement the minimum code to make the test pass (green).
4. Commit atomically: one commit per meaningful unit of behaviour.
5. Open a pull request against `master`.
6. All CI checks (build, test, checkstyle, coverage) must pass.
7. A code review pass is required before merge.

## Class design

This project follows [Elegant Objects](https://www.elegantobjects.org/) by Yegor Bugayenko.
Key rules:

- Classes are named as nouns (what they ARE, not what they DO). No `-er`, `-or`, `-Helper`,
  `-Manager`, `-Service` suffixes.
- Create an interface only when at least one of: (1) a second implementation exists or is
  concretely planned; (2) a test uses a substitutable fake or stub; (3) decorator composition
  is actually applied. Data carriers use Java records. See `docs/adr/005-proportionate-abstraction.md`.
- No getters or setters. Objects expose behaviour, not data.
- All fields are `final`. Objects are immutable by default.
- Constructors only assign; no logic.
- Never return or pass `null`. Use `Optional<T>` for look-up methods that may find nothing.
  Use the Null Object pattern only when the null-object participates in decorator composition
  or is used polymorphically in tests.
- No static methods or utility classes.
- No implementation inheritance (`extends` is only for implementing interfaces or the
  JavaPlugin framework constraint).

## Commit messages

Format: `type: short imperative description`

Types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`

Example: `feat: open session on player join`

Keep each commit self-contained and buildable.
