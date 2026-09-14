# Local Solder integration testing

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

**Use `BUILD_NUMBER=999` for local testing so communication with the Technic Platform API works.**
The commands below use this build number and its matching JAR filename.

Use a **separate portable launcher directory**. On Linux, with an existing standard launcher
installation supplying the font assets, this creates a fresh profile (the directory must not exist):

```sh
BUILD_NUMBER=999 ./gradlew shadowJar
LOCAL="$HOME/.local/share/technic-local-runtime-test"
mkdir -p "$HOME/.local/share"
mkdir "$LOCAL" || exit 1
mkdir -p "$LOCAL/technic/assets/launcher"
cp build/libs/launcher-4.0-999.jar "$LOCAL/launcher.jar"
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
