LauncherV3

## Modern Forge and NeoForge installation

Modern loader packs must supply their installer JAR as `bin/modpack.jar`. The launcher reads its
`install_profile.json` before resolving the version, uses the profile-selected version JSON, and then
applies pack patches. It runs client processors during installation in separate JVMs using the selected
game Java runtime; ForgeWrapper is not used. Processors are ordinary local programs, not security-sandboxed code.

- Shared Maven artifacts live under `libraries/` in the configured launcher root. Compatible artifacts
  are copied on demand from the installer's `maven/` entries, the old mixed `cache/`, or `~/.m2/repository`.
  Original cache and installer files are retained.
- Declared artifact and output hashes are verified. Inferred processor tools retain `.sha1` sidecars
  for verified cache reuse. Shared library writes and processor sequences use the cache's
  `modern-installer.lock` across launcher instances.
- Modern launch uses a verified, unmodified vanilla copy at `bin/native-launch/<version-id>.jar`.
  This launcher-managed directory is cleaned on full reinstall. Legacy launches retain their existing
  pack-local JAR behavior; modern launch does not create an unused signature-stripped `bin/minecraft.jar`.
- Processors without declared outputs run on every installation. Cached libraries and tool sidecars
  do not guarantee a completely offline recipe: upstream tasks such as `DOWNLOAD_MOJMAPS` fetch Mojang
  metadata even when their previous output exists.

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
