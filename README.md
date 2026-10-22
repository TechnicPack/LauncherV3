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
| `./gradlew previewMemoryWarning` | Preview the memory warning without generating memory pressure. |

Signing during `package` is enabled when `CERT_KEYSTORE` is set and also requires `CERT_ALIAS`,
`CERT_STOREPASS`, and `CERT_KEYPASS`. Setting `SENTRY_AUTH_TOKEN` enables Sentry source bundle
upload. Keep credentials and generated files out of version control.

### Memory warning preview

```sh
# Open the real dialog with a simulated 4 GiB heap and 2 GiB available.
./gradlew previewMemoryWarning

# Supply a different simulated heap and available memory, both in MiB.
./gradlew previewMemoryWarning --args="8192 4096"

# Healthy and unknown readings should finish without displaying a warning.
./gradlew previewMemoryWarning --args="4096 8192"
./gradlew previewMemoryWarning --args="4096 -1"
```

The preview reports **Cancel** or **Launch anyway** in the terminal. It does not generate memory
pressure, load or save launcher settings, or start Minecraft. It uses the production warning
dialog; the preview script itself is development-only and is not included in the launcher JAR.

### Native architecture checks without a Mac

```sh
./gradlew test --tests net.technicpack.utilslib.OSUtilsTest
```

These tests expose synthetic macOS-style functions through JNA and exercise the native calling
interface, ARM64/Intel results, and unavailable or malformed query results. They run locally
without a Mac or C compiler. They do **not** verify Apple's actual `sysctl` implementation or
Rosetta; that still requires an Apple Silicon Mac, such as a suitable hosted macOS runner.

### Bundled HTTPS trust and release refresh

Launcher-process HTTPS uses only the Mozilla roots in `src/main/resources/tls/cacert.pem`,
filtered by the bundled Mozilla OneCRL snapshot in `onecrl.json`. Both URLConnection (including
Sentry) and the Google HTTP authentication clients use this policy. Authentication uses the
JDK-backed `NetHttpTransport` to support older Java 8 runtimes. The policy is installed before services
start; invalid or missing TLS inputs stop startup rather than falling back to Java's trust store.
Private/company CAs installed only in Java or OS trust stores, including a store selected through
`javax.net.ssl.trustStore`, are intentionally not trusted.

Before each release, explicitly refresh and review all three versioned inputs:

```sh
python3 scripts/refresh-tls-data.py
python3 scripts/refresh-tls-data.py --check
python3 -B scripts/test_refresh_tls_data.py
BUILD_NUMBER=0 ./gradlew check shadowJar
```

The standard-library-only Python utility downloads over verified HTTPS, verifies curl's published
PEM SHA-256, validates OneCRL records and pagination, and stages files only after validation succeeds.
`--check` works offline and never modifies the files. Ordinary builds use the versioned snapshot;
Gradle does not download trust data. `sources.json` records UTC retrieval time, source URLs, the
collection ETag, counts and hashes of the exact shipped bytes. These checks/provenance do not
constitute independent cryptographic authentication. A refresh interrupted during file replacement
must not be released: rerun `--check` and complete a successful refresh.

Release verification must execute the packaged policy on **legacy Java 8u202 and Java 25**.
CI runs this smoke before uploading artifacts, using a checksum-pinned 8u202 runtime.
A current Java 8 update alone is insufficient: it contains APIs absent from older installations.
This legacy runtime is only an offline compatibility-test fixture, not a recommended runtime or a
new minimum Java requirement. With `JAVA25_HOME` and `JAVA8_HOME` pointing to Java 25 and 8u202:

```sh
"$JAVA25_HOME/bin/javac" --release 8 -cp build/libs/launcher-4.0-0.jar \
  -d build/tls-smoke \
  src/test/java/net/technicpack/utilslib/TlsTestFixture.java \
  src/test/java/net/technicpack/utilslib/TlsSmoke.java
"$JAVA8_HOME/bin/java" \
  -cp build/tls-smoke:src/test/resources:build/libs/launcher-4.0-0.jar \
  net.technicpack.utilslib.TlsSmoke
"$JAVA25_HOME/bin/java" \
  -cp build/tls-smoke:src/test/resources:build/libs/launcher-4.0-0.jar \
  net.technicpack.utilslib.TlsSmoke
```

Use `;` instead of `:` as the classpath separator on Windows. The smoke uses loopback HTTPS,
loads production classes/data from the shadow JAR, checks both transports and startup failure,
and exits nonzero on failure. A `--release 8` compile alone is not Java 8 runtime proof.
If `DISPLAY` points at a nonexistent X server in a headless environment, unset it for Gradle tests.

The test-only TLS fixtures use RSA-2048/SHA-256, normal validity from 2020-01-01 to 2120-01-01
and an expired leaf valid from 2000-01-01 to 2001-01-01. Only the test server's leaf private key
is stored; CA private keys were not retained. Twelve public eMudhra TLS certificates from
[Mozilla bug 2083449](https://bugzilla.mozilla.org/show_bug.cgi?id=2083449) are regression evidence,
not trust anchors; source attachment IDs and certificate fingerprints are recorded in
`TlsTestFixture.java`. Tests must continue to reject every one against the current bundled feed:
missing coverage is a security-data release blocker, not a reason to remove a fixture.
Neither these certificates nor the test server key belong in the shipped JAR.

The snapshot remains enforced indefinitely; it is neither refreshed nor disabled based on age at
runtime. Newly discovered revocations require a launcher update. This protects against the bundled
known revocations, not all website/leaf revocations. It does not import all Firefox trust-policy
constraints ([curl export limitations](https://curl.se/docs/caextract.html)), repair other old-Java
vulnerabilities, or change the separate Minecraft JVM's TLS configuration.

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
