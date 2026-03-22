# Repository Guidelines

## Project Structure & Module Organization
`src/main/java/net/technicpack/...` contains the launcher application, UI, install/update flows, and shared utilities. `src/main/resources/` holds bundled images, language files, and launcher metadata. Platform packaging assets live in `src/main/app/` for macOS and `src/main/resources/exe/` for Windows. Tests mirror production packages under `src/test/java/`. Custom Gradle build logic, including packaging tasks, lives in `buildSrc/src/main/kotlin/`.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repository root:

- `./gradlew run` runs `net.technicpack.launcher.LauncherMain` locally.
- `./gradlew spotlessApply` formats Java and Gradle/Kotlin build files.
- `./gradlew spotlessCheck` verifies formatting without changing files.
- `./gradlew test` runs the JUnit 5 suite.
- `./gradlew check` runs tests plus verification such as `verifyShadowJarServices`.
- `./gradlew build` compiles and tests the app.
- `./gradlew package` builds the shadow JAR, Windows `.exe`, and macOS app zip.

The build uses a Java 25 toolchain and compiles production sources for Java 8 compatibility.

## Coding Style & Naming Conventions
Java is the primary language. Follow the existing style: 4-space indentation, same-line braces, and `UpperCamelCase` for types with `lowerCamelCase` for methods and fields. Keep lines at or under the `120` character limit defined in `.editorconfig`. Package names stay rooted at `net.technicpack`. Spotless is the formatting authority for Java and Gradle/Kotlin build logic, so run `./gradlew spotlessApply` instead of hand-formatting large edits.

## Testing Guidelines
Tests use JUnit Jupiter. Add tests in the matching package under `src/test/java/` and name files `*Test.java`. Prefer descriptive method names that state the expected behavior, for example `openFallsBackToXdgOpenWhenDesktopOpenThrows()`. Run `./gradlew test` before opening a PR; run `./gradlew check` when changing packaging, shadow-jar behavior, or build logic.

## Commit & Pull Request Guidelines
Recent history follows Conventional Commit style such as `feat(ui): ...` and `fix(build): ...`; keep using `type(scope): summary`. PRs should include a short problem statement, the chosen fix, verification steps, and screenshots for UI changes. Link related issues when applicable and call out any packaging or release-impacting changes.

## Configuration & Release Notes
`BUILD_NUMBER` controls the launcher version suffix. `SENTRY_AUTH_TOKEN` enables source bundle upload, and signing during `package` requires `CERT_KEYSTORE`, `CERT_ALIAS`, `CERT_STOREPASS`, and `CERT_KEYPASS`. Do not commit secrets or generated artifacts.
