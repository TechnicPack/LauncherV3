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

`CHANGELOG.md` must describe end-user-relevant changes: user-visible features, fixes, security changes, and compatibility impacts. Omit internal refactors, CI/build tooling, dependency maintenance, and automation configuration unless they have a concrete end-user impact; describe that impact rather than the implementation details.

## Promotion & Release Workflow
Use `.github/workflows/gradle.yml` to build and `.github/workflows/promote.yml` to publish a stable release.

1. Ensure user-facing release notes are under `CHANGELOG.md` → `[Unreleased]`, then push the approved commit:
   ```sh
   git push origin master
   gh run list --workflow gradle.yml --branch master --limit 5 \
     --json databaseId,headSha,status,conclusion,number,url
   ```
   Match the run's full `headSha` to the pushed commit. CI builds on `master` pushes, except ignored-path-only
   changes. The launcher build number is the Gradle workflow run number plus `1050`; versions are `v4.0-<build>`.
2. Wait for that exact build to succeed:
   ```sh
   gh run watch <build-run-id> --exit-status --interval 15
   ```
   CI runs tests, formatting checks, packaging, and packaged TLS smoke checks on legacy Java 8 and Java 25.
   Do not promote a failed or unfinished build.
3. Promote the verified run explicitly rather than relying on the latest-successful-build default:
   ```sh
   gh workflow run promote.yml --ref master -f run_id=<build-run-id>
   gh run watch <promotion-run-id> --exit-status --interval 10
   ```
   Use the promotion run ID returned by dispatch. `build_number` is an alternative input; `run_id` takes precedence.
   Promotion validates release notes against the selected build and previous published release, uploads the
   Windows executable, JAR, and macOS ZIP, updates the stable symlink and launcher APIs, notifies Discord,
   creates the GitHub release, registers the Sentry release, and rolls over the changelog on `master`.
   Existing releases cannot be overwritten. Discord and Sentry failures are non-fatal; inspect their step results.
4. Verify the published release and live update metadata:
   ```sh
   gh release view v4.0-<build> --json url,isDraft,isPrerelease,assets
   ```
   Read `https://api.technicpack.net/launcher/version/stable4` and confirm its `build` matches the promoted build
   and its download URLs reference that build. A successful build alone is not a completed deployment.
5. Fast-forward the local checkout to include the automated changelog rollover:
   ```sh
   git pull --ff-only origin master
   ```
   Preserve unrelated local changes. If rollover fails because `master` advanced, inspect the release state
   before retrying anything: artifacts and the release may already be live. Never blindly re-promote or rebase
   release-note boundaries.
