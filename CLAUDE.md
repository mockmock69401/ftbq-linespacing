# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A client-side Fabric mod for Minecraft 1.20.1 and 1.21.1 that improves FTB Quests (2001.x / 2101.x) UI
readability via Mixins: adjustable line spacing in quest descriptions, and a
card-style chapter sidebar with progress percentages and hover/slide animations.
Everything is toggleable through `config/ftbq-linespacing.properties`.

## Build

JDK 17 required. FTB Quests / FTB Library / FTB Teams are pulled as compile-only
dependencies from CurseMaven, so **the first build needs an internet connection**.

```
gradlew.bat build          # Windows;  ./gradlew build on Linux/macOS
```

Output: `build/libs/ftbq-linespacing-1.0.0+1.20.1.jar` (the one *without* the `-sources`
suffix; a `-sources.jar` is also produced via `java { withSourcesJar() }`). The jar's
mixin refmap is pinned to `ftbq_linespacing.refmap.json` via `loom { mixin {
defaultRefmapName = ... } }` in build.gradle — Loom would otherwise emit
`ftbq-linespacing-refmap.json`, which doesn't match what
`ftbq_linespacing.mixins.json` declares.

There are no unit tests — this mod is verified by running it in-game against FTB
Quests. There is no `runClient` task configured; testing means dropping the built
jar into a real modded instance of the matching Minecraft version (FTB Quests + FTB Library).

### Multi-version layout (1.20.1 + 1.21.1)

| | 1.20.1 | 1.21.1 |
|---|---|---|
| Build dir | repo root | [versions/1.21.1/](versions/1.21.1/) — an **independent** Gradle build (own `settings.gradle`, wrapper) |
| Command | `gradlew.bat build` | `versions\1.21.1\gradlew.bat -p versions\1.21.1 build` |
| Toolchain | JDK 17, Gradle 8.8, Loom 1.6.12 | JDK 21 (Gradle toolchain, foojay auto-provisioning), Gradle 9.7.1, Loom 1.17.20 |
| Output | `build/libs/ftbq-linespacing-1.0.0+1.20.1.jar` | `versions/1.21.1/build/libs/ftbq-linespacing-1.0.0+1.21.1.jar` |

- **Java sources are shared.** `versions/1.21.1/build.gradle` sets
  `sourceSets.main.java.srcDirs = ["../../src/main/java"]`; every Mixin target and
  ordinal was verified identical in FTB 2101.x (see
  [versions/1.21.1/PORTING.md](versions/1.21.1/PORTING.md)). Any change under
  `src/main/java` therefore ships to both versions — **build both** after editing.
  If a class ever needs a version-specific change, fork it into
  `versions/1.21.1/src/main/java` and exclude it from the shared dir.
- **Resources are per-version**: `src/main/resources` (1.20.1) and
  `versions/1.21.1/src/main/resources` (1.21.1) each have their own `fabric.mod.json`
  (minecraft / java / `ftbquests` / `ftblibrary` ranges) and `ftbq_linespacing.mixins.json`
  (`JAVA_17` vs `JAVA_21`). Keep the mixin class lists in sync.
- The 1.21.1 build needs Loom 1.17 because FTBTeam's 2101.x jars were built with a
  newer Loom (Loom refuses to remap a dependency built by a newer Loom), and Loom
  1.12+ requires Gradle 9 and disables the legacy Mixin AP by default — hence
  `useLegacyMixinAp = true` there, so the refmap is still emitted.
- Mod version strings carry the MC version: `mod_version=1.0.0+<mc>` in each
  `gradle.properties`.

## Architecture

The mod does almost nothing on its own. All behavior comes from Mixins that
patch FTB Quests' GUI classes at load time, reading options from a single config
holder.

- **[SpacingConfig.java](src/main/java/io/github/mockmock69401/ftbqlinespacing/SpacingConfig.java)** —
  static holder loaded once in a `static {}` block. Reads
  `config/ftbq-linespacing.properties` and clamps every value to a safe range.
  **The file is only written when it doesn't exist** (`defaultFileContents()`: keys
  grouped into sections — quest title / subtitle & description / frame / chapter
  cards / animation — each with a `#` comment and its range); an existing file is
  never rewritten, so user edits and order survive and missing keys just use their
  defaults (delete the file to regenerate it). All fields are `public static final`,
  so **config changes require a game restart**. When adding a new option, update the
  read, the clamp, the `entry(...)` in the matching section of `defaultFileContents()`,
  and the README table together.

- **Mixins** ([src/main/java/io/github/mockmock69401/ftbqlinespacing/mixin/](src/main/java/io/github/mockmock69401/ftbqlinespacing/mixin/)),
  registered in [ftbq_linespacing.mixins.json](src/main/resources/ftbq_linespacing.mixins.json)
  (all `client`):
  - `ViewQuestPanelMixin` — patches the quest-view layout: a `@Redirect` on the
    description `setSpacing` (which also flags the field for word wrapping, see
    `TextFieldMixin`), an `@ModifyArg` for the gap arg of
    `WidgetLayout.Vertical` (each description entry is its own `TextField` whose
    height counts the last line as only `fontHeight − 1`, so the gap is
    `line-spacing − 8 + paragraph-gap` — vanilla's gap 1 at spacing 9 — to keep
    entry-to-entry pitch equal to wrapped-line pitch); `@Redirect`s (selected by `ordinal`) on the title/
    subtitle `setSpacing` calls to also apply `setScale` (title = ordinal 0,
    subtitle = ordinal 1; the subtitle one also flags the field for word wrapping),
    and on `panelText`'s `BlankPanel.setPosAndSize`
    (ordinal 3) to inset the subtitle/description panel. Extra `@ModifyArg`s cover:
    the title's Y in `titleField.setPosAndSize` (ordinal-0 `TextField
    .setPosAndSize`, index 1) = title top padding; and the ContentPanel's Y
    (ordinal-0 `BlankPanel.setPosAndSize`, index 1) which is pushed down by
    `2 × (padding − 4)` so the title keeps equal above/below padding (vanilla title
    band = 4 px top + 4 px bottom). **Window sizing / scroll** (three coupled
    `@Redirect`s, `@Shadow`ing `panelContent`): vanilla computes the modal height as
    `min(panelContent.contentHeight + titleHeight + 12, screen − 10)` (ordinal-2
    `Math.min`, with-description branch) or just `min(contentHeight, screen − 10)`
    (ordinal-1, no-subtitle/description branch — ignores the title band entirely and
    cuts off short quests), and the ContentPanel height as `this.height − 17`, all
    tuned for the vanilla title band (`contentTop ≈ 21`). A scaled/padded title makes
    `panelContent.posY` larger, so the panel overflows the window bottom and the
    quest scrolls. Both `Math.min`s are rebuilt from the panel's real `posY` as
    `min(max(contentHeight + panelContent.posY + 4 + descBottomPad, minWindowHeight),
    screen − 10)` (equals vanilla when `posY == titleHeight + 8`; `descBottomPad = 0`
    on the no-description branch), and the last `BlankPanel.setHeight` (ordinal 4)
    becomes `this.height − panelContent.posY` so the panel ends exactly at the window
    bottom — no scrollbar.
    Description **bottom** padding rides in that first term (growing the window,
    clamped to the screen) rather than the `WidgetLayout.Vertical` `post` arg, which
    lives *inside* the scrolling ContentPanel and would reintroduce the scroll. Note
    a
    `@ModifyArg` and a `@Redirect` **cannot share the same invocation**, which is
    why the line-spacing redirect targets only `addDescriptionText` while the
    title/subtitle `setSpacing` (in `addWidgets`) are handled by separate redirects.
    Text-scale caveat:
    `TextField` wraps at `maxWidth` in *unscaled* font pixels but renders at
    `scale`×, so the subtitle redirect divides `maxWidth` by the scale to avoid
    horizontal overflow. It also `@Redirect`s the **second** `Icon.draw` in
    `drawBackground` (ordinal 1; ordinal 0 is the panel background) to vertically
    centre the quest icon against the `@Shadow`ed `titleField` (`getY()` is
    absolute), since vanilla draws it top-aligned at a fixed `y + 4` and it drifts
    once the title is scaled/padded. The **first** `Icon.draw` (ordinal 0) is the
    whole-window background and is redirected to
    [RoundedRect.java](src/main/java/io/github/mockmock69401/ftbqlinespacing/RoundedRect.java) for
    `window-corner-radius`. That selector omits the descriptor
    (`Icon;draw`, no `class_332`) so no Minecraft type — which would need remapping
    under `remap = false` — appears in the target string. The top-right close/pin
    buttons are re-centred the same way via `@Redirect`s on `Button.setPosAndSize`
    ordinals 0/1 (ordinals 2/3 are the ◄ ► dependency arrows). Those arrow X args
    are inset from the window edges by `@ModifyArg`s on the same `setPosAndSize`
    ordinals 2 (`x + pad`) and 3 (`x − pad`). The title-row icons have their own
    horizontal inset (`title-icon-padding-x`) folded into the three centring
    redirects: the quest icon shifts `x + pad` (from the left edge), the close/pin
    buttons shift `x − pad` (from the right edge). Uses `@Pseudo` + string
    target (`targets = "...ViewQuestPanel"`) so FTBQ isn't needed to compile this
    class.
  - `TextFieldMixin` — targets FTB Library's `TextField` (not FTBQ) and
    `@Redirect`s the `Theme.listFormattedStringToWidth` call inside `setText` to
    [WordWrap.java](src/main/java/io/github/mockmock69401/ftbqlinespacing/WordWrap.java), but only
    for fields flagged through the `WordWrapTarget` duck interface — in practice
    the quest subtitle and description. Without it the splitter breaks CJK text between
    syllables, so every line is filled to the panel edge and the paragraph reads as
    justified rather than flush left (`desc-word-wrap=false` restores vanilla
    wrapping). The selector omits the descriptor because it names a Minecraft type,
    which would not be remapped under `remap = false`; `method = "setText"` matches
    both overloads, hence `require = 0`. Not `@Pseudo` — FTB Library is a compile
    dependency.
  - `ChapterButtonMixin` — cancels the vanilla `draw` and renders the card
    (border, centered icon, title + `getRelativeProgress` %, gold selection accent,
    hover lerp, click flash, subtle scale). Also overrides `getActualWidth` /
    height for card sizing. Reads `TeamData` via
    `((QuestScreenAccessor) getGui()).ftbqls$getFile().selfTeamData` — the same
    source vanilla `ChapterButton` uses — not `ClientQuestFile.INSTANCE` (which is
    the client's global singleton and can disagree with the screen's own file).
    `getRelativeProgress(chapter)` walks every quest/task in the chapter and FTBQ
    doesn't cache it, so the result (and the rendered `"NN %"` string) is cached
    per-instance, recomputed only when the `TeamData` reference changes or 250 ms
    have elapsed.
  - `ChapterGroupButtonMixin` — cancels `draw` for group headers; animated hover
    highlight and a collapse arrow that rotates smoothly between states.
  - `ChapterPanelMixin` — `@Redirect`s the layout to a spaced `CARD_LAYOUT`, and
    drives the slide animation: entries are tracked by a stable key
    (`"c"+chapterId` / `"g"+groupId`) across `refreshWidgets` rebuilds so
    animations survive widget recreation and mid-flight re-layouts.
  - `ChapterButtonAccessor` / `QuestScreenAccessor` — `@Accessor` interfaces to
    reach FTBQ's private `chapter`, `selectedChapter`, and `file` fields.

- **[RoundedRect.java](src/main/java/io/github/mockmock69401/ftbqlinespacing/RoundedRect.java)** —
  rounds the quest window's corners. An `Icon` cannot be masked (it may be a colour,
  a tiled texture, or a composite), so it is drawn repeatedly under a scissor: once
  for the straight middle band, then one scanline per corner row, inset by the circle
  equation. Clipping also eats the theme border's corner runs, so the arcs are
  repainted with the colour found by walking the icon tree for a
  `HollowRectangleIcon` (no border in the theme = no arcs). Scissor rects ignore the
  pose matrix, which is safe because FTB Library's screen only translates along Z.
  The per-row insets (`sqrt` per scanline) are cached by radius, and the border
  colour found by the icon-tree walk is cached by `Icon` identity (render-thread
  only) — both depend only on inputs that don't change frame to frame.

- **[WordWrap.java](src/main/java/io/github/mockmock69401/ftbqlinespacing/WordWrap.java)** — a
  greedy line splitter that only breaks at spaces, falling back to a mid-word break
  for a single word wider than the line. Flattens the `FormattedText` into chars +
  per-char `Style`, wraps, then reassembles each line with `FormattedText.composite`.
  Measures candidate lines with `Theme.getStringWidth`, so it stays correct under
  whatever font/text engine is installed.

- **[SlideAnim.java](src/main/java/io/github/mockmock69401/ftbqlinespacing/SlideAnim.java)** — a
  single smoothstep-eased vertical slide, keyed externally by chapter/group id so
  it outlives widget recreation. Animations are captured at the tail of
  `alignWidgets` and advanced each frame in `drawBackground`.

### Conventions that matter

- **`remap = false` on every FTBQ/FTB Library Mixin** — both are unobfuscated mod
  code, not Minecraft, so their names must not be remapped. (Mixins targeting
  Minecraft classes would remap normally, but there are none here.)
- **Unique member prefix `ftbqls$`** on all injected methods/fields to avoid
  collisions with the target class.
- **`require = 0` on the spacing/layout/slide injectors** — if FTBQ's internals
  change, those injections silently no-op instead of crashing. The card-rendering
  injectors inherit `defaultRequire: 1` (they *will* fail the mixin if their
  target is gone); `chapter-cards=false` is the safe off-switch for the whole
  card feature.
- When targeting FTBQ methods, remember they are **called before `setText`**, so
  changing spacing recalculates widget height automatically.

## Dependency version pins

CurseMaven file IDs in [build.gradle](build.gradle) pin the FTBQ/FTB Library API
the Mixins were written against (FTB Library 2001.2.11, FTB Quests 2001.4.22, FTB
Teams 2001.3.2). Bumping these may move the injection points the string/target
selectors rely on — re-verify the Mixin targets after any bump (decompile the new
CurseForge file and check every class/method/field/ordinal against the Mixins
listed under Architecture above).

FTB Library is deliberately capped at 2001.2.11, one version below the latest
(2001.2.13): starting at 2001.2.12, FTBTeam's own build of FTB Library was done
with a newer Fabric Loom than this project pins (`fabric-loom` in build.gradle),
and Loom refuses to remap a `modCompileOnly`/`modImplementation` dependency that
was built by a newer Loom than the consumer's
(`net.fabricmc.loom.configuration.mods.ArtifactMetadata.validateLoomVersion`) —
the build fails at dependency resolution with "Mod was built with a newer version
of Loom" before a single Mixin is even checked. This is a **devtime toolchain**
restriction, not a known runtime/Mixin incompatibility, so `fabric.mod.json`'s
`ftblibrary` range still allows 2001.2.12/2001.2.13 at runtime. Note the Mixin
targets were only decompile-verified up to 2001.2.11; the FTB Library-targeted
injector (`TextFieldMixin`) is `require = 0`, but the card mixins call FTB Library
APIs (`Theme`, `Color4I`, `GuiHelper`) directly. To ship against the
true latest FTB Library, bump the pinned `fabric-loom` version first and confirm
the resulting jar still meets this project's Java/Gradle/Loom requirements, then
retry the CurseMaven pin.

When bumping either pin: after re-verifying the Mixin targets, also update the
`fabric.mod.json` `depends` range for that mod so installs outside the verified
window fail loudly instead of silently.

The 1.21.1 build pins its own set in
[versions/1.21.1/build.gradle](versions/1.21.1/build.gradle): FTB Library 2101.1.36,
FTB Quests 2101.1.35, FTB Teams 2101.1.11 (ranges `>=2101.1.35/36 <2101.2.0` in its
`fabric.mod.json`). Because the Java is shared, a pin bump on either side must be
re-verified against **both** versions' FTB jars.
