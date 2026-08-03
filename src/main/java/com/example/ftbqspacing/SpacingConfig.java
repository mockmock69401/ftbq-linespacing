package com.example.ftbqspacing;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads values from config/ftbq-linespacing.properties.
 * A default file is created on first launch; missing keys are merged in.
 */
public final class SpacingConfig {

    /** Pixel distance between wrapped lines inside a paragraph (FTBQ default: 9). */
    public static final int LINE_SPACING;
    /** Pixel gap between paragraphs in the quest description (FTBQ default: 1). */
    public static final int PARAGRAPH_GAP;

    /** Font scale applied to the quest title (1.0 = FTBQ default). */
    public static final float TITLE_SCALE;
    /** Font scale applied to the quest subtitle (1.0 = FTBQ default). */
    public static final float SUBTITLE_SCALE;
    /** Vertical padding (px) above and below the quest title (FTBQ default: 4). */
    public static final int TITLE_PADDING_Y;
    /** Horizontal inset (px) added to each side of the subtitle/description text panel. */
    public static final int DESC_PADDING_X;
    /** Extra space (px) above the subtitle/description text panel. */
    public static final int DESC_PADDING_TOP;
    /** Extra space (px) below the subtitle/description text (FTBQ default: 2). */
    public static final int DESC_PADDING_BOTTOM;
    /** Horizontal inset (px) of the ◄ ► dependency/dependant arrows from the window edges. */
    public static final int ARROW_PADDING_X;
    /** Horizontal inset (px) of the title-row icons (quest icon + pin/close) from the window edges. */
    public static final int TITLE_ICON_PADDING_X;
    /** Minimum quest window height in px (0 = off); floors the modal so short quests aren't cut. */
    public static final int MIN_WINDOW_HEIGHT;

    /** Master switch for the card-style chapter list. */
    public static final boolean CHAPTER_CARDS;
    /** Height of each chapter card in pixels. */
    public static final int CARD_HEIGHT;
    /** Vertical gap between chapter cards in pixels. */
    public static final int CARD_GAP;
    /** Whether to show a completion percentage line under the chapter title. */
    public static final boolean SHOW_PROGRESS;
    /** Whether to draw a subtle progress bar tint across the card background. */
    public static final boolean PROGRESS_BAR;
    /** Hover/click transition duration in milliseconds (0 = instant, no animation). */
    public static final int TRANSITION_MS;
    /** Whether cards subtly scale up on hover and dip on click. */
    public static final boolean CARD_SCALE;
    /** Minimum width of chapter cards in pixels (panel widens to fit). */
    public static final int CARD_MIN_WIDTH;

    private static final int D_LINE_SPACING = 11;
    private static final int D_PARAGRAPH_GAP = 3;
    private static final float D_TITLE_SCALE = 1.45f;
    private static final float D_SUBTITLE_SCALE = 1.0f;
    private static final int D_TITLE_PADDING_Y = 8;
    private static final int D_DESC_PADDING_X = 8;
    private static final int D_DESC_PADDING_TOP = 2;
    private static final int D_DESC_PADDING_BOTTOM = 8;
    private static final int D_ARROW_PADDING_X = 4;
    private static final int D_TITLE_ICON_PADDING_X = 4;
    private static final int D_MIN_WINDOW_HEIGHT = 120;
    private static final boolean D_CHAPTER_CARDS = true;
    private static final int D_CARD_HEIGHT = 26;
    private static final int D_CARD_GAP = 4;
    private static final boolean D_SHOW_PROGRESS = true;
    private static final boolean D_PROGRESS_BAR = true;
    private static final int D_TRANSITION_MS = 120;
    private static final boolean D_CARD_SCALE = true;
    private static final int D_CARD_MIN_WIDTH = 170;

    static {
        Properties props = new Properties();
        Path file = null;
        try {
            file = FabricLoader.getInstance().getConfigDir().resolve("ftbq-linespacing.properties");
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            }
        } catch (IOException ignored) {
        }

        int lineSpacing = parseInt(props, "line-spacing", D_LINE_SPACING);
        int paragraphGap = parseInt(props, "paragraph-gap", D_PARAGRAPH_GAP);
        boolean chapterCards = parseBool(props, "chapter-cards", D_CHAPTER_CARDS);
        int cardHeight = parseInt(props, "card-height", D_CARD_HEIGHT);
        int cardGap = parseInt(props, "card-gap", D_CARD_GAP);
        boolean showProgress = parseBool(props, "show-progress", D_SHOW_PROGRESS);
        boolean progressBar = parseBool(props, "progress-bar", D_PROGRESS_BAR);
        int transitionMs = parseInt(props, "transition-ms", D_TRANSITION_MS);
        boolean cardScale = parseBool(props, "card-scale", D_CARD_SCALE);
        int cardMinWidth = parseInt(props, "card-min-width", D_CARD_MIN_WIDTH);
        float titleScale = parseFloat(props, "title-scale", D_TITLE_SCALE);
        float subtitleScale = parseFloat(props, "subtitle-scale", D_SUBTITLE_SCALE);
        int titlePaddingY = parseInt(props, "title-padding-y", D_TITLE_PADDING_Y);
        int descPaddingX = parseInt(props, "desc-padding-x", D_DESC_PADDING_X);
        int descPaddingTop = parseInt(props, "desc-padding-top", D_DESC_PADDING_TOP);
        int descPaddingBottom = parseInt(props, "desc-padding-bottom", D_DESC_PADDING_BOTTOM);
        int arrowPaddingX = parseInt(props, "arrow-padding-x", D_ARROW_PADDING_X);
        int titleIconPaddingX = parseInt(props, "title-icon-padding-x", D_TITLE_ICON_PADDING_X);
        int minWindowHeight = parseInt(props, "min-window-height", D_MIN_WINDOW_HEIGHT);

        LINE_SPACING = clamp(lineSpacing, 9, 20);
        PARAGRAPH_GAP = clamp(paragraphGap, 0, 12);
        TITLE_SCALE = clampF(titleScale, 0.5f, 2.0f);
        SUBTITLE_SCALE = clampF(subtitleScale, 0.5f, 2.0f);
        TITLE_PADDING_Y = clamp(titlePaddingY, 0, 20);
        DESC_PADDING_X = clamp(descPaddingX, 0, 40);
        DESC_PADDING_TOP = clamp(descPaddingTop, 0, 40);
        DESC_PADDING_BOTTOM = clamp(descPaddingBottom, 0, 40);
        ARROW_PADDING_X = clamp(arrowPaddingX, 0, 30);
        TITLE_ICON_PADDING_X = clamp(titleIconPaddingX, 0, 30);
        MIN_WINDOW_HEIGHT = clamp(minWindowHeight, 0, 400);
        CHAPTER_CARDS = chapterCards;
        CARD_HEIGHT = clamp(cardHeight, 20, 40);
        CARD_GAP = clamp(cardGap, 0, 10);
        SHOW_PROGRESS = showProgress;
        PROGRESS_BAR = progressBar;
        TRANSITION_MS = clamp(transitionMs, 0, 500);
        CARD_SCALE = cardScale;
        CARD_MIN_WIDTH = clamp(cardMinWidth, 100, 400);

        // write back merged values so new keys appear in old config files
        if (file != null) {
            props.setProperty("line-spacing", String.valueOf(LINE_SPACING));
            props.setProperty("paragraph-gap", String.valueOf(PARAGRAPH_GAP));
            props.setProperty("chapter-cards", String.valueOf(CHAPTER_CARDS));
            props.setProperty("card-height", String.valueOf(CARD_HEIGHT));
            props.setProperty("card-gap", String.valueOf(CARD_GAP));
            props.setProperty("show-progress", String.valueOf(SHOW_PROGRESS));
            props.setProperty("progress-bar", String.valueOf(PROGRESS_BAR));
            props.setProperty("transition-ms", String.valueOf(TRANSITION_MS));
            props.setProperty("card-scale", String.valueOf(CARD_SCALE));
            props.setProperty("card-min-width", String.valueOf(CARD_MIN_WIDTH));
            props.setProperty("title-scale", String.valueOf(TITLE_SCALE));
            props.setProperty("subtitle-scale", String.valueOf(SUBTITLE_SCALE));
            props.setProperty("title-padding-y", String.valueOf(TITLE_PADDING_Y));
            props.setProperty("desc-padding-x", String.valueOf(DESC_PADDING_X));
            props.setProperty("desc-padding-top", String.valueOf(DESC_PADDING_TOP));
            props.setProperty("desc-padding-bottom", String.valueOf(DESC_PADDING_BOTTOM));
            props.setProperty("arrow-padding-x", String.valueOf(ARROW_PADDING_X));
            props.setProperty("title-icon-padding-x", String.valueOf(TITLE_ICON_PADDING_X));
            props.setProperty("min-window-height", String.valueOf(MIN_WINDOW_HEIGHT));
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "FTB Quests spacing & chapter cards. line-spacing: px between wrapped lines (9-20). paragraph-gap: px between paragraphs (0-12). chapter-cards: card-style chapter list. card-height (20-40) / card-gap (0-10). show-progress: % line on cards. progress-bar: subtle progress tint on card background. transition-ms: hover/click fade duration, 0-500 (0 = off). card-scale: subtle zoom on hover/click. card-min-width: minimum card width in px, 100-400. title-scale / subtitle-scale: font scale for the quest title & subtitle, 0.5-2.0 (1.0 = vanilla). title-padding-y: vertical padding px above & below the title, 0-20 (vanilla 4). desc-padding-x: horizontal inset px on each side of the subtitle/description panel, 0-40. desc-padding-top: extra px above the subtitle/description panel, 0-40. desc-padding-bottom: extra px below the subtitle/description text, 0-40 (vanilla 2). arrow-padding-x: horizontal inset px of the left/right dependency arrows from the window edges, 0-30. title-icon-padding-x: horizontal inset px of the title-row icons (quest icon + pin/close) from the window edges, 0-30. min-window-height: minimum quest window height in px, 0-400 (0 = off, content-fit only).");
            } catch (IOException ignored) {
            }
        }
    }

    private static int parseInt(Properties p, String key, int fallback) {
        String v = p.getProperty(key);
        if (v == null) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBool(Properties p, String key, boolean fallback) {
        String v = p.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }

    private static float parseFloat(Properties p, String key, float fallback) {
        String v = p.getProperty(key);
        if (v == null) return fallback;
        try {
            return Float.parseFloat(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clampF(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private SpacingConfig() {
    }
}
