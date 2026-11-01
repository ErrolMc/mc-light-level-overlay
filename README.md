# Light Level Overlay

A client-side Fabric mod for Minecraft 26.3. Press **H** to mark every block around you where monsters can spawn, either as crosses or as light level numbers. It also shows how dark water is, so you can light a river or lake until drowned stop spawning in it.

## What the marks mean

**Crosses** mode puts a cross on every spot where monsters can spawn:

| Colour | Meaning |
|---|---|
| Red | Monsters can spawn here at any time (block light 0, no daylight) |
| Yellow | Block light 0, but open to the sky: monsters only spawn here at night or in a thunderstorm |

**Numbers** mode shows the block light level on every block you can stand on:

| Colour | Meaning |
|---|---|
| Red | 0, whether it's under the open sky or not, and even on blocks nothing spawns on (glass, leaves...) |
| White | Lit (1 or more) |
| Blue | Water that is lit enough for drowned not to spawn |

Land marks use vanilla's own spawn checks, so a cross means a zombie, skeleton or creeper really could spawn there. In the Overworld that's anywhere with block light 0; in the Nether it's block light 7 or less, and in the End block light 0.

## Lighting water against drowned

Drowned spawn in any water block that has water under it, at any depth, not only at the bottom. So the mark on the water surface is for the darkest spot in the water below it:

- In crosses mode, a cross on the water means there is still somewhere in that column dark enough for drowned.
- In numbers mode, the number is the light level of that darkest spot (blue once it's at least 1).

Put glowstone or sea lanterns on the river bed and keep going until the crosses on the water are gone. The marks update a few times a second, so you can watch them disappear as you place each block. Light fades by one level per block through water, so a light source covers roughly 14 blocks out, less in deep water where the top of the column is far from the bed.

The water marks don't check the biome or depth rules for where drowned spawn naturally (rivers and oceans, or deep water elsewhere), so they also show on ponds where drowned wouldn't appear.

## Keys

- **H**: overlay on / off.
- **Switch crosses / numbers**: unbound at first; set it under Options → Controls → Key Binds → Light Level Overlay (H lives there too).

If you have MiniHUD installed, its own main toggle is also H by default, so one press toggles both. Rebind one of them (MiniHUD: H + C → Generic Hotkeys → mainRenderingToggle).

## Options

Open them with **Mod Menu → Light Level Overlay → Configure** (needs Cloth Config), or edit `config/light-level-overlay.json`.

- **Show**: Crosses or Numbers
- **Show water**: marks on water where drowned can spawn (on by default)
- **Show night-only spawns**: the yellow crosses; turn off to see only the red, always-dark spots (numbers mode shows every 0 in red either way)
- **Numbers face you**: on by default, numbers turn to read upright from wherever you look; off keeps them fixed with the top of the digits to the north, like a map
- **Range** / **Height range**: how far out and how far up and down the overlay reaches (16 and 12 blocks by default)

## Install

1. Build it (below) or take `light-level-overlay-<version>.jar` from `build/libs` (not the `-sources` jar).
2. Put it in the instance's `mods` folder next to Fabric API. Mod Menu and Cloth Config are optional and only needed for the settings screen.
3. Restart the game.

Requires Minecraft 26.3, Fabric Loader 0.20.1+, Fabric API and Java 25. Works on any server: it only reads the light levels your client already knows.

## Development (VS Code)

1. Install JDK 25 (this machine has it at `C:\Program Files\Java\jdk-25.0.5.0.2`; `.vscode/settings.json` points there).
2. Open the folder in VS Code and install the recommended extensions when asked (Extension Pack for Java, Gradle for Java).
3. Run **Terminal → Run Task → Decompile Minecraft sources** once, then **Java: Clean Java Language Server Workspace** from the command palette.
4. Press **F5** (launch config "Minecraft Client") to start a dev client through Gradle with the debugger attached.

Gradle tasks, from VS Code's task menu or a terminal:

```
.\gradlew.bat build               # compile, unit tests, jar in build\libs
.\gradlew.bat runClient           # dev client without the debugger
.\gradlew.bat runClientGameTest   # in-game test (muted): builds a dark room and a water basin, presses H, checks the marks
.\gradlew.bat genSources          # decompiled Minecraft sources for the editor
```

The icon is drawn by `python -I tools/make-icon.py`.

## Licence

MIT
