# Solder Java runtime overrides

A Solder build can select a Mojang Java runtime with the optional `java_runtime` field:

```json
{
  "minecraft": "1.20.1",
  "java": "1.8",
  "java_runtime": "java-runtime-delta",
  "mods": []
}
```

When **Use Mojang Java runtimes** is enabled, this selects the exact component from Mojang's
runtime catalog after Minecraft and version-patch metadata have been resolved. The selected
runtime is used for the game and, when applicable, mod-loader installer processors.
`java-runtime-delta`, for example, selects Mojang's Java 21 runtime, not a pinned patch release.

An absent or `null` field leaves automatic selection unchanged. Disabling **Use Mojang Java
runtimes** leaves the player's manually selected Java installation in use and ignores the
override. The existing `java` and `memory` requirements retain their meaning.

An explicit override must name an available component for the player's OS and architecture.
Unavailable components stop installation rather than silently selecting another Java version.
The existing runtime catalog cache is used when Mojang cannot be reached.
Solder's `SOLDER_ADVANCED_MODE` flag only controls its UI; the launcher does not check that flag.
