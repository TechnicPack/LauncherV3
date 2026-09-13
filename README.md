LauncherV3

## Local Solder integration testing

`scripts/local-platform-fixture.py` provides loopback-only Platform discovery for explicitly
allowed packs on a real local Solder. It reads pack names from Solder, advertises that Solder API,
and provides local search, Discover, and news responses. Install/run statistics (including the
launcher's `HEAD` pings) stay in memory in the fixture process. It does not serve build metadata,
mod archives, Minecraft files, or Java runtimes, and does not proxy production Platform traffic.

Start Solder separately, then create a public pack with published builds and a recommended build.
For a minimal launch test, use Minecraft 1.20.1 with no mods. Pack metadata and build definitions
must come from Solder, not the fixture.

From the launcher checkout:

```sh
python3 scripts/local-platform-fixture.py \
  --solder-url http://127.0.0.1:18081/api/ \
  --pack local-runtime-test
```

The fixture listens on `127.0.0.1:18082`. Repeat `--pack` to allow more packs; use `--port` to
change the port. Unknown packs return 404 and upstream Solder failures return 502, not fabricated
metadata. `/health` checks fixture liveness only; `/modpack/local-runtime-test` also checks Solder.

Use a **separate portable launcher directory**. On Linux, with an existing standard launcher
installation supplying the font assets, this creates a fresh profile (the directory must not exist):

```sh
BUILD_NUMBER=0 ./gradlew shadowJar
LOCAL="$HOME/.local/share/technic-local-runtime-test"
mkdir -p "$HOME/.local/share"
mkdir "$LOCAL" || exit 1
mkdir -p "$LOCAL/technic/assets/launcher"
cp build/libs/launcher-4.0-0.jar "$LOCAL/launcher.jar"
printf '%s\n' '{"directory":"portable","useMojangJava":true,"launchToModpacks":true}' \
  > "$LOCAL/technic/settings.json"
cp "$HOME/.technic/assets/launcher/"*.ttf "$LOCAL/technic/assets/launcher/"
java -Djava.net.preferIPv4Stack=true -Dawt.useSystemAAFontSettings=lcd -Dswing.aatext=true \
  -jar "$LOCAL/launcher.jar" -launcheronly \
  -platformApiUrl http://127.0.0.1:18082/ \
  -solderApiUrl http://127.0.0.1:18081/api/ \
  -discover http://127.0.0.1:18082/discover
```

`-launcheronly` skips both launcher replacement and asset updates, so provide the font assets
before starting. Copy only fonts, not `users.json`, `oauth/`, or your normal settings. Sign in
normally in the isolated profile. Use these flags on every launch; without overrides, the
production endpoints remain the defaults.

`-platformApiUrl` controls Platform metadata, search, news, and install/run statistics.
`-solderApiUrl` controls the default public-pack list; individual packs still use the Solder URL
from their Platform metadata. Both accept absolute HTTP(S) roots, normalize a missing trailing
slash, and reject credentials, query strings, and fragments. These options do not redirect
Microsoft/Mojang services, arbitrary mod-download URLs, or launcher error reporting. Use only
local test packs and inspect their download URLs before installing.

After signing in, search for the allowed pack's name or select its link in **Discover**.
Pasting a loopback API URL into the search box is not supported. Select builds under
**Modpack Options**, and verify the game process's Java executable and game log. With Mojang
runtimes disabled, a compatible manually selected Java should be used instead. Restart the
launcher between edits to the same Solder build to avoid its in-memory build cache; do not delete
your normal launcher data. Stop the fixture with Ctrl-C; close the launcher before replacing its
JAR with a new local build.

## Modern Forge and NeoForge installation

Modern loader packs must supply their installer JAR as `bin/modpack.jar`. The launcher reads its
`install_profile.json` before resolving the version, uses the profile-selected version JSON, and then
applies pack patches. It runs client processors during installation in separate JVMs using the selected
game Java runtime; ForgeWrapper is not used. Processors are ordinary local programs, not security-sandboxed code.

- Shared Maven artifacts live under `libraries/` in the configured launcher root. Compatible artifacts
  are copied on demand from the installer's `maven/` entries, the old mixed `cache/`, or `~/.m2/repository`.
  Ordinary acquisition retains its sources.
  A one-time startup migration moves canonical Maven files from `cache/` to `libraries/` using
  SHA-256-verified copy/delete, including matching paired `.sha1` files. Identical destinations are
  deduplicated without rewriting them; differing destinations and their cached originals both remain.
  Partial I/O failures keep the upgrade retryable on the next startup without blocking the launcher.
  FML's `cache/fmllibs`, processor state/work directories, noncanonical paths, and unrelated cache files
  stay untouched, as do pack-local files, installer archives, and `~/.m2`. Only this migration deletes
  old shared-cache sources; on-demand fallbacks remain available. Older launchers may redownload
  moved libraries, and cache files they add after the migration are not bulk-scanned again.
- Declared artifact and output hashes are verified. Inferred processor tools retain `.sha1` sidecars
  for verified cache reuse. Shared library writes and processor sequences use the cache's
  `modern-installer.lock` across launcher instances.
- Modern launch uses a verified, unmodified vanilla copy at `bin/native-launch/<version-id>.jar`.
  This launcher-managed directory is cleaned on full reinstall. Legacy launches retain their existing
  pack-local JAR behavior; modern launch does not create an unused signature-stripped `bin/minecraft.jar`.
- Processors without declared output hashes can reuse successful runs when their file arguments are
  conservatively trackable: whole Maven coordinates and supported whole-token references. SHA-256
  receipts under `cache/processor-state/` bind the installer recipe, selected Java runtime, vanilla
  client, processor classpath, and referenced files. A baseline is recorded only after a successful
  run produces or rewrites a referenced artifact; existing files alone never authorize reuse.
  Changed or missing files invalidate reuse without making argument paths deletion targets.
  Untrackable arguments and processors that produce no observed artifact continue to run.
- Cached libraries alone do not guarantee an offline recipe. Cold or uncacheable runs of upstream
  tasks such as `DOWNLOAD_MOJMAPS` still fetch Mojang metadata.

### Installer verification

`./gradlew check` runs the deterministic tests and repository checks. The opt-in corpus test also
parses every installer, builds its artifact plan, and resolves client data, arguments and outputs:

```sh
FORGE_INSTALLER_MIRROR=/path/to/forge/downloads \
NEOFORGE_INSTALLER_MIRROR=/path/to/neoforge/downloads \
./gradlew test --tests ModernInstallerCorpusTest.parsesEveryInstaller
```

A supplied mirror directory must contain the required historical fixtures; missing fixtures fail
rather than silently reducing coverage. This corpus check does not execute third-party processors.
