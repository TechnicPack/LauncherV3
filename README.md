LauncherV3

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
