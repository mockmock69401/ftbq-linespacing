# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A client-side Fabric mod for Minecraft 1.20.1 that improves FTB Quests (2001.x) UI
readability via Mixins: adjustable line spacing in quest descriptions, and a
card-style chapter sidebar with progress percentages and hover/slide animations.
Everything is toggleable through `config/ftbq-linespacing.properties`.

## Build

JDK 17 required. FTB Quests / FTB Library / FTB Teams are pulled as compile-only
dependencies from CurseMaven, so **the first build needs an internet connection**.

```
gradlew.bat build          # Windows;  ./gradlew build on Linux/macOS
```

Output: `build/libs/ftbq-linespacing-1.0.0.jar` (the one *without* the `-sources` suffix).

There are no unit tests — this mod is verified by running it in-game against FTB
Quests. There is no `runClient` task configured; testing means dropping the built
jar into a real modded 1.20.1 instance (FTB Quests + FTB Library).

## Architecture

The mod does almost nothing on its own. All behavior comes from Mixins that
patch FTB Quests' GUI classes at load time, reading options from a single config
holder.

- **[SpacingConfig.java](src/main/java/com/example/ftbqspacing/SpacingConfig.java)** —
  static holder loaded once in a `static {}` block. Reads
  `config/ftbq-linespacing.properties`, clamps every value to a safe range, and
  **writes the merged config back** so new keys appear in old files. All fields
  are `public static final`, so **config changes require a game restart**. When
  adding a new option, update the read, the clamp, the write-back, and the header
  comment together.

- **Mixins** ([src/main/java/com/example/ftbqspacing/mixin/](src/main/java/com/example/ftbqspacing/mixin/)),
  registered in [ftbq_linespacing.mixins.json](src/main/resources/ftbq_linespacing.mixins.json)
  (all `client`):
  - `ViewQuestPanelMixin` — patches the quest-view layout: `@ModifyArg` for
    description line spacing (`setSpacing`) and the paragraph-gap arg of
    `WidgetLayout.Vertical`; `@Redirect`s (selected by `ordinal`) on the title/
    subtitle `setSpacing` calls to also apply `setScale` (title = ordinal 0,
    subtitle = ordinal 1), and on `panelText`'s `BlankPanel.setPosAndSize`
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
    why the line-spacing `@ModifyArg` targets only `addDescriptionText` while the
    title/subtitle `setSpacing` (in `addWidgets`) are handled by the redirects.
    Text-scale caveat:
    `TextField` wraps at `maxWidth` in *unscaled* font pixels but renders at
    `scale`×, so the subtitle redirect divides `maxWidth` by the scale to avoid
    horizontal overflow. It also `@Redirect`s the **second** `Icon.draw` in
    `drawBackground` (ordinal 1; ordinal 0 is the panel background) to vertically
    centre the quest icon against the `@Shadow`ed `titleField` (`getY()` is
    absolute), since vanilla draws it top-aligned at a fixed `y + 4` and it drifts
    once the title is scaled/padded. That selector omits the descriptor
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
  - `ChapterButtonMixin` — cancels the vanilla `draw` and renders the card
    (border, centered icon, title + `getRelativeProgress` %, gold selection accent,
    hover lerp, click flash, subtle scale). Also overrides `getActualWidth` /
    height for card sizing.
  - `ChapterGroupButtonMixin` — cancels `draw` for group headers; animated hover
    highlight and a collapse arrow that rotates smoothly between states.
  - `ChapterPanelMixin` — `@Redirect`s the layout to a spaced `CARD_LAYOUT`, and
    drives the slide animation: entries are tracked by a stable key
    (`"c"+chapterId` / `"g"+groupId`) across `refreshWidgets` rebuilds so
    animations survive widget recreation and mid-flight re-layouts.
  - `ChapterButtonAccessor` / `QuestScreenAccessor` — `@Accessor` interfaces to
    reach FTBQ's private `chapter` and `selectedChapter` fields.

- **[SlideAnim.java](src/main/java/com/example/ftbqspacing/SlideAnim.java)** — a
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
the Mixins were written against (FTB Library 2001.2.9, FTB Quests 2001.4.14, FTB
Teams 2001.3.2). Bumping these may move the injection points the string/target
selectors rely on — re-verify the Mixin targets after any bump.
