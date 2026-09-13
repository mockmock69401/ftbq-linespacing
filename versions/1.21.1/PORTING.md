# Porting notes — Minecraft 1.21.1

Standalone Gradle build for the 1.21.1 Fabric release of this mod. Package
renamed `com.example.ftbqspacing` → `io.github.mockmock69401.ftbqlinespacing`.

## Toolchain

- Minecraft 1.21.1, official Mojang mappings (same style as the 1.20.1 root build).
- Fabric Loader 0.19.5, Fabric API 0.116.1+1.21.1, Architectury API (fabric) 13.0.8.
- Fabric Loom **1.17.20** — not 1.7.x–1.9.x as originally anticipated. The FTB
  jars published for 1.21.1 today (Sep 2026) were built with a much newer Loom
  and refuse to load under old Loom ("Mod was built with a newer version of
  Loom (1.17.491), you are using Loom (1.7.4)"). Loom 1.12+ also requires
  Gradle 9 (root wrapper is Gradle 8.8), so this build carries its **own**
  wrapper (`versions/1.21.1/gradlew(.bat)` + `gradle/wrapper/*`) pinned to
  Gradle 9.7.1. Root wrapper untouched.
- Loom 1.12+ disables the legacy Mixin annotation processor by default (it
  remaps mixins via TinyRemapper directly and warns that `loom.mixin {}`
  config has no effect). Since the task wants a real
  `ftbq_linespacing.refmap.json` inside the jar (matching the 1.20.1 build's
  shape) and this project's mixins are all `remap = false` anyway, build.gradle
  sets `loom.mixin.useLegacyMixinAp = true` alongside `defaultRefmapName` to
  restore that behaviour. Verified: the built jar contains
  `ftbq_linespacing.refmap.json` (empty mappings/data, expected since every
  mixin here is `remap = false`).
- JDK 21 via Gradle toolchain (`languageVersion = 21`) + the
  `foojay-resolver-convention` settings plugin, since no local JDK 21 was
  found (only 17 and 22) — Gradle downloads one automatically. No
  `org.gradle.java.home` committed anywhere.
- `java { withSourcesJar() }` added.

## Dependencies (CurseMaven, modCompileOnly, transitive = false)

| Mod | CurseForge project | File ID | Version | Notes |
|---|---|---|---|---|
| FTB Library (Fabric) | 438495 | 8858847 | 2101.1.36 | latest 1.21.1 build as of Sep 2026 |
| FTB Quests (Fabric) | 438496 | 8842728 | 2101.1.35 | latest 1.21.1 build as of Sep 2026 |
| FTB Teams (Fabric) | 438497 | 8724781 | 2101.1.11 | latest 1.21.1 build as of Sep 2026 |
| Fabric API | — | — | 0.116.1+1.21.1 | needed transitively (FTB classes reference it) |
| Architectury API (fabric) | — | — | 13.0.8 | needed transitively (FTB uses `dev.architectury.networking.NetworkManager` etc.) |

`fabric.mod.json` pins `ftbquests`/`ftblibrary` to
`>=<verified version> <next-minor>` (e.g. `>=2101.1.35 <2101.2.0`), per the
lower bound actually compiled/verified against.

## Mixin verification (decompiled FTB Quests 2101.1.35 / FTB Library 2101.1.36, mojmap)

Every injection point documented in the root `CLAUDE.md` was re-verified
against the actual bytecode of `ViewQuestPanel.addWidgets` /
`addDescriptionText` / `drawBackground` (via `javap -c -p`, not just the
decompiled source — ordinals are a bytecode-level concept and a
source-level read can be misleading, see the `Button.setPosAndSize` row
below) and against the decompiled source of `ChapterPanel`, `QuestScreen`,
`Chapter`, `ChapterGroup`, `TeamData`, `TextField`, `WidgetLayout`, `Icon`
and friends.

| Target | 1.20.1 (root) | 1.21.1 (verified) | Changed? |
|---|---|---|---|
| `TextField.setSpacing` in `addWidgets`, ordinal 0 (title) | ordinal 0 | ordinal 0 | No |
| `TextField.setSpacing` in `addWidgets`, ordinal 1 (subtitle) | ordinal 1 | ordinal 1 | No |
| `TextField.setSpacing` in `addDescriptionText` (single match) | unique | unique | No |
| `WidgetLayout.Vertical.<init>(III)`, index 1 (paragraph gap) | single match | single match | No — `WidgetLayout` is now an `interface` (was a class) but `Vertical` and the ctor descriptor are unchanged; `VERTICAL` is now an interface field (implicitly `public static final`), same `GETSTATIC` shape |
| `Math.min(II)`, ordinal 0 (icon sizing, untouched) | ordinal 0 | ordinal 0 | No |
| `Math.min(II)`, ordinal 1 (no-description branch) | ordinal 1 | ordinal 1 | No |
| `Math.min(II)`, ordinal 2 (with-description branch) | ordinal 2 | ordinal 2 | No |
| `BlankPanel.setHeight(I)`, ordinal 4 (final, `panelContent`) | ordinal 4 | ordinal 4 | No — 5 total `setHeight` calls in both, same order (tasks, rewards, no-desc branch, with-desc branch, panelContent) |
| `TextField.setPosAndSize`, ordinal 0, index 1 (title Y) | ordinal 0 | ordinal 0 | No |
| `BlankPanel.setPosAndSize`, ordinal 0, index 1 (ContentPanel Y) | ordinal 0 | ordinal 0 | No |
| `BlankPanel.setPosAndSize`, ordinal 3 (`panelText`) | ordinal 3 | ordinal 3 | No |
| `Button.setPosAndSize`, ordinal 0 (close) | ordinal 0 | ordinal 0 | No |
| `Button.setPosAndSize`, ordinal 1 (pin) | ordinal 1 | ordinal 1 | No |
| `Button.setPosAndSize`, ordinal 2 (◄ dependencies arrow) | ordinal 2 | ordinal 2 | **No, but verify-worthy**: 2101.x's `addWidgets` adds two *new* buttons before the arrows (`GotoLinkedQuestButton`, `ViewQuestLinksButton`, part of a new "linked quest" cross-navigation feature). At the **source** level this looks like it should push the arrows to ordinal 4/5. At the **bytecode** level it does not: `javap` shows those two new calls compile as `invokevirtual ViewQuestPanel$GotoLinkedQuestButton.setPosAndSize` / `...$ViewQuestLinksButton.setPosAndSize` (owner = the local variable's declared subtype, since javac records the compile-time static type as the `invokevirtual` owner), not `Button.setPosAndSize`. Mixin's ordinal counting matches on exact bytecode owner, so these two calls don't count in the `Button.setPosAndSize` sequence at all. Confirmed via `javap -c -p` on the decompiled+remapped jar, not by reading source. |
| `Button.setPosAndSize`, ordinal 3 (► dependants arrow) | ordinal 3 | ordinal 3 | No (same reasoning as above) |
| `Icon.draw`, ordinal 0 (window background → `RoundedRect`) | ordinal 0 | ordinal 0 | No |
| `Icon.draw`, ordinal 1 (quest icon, centered on title) | ordinal 1 | ordinal 1 | No — a third `Icon.draw` (ordinal 2) now exists for a new "time until repeatable" clock icon; irrelevant since we only target 0/1 |
| `TextField.setText` → `Theme.listFormattedStringToWidth` (`TextFieldMixin`) | matches | matches | No — signature unchanged. Note: `TextField` gained a new `reflow()` method that also calls `listFormattedStringToWidth` but is a *different method name* (not `setText`), so it's untouched by our `@Redirect`. No internal FTBQ/FTB Library caller of `reflow()` was found in the decompiled 2101.x jars, so in practice nothing currently triggers the un-word-wrapped path — flagged as a latent gap if some other mod starts calling it. |
| `ChapterButtonAccessor` → `ChapterPanel.ChapterButton#chapter` | `private final Chapter chapter` | `private final Chapter chapter` | No |
| `QuestScreenAccessor` → `QuestScreen#selectedChapter` | package-private `Chapter selectedChapter` | package-private `Chapter selectedChapter` | No |
| `QuestScreenAccessor` → `QuestScreen#file` (**new**) | n/a | package-private `final ClientQuestFile file` | **Added** — needed for fix (c), see below |
| `ChapterButton.<init>(ChapterPanel, Chapter)` | matches | matches | No |
| `ChapterButton.onClicked(MouseButton)` | matches | matches | No |
| `ChapterButton.getActualWidth(QuestScreen)` | matches | matches | No |
| `ChapterButton.draw(GuiGraphics, Theme, int, int, int, int)` | matches | matches | No |
| `ChapterGroupButton.draw(GuiGraphics, Theme, int, int, int, int)` | assumed inherited only (hence `require = 0`) | **explicitly declared** on `ChapterGroupButton` itself (source line ~388) | Declaration confirmed to exist → `require = 0` removed per fix policy (d) |
| `ChapterPanel.alignWidgets()` | matches | matches | No |
| `ChapterPanel.drawBackground(...)` | matches | matches | No |
| `WidgetLayout.VERTICAL` field redirect (`ChapterPanelMixin`) | class field | interface field (`WidgetLayout` is now an interface) | No functional change — same `GETSTATIC` shape, `Vertical(int,int,int)` ctor unchanged |
| `Panel.widgets` field (protected, iterated in `ChapterPanelMixin`) | protected | protected | No |

## Fix list applied

> **Sources are now shared.** After both builds were verified, this directory's
> copy of `src/main/java` was removed and `build.gradle` points
> `sourceSets.main.java.srcDirs` at the repo root's `src/main/java`, so the
> 1.21.1 jar is compiled from exactly the same Java as the 1.20.1 jar. The fix
> list below was implemented once, in the root sources (e.g. `RoundedRect` caches
> insets in an `int[][]` indexed by radius rather than a `Map`); the notes
> describe intent, which is identical.

- **(a)** `ChapterButtonMixin`: added `ftbqls$cachedTeamData` /
  `ftbqls$cachedProgress` / `ftbqls$cachedProgressText` /
  `ftbqls$progressCacheNanos` (`@Unique`, per-instance). Recomputes
  `getRelativeProgress` + the `"N %"` string only when the `TeamData`
  reference changes or 250ms have elapsed.
- **(b)** `RoundedRect`: added `INSET_CACHE` (`Map<Integer,int[]>`, keyed on
  radius) and `BORDER_COLOR_CACHE` (`IdentityHashMap<Icon,Color4I>`, keyed on
  icon reference identity, `containsKey` used so a cached `null` — "no
  border" — is distinguished from "not yet computed"). Same visual output,
  confirmed by an unchanged draw call sequence.
- **(c)** `ChapterButtonMixin.ftbqls$drawCard` now reads
  `((QuestScreenAccessor) getGui()).ftbqls$getFile().selfTeamData` instead of
  `ClientQuestFile.INSTANCE.selfTeamData` — this is exactly the expression
  FTBQ's own `ChapterButton.draw`/`getActualWidth` use
  (`this.chapterPanel.questScreen.file.selfTeamData`, confirmed in the
  decompiled 2101.x source). Required adding a new `@Accessor("file")` to
  `QuestScreenAccessor` since `QuestScreen.file` is package-private.
- **(d)** `ChapterGroupButtonMixin`: `ChapterGroupButton.draw` is confirmed
  declared in 2101.x, so the explicit `require = 0` was removed from its
  `@Inject` (now inherits `defaultRequire = 1` from the mixin config, with a
  comment explaining why).
- **(e)** Package renamed to `io.github.mockmock69401.ftbqlinespacing`
  (files + `package`/`import` statements + `mixins.json` `"package"`).
  `gradle.properties`: `maven_group=io.github.mockmock69401`,
  `mod_version=1.0.0+1.21.1`, `archives_base_name=ftbq-linespacing` (mod id
  `ftbq_linespacing` unchanged).
- **(f)** `fabric.mod.json`: `environment: client`, `minecraft: "1.21.1"`,
  `java: ">=21"`, `authors: ["mockmock"]`, `contact` block, `ftbquests`/
  `ftblibrary` bounded to `>=<verified> <next-minor>`.
- **(g)** `mixins.json`: `compatibilityLevel: JAVA_21`, `minVersion: "0.8"`,
  `refmap: "ftbq_linespacing.refmap.json"`; `build.gradle`'s
  `loom.mixin.defaultRefmapName` matches, and the built jar was inspected to
  confirm the refmap is really named that.
- **(h)** `build.gradle`: `java { withSourcesJar() }`.

## Vanilla (Mojang) API changes

None needed. This codebase never constructs `ResourceLocation` directly (FTB
Library's own `Icon.getIcon(String)` handles that internally), and every
vanilla type this mod touches directly — `GuiGraphics` (`enableScissor` /
`disableScissor` / `pose()`), `FormattedText`, `Style`, `Component` — kept
identical signatures between 1.20.1 and 1.21.1 for the methods used here.
`compileJava` succeeded against the 1.21.1 mojmap classpath with zero source
changes beyond the fix list above.

## Build verification

```
versions\1.21.1\gradlew.bat -p versions\1.21.1 clean build   # (own wrapper, Gradle 9.7.1)
```

`BUILD SUCCESSFUL`. Inspected `build/libs/ftbq-linespacing-1.0.0+1.21.1.jar`:

- Only `io/github/mockmock69401/ftbqlinespacing/**` classes are bundled — no
  FTB/Minecraft classes leaked in.
- `ftbq_linespacing.refmap.json` present, content `{"mappings":{},"data":{}}`
  (expected: every mixin here is `remap = false`).
- `fabric.mod.json` version expanded to `1.0.0+1.21.1`.
- `ftbq_linespacing.mixins.json`: `compatibilityLevel: JAVA_21`,
  `minVersion: "0.8"`, correct package.
- `META-INF/MANIFEST.MF` confirms `Fabric-Loom-Version: 1.17.20`,
  `Fabric-Minecraft-Version: 1.21.1`, `Fabric-Mixin-Version: 0.17.4+mixin.0.8.7`
  (satisfies `minVersion: "0.8"`).
- `grep -r com.example versions/1.21.1` → no matches.

## Not verifiable without running the game

- Actual in-game rendering/behaviour of the quest window, chapter cards, word
  wrap, and animations — this mod has no `runClient` task and is normally
  verified by dropping the jar into a real modded instance (per the project's
  own `CLAUDE.md`). All verification here is static: compilation, decompiled
  bytecode inspection, and jar content inspection.
- Whether FTB Teams 2101.1.11 is actually required at runtime (it's
  `modCompileOnly`, `transitive = false`, and nothing in this mod's own
  source references FTB Teams types directly — it was carried over from the
  root build's dependency list for parity; if it turns out to be unused it
  could potentially be dropped, but removing it was out of scope here since
  the root 1.20.1 build depends on it too).
- Whether `TextField.reflow()` (new in 2101.x, not called anywhere in the
  decompiled FTBQ/FTB Library jars) is invoked by some other mod at runtime
  in a way that would bypass the word-wrap redirect on `setText`.
