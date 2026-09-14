# Technic Launcher

Technic Launcher is a desktop application built specifically for use with the Technic Platform,
letting players discover, install, update, and play Minecraft modpacks. This repository (`LauncherV3`)
contains the Java launcher, its Technic Platform and Solder integrations, and platform packaging tools.

## Documentation

- [Solder Java runtime overrides](docs/solder-java-runtime-overrides.md) — select a Mojang Java
  runtime for a Solder build, including validation and manual-Java behavior.
- [Local Solder integration testing](docs/local-solder-integration-testing.md) — connect a local
  Solder instance through the Platform fixture and test with an isolated launcher profile.
- [Modern Forge and NeoForge installation](docs/modern-forge-and-neoforge-installation.md) —
  installer processing, shared libraries, cache migration, offline limitations, and corpus verification.

See the [changelog](CHANGELOG.md) for recent changes and [history](HISTORY.md) for older releases.

## Development

Use the included Gradle wrapper from the repository root. The build uses a **Java 25 toolchain**
with automatic toolchain provisioning configured; production sources target **Java 8**. The Java
runtime needed by a Minecraft modpack is selected separately from the build toolchain.

On Windows, use `gradlew.bat` in place of `./gradlew`.

For local testing, set `BUILD_NUMBER=999` so communication with the Technic Platform API works.
On Windows, set this environment variable before invoking `gradlew.bat`.

```sh
# Run the launcher (requires a graphical desktop)
BUILD_NUMBER=999 ./gradlew run

# Build the standalone JAR
BUILD_NUMBER=999 ./gradlew shadowJar
```

The example produces `build/libs/launcher-4.0-999.jar`. Without `BUILD_NUMBER`, the build defaults to `0`;
use `999` for local testing.
For local Solder testing without using your normal launcher profile, follow the
[isolated-profile guide](docs/local-solder-integration-testing.md).

### Common commands

| Command | Purpose |
| --- | --- |
| `./gradlew test` | Run the JUnit 5 test suite. |
| `./gradlew spotlessApply` | Format Java and Gradle/Kotlin build files. |
| `./gradlew spotlessCheck` | Check formatting without changing files. |
| `./gradlew check` | Run tests, formatting checks, and shadow-JAR service verification. |
| `./gradlew build` | Compile, assemble, and verify the application. |
| `./gradlew package` | Build the shadow JAR, Windows executable, and macOS app ZIP. |

Signing during `package` is enabled when `CERT_KEYSTORE` is set and also requires `CERT_ALIAS`,
`CERT_STOREPASS`, and `CERT_KEYPASS`. Setting `SENTRY_AUTH_TOKEN` enables Sentry source bundle
upload. Keep credentials and generated files out of version control.

## Project layout

- `src/main/java/net/technicpack/` — launcher UI, installation and update flows, and shared utilities.
- `src/main/resources/` — bundled images, translations, metadata, and Windows packaging assets.
- `src/main/app/` — macOS application assets.
- `src/test/java/` — tests mirroring production packages.
- `buildSrc/src/main/kotlin/` — custom Gradle build and packaging logic.
- `scripts/` — development utilities, including the local Platform fixture.
- `docs/` — detailed guides and existing design documents.

## License

The launcher is licensed under the [GNU General Public License, version 3 or later](LICENSE).
See [LICENSE.txt](LICENSE.txt) and [COPYING.txt](COPYING.txt) for the Technic Launcher Core license notice
and bundled license text.
