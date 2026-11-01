# Light Level Overlay

Client-only Fabric mod for Minecraft 26.3: H toggles an overlay that marks spots where monsters can spawn (crosses, or light level numbers), plus water where drowned can spawn. Built for the user's MultiMC instance at `E:\MultiMC\instances\26.3` (mods in `.minecraft\mods`). Set up like `..\inventory-sorter` (same toolchain, config and test conventions).

## Commands

- `.\gradlew.bat build`: compile, run unit tests, jar in `build\libs` (ship `light-level-overlay-<version>.jar`, not `-sources`)
- `.\gradlew.bat runClientGameTest`: real client in a superflat test world; builds a sealed dark room (torch, glass, bottom slab, carpet) and a 4-deep water basin with glowstone under one end, presses H with real input, checks the marks, screenshots both modes into `build\run\clientGameTest\screenshots`. Muted via `src/gametest/.../MuteAudio`.
- `.\gradlew.bat runClient`: dev client (Mod Menu and Cloth Config are on the dev runtime classpath)
- `.\gradlew.bat genSources`: decompiled Minecraft sources, needed for checking 26.x names
- `.\tools\check-deps.ps1 -ModsDir 'E:\MultiMC\instances\26.3\.minecraft\mods' -Staged build\libs\light-level-overlay-<version>.jar` (PowerShell 7): must print `PROBLEMS: 0`

## Gotchas

- Needs JDK 25; `gradle/gradle-daemon-jvm.properties` makes the wrapper use it although `JAVA_HOME` is JDK 17.
- 26.x is unobfuscated Mojang names with renames versus 1.21: entity types are in `EntityTypes` (not `EntityType.ZOMBIE`), `Identifier`, `LightCoordsUtil.FULL_BRIGHT`, `player.sendOverlayMessage`. Check names in the genSources output instead of guessing.
- Loom plugin is `net.fabricmc.fabric-loom` (no remapping): plain `implementation`/`compileOnly`/`localRuntime`.
- Rendering: Fabric's `LevelRenderEvents.COLLECT_SUBMITS` (26.x replaced `WorldRenderEvents`). Its pose stack is identity and positions are camera-relative (`levelState().cameraRenderState.pos`). Crosses go through `submitCustomGeometry` with `RenderTypes.lines()` (vertices need normal + `setLineWidth`, and must be clipped at the near plane like vanilla's `GizmoFeatureRenderer`); numbers through `submitText` with a drop shadow and `POLYGON_OFFSET` (an outline colour was tried: too heavy at a distance). Vanilla `Gizmos` text is billboarded, which is why it isn't used.
- Spawn rules: land uses vanilla `SpawnPlacementTypes.ON_GROUND` for a zombie plus `noCollision` of its spawn box (that is what rules out carpets and bottom slabs); the light rule is in `src/main/.../SpawnDanger` and mirrors `Monster.isDarkEnoughToSpawn` with the dimension's `monster_spawn_block_light_limit` and `monster_spawn_light_level`. Drowned (`Drowned.checkDrownedSpawnRules`): water with water below and no redstone conductor above, at any depth.
- The user's pack has MiniHUD, whose main rendering toggle is also H.
- IDE is VS Code. `.vscode/launch.json` is hand-written; don't run `gradlew vscode`.

## Layout

- `src/main`: `light/SpawnDanger` is pure Java (no Minecraft), unit tested in `src/test`.
- `src/client`: `overlay/Scanner` walks columns around the player every 5 ticks and builds `Mark`s; `overlay/Overlay` holds on/off and the latest marks; `overlay/OverlayRenderer` draws them; `input/OverlayKeys` is H and the (unbound) mode key; `config/` holds the Gson config, Mod Menu entrypoint and Cloth Config screen.
- `src/gametest`: the client game test.

## Working with the user

Terse; wants things done and recommended, not surveyed. Ask before committing, pushing, or touching the MultiMC `mods` folder.
