package io.github.mockmock69401.ftbqlinespacing;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads values from config/ftbq-linespacing.properties.
 * A commented default file is created only when none exists; an existing file is
 * read but never rewritten, so missing keys simply fall back to their defaults.
 */
public final class SpacingConfig {

    /**
     * Pixel distance between wrapped lines of the quest subtitle/description
     * (FTBQ default: 9 = the font height, i.e. 1.0; 16 ≈ 1.8).
     */
    public static final int LINE_SPACING;
    /**
     * Extra px between description entries, on top of the line spacing (0 = entries
     * are spaced exactly like wrapped lines). Newlines inside a single entry don't
     * get it, so a non-zero value spaces multi-entry descriptions differently.
     */
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
    /**
     * Break quest subtitle/description lines at spaces only, instead of wherever the font's
     * line-break algorithm allows. CJK text has a legal break after almost every
     * character, so the default splitter fills each line right to the panel edge
     * and the paragraph reads as justified; word wrapping restores the ragged right
     * edge of ordinary left-aligned text.
     */
    public static final boolean DESC_WORD_WRAP;
    /** Horizontal inset (px) of the ◄ ► dependency/dependant arrows from the window edges. */
    public static final int ARROW_PADDING_X;
    /** Horizontal inset (px) of the title-row icons (quest icon + pin/close) from the window edges. */
    public static final int TITLE_ICON_PADDING_X;
    /** Corner radius (px) of the quest window outline (0 = off, square corners). */
    public static final int WINDOW_CORNER_RADIUS;
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

    private static final int D_LINE_SPACING = 12;
    private static final int D_PARAGRAPH_GAP = 0;
    private static final float D_TITLE_SCALE = 1.35f;
    private static final float D_SUBTITLE_SCALE = 1.0f;
    private static final int D_TITLE_PADDING_Y = 8;
    private static final int D_DESC_PADDING_X = 8;
    private static final int D_DESC_PADDING_TOP = 2;
    private static final int D_DESC_PADDING_BOTTOM = 8;
    private static final boolean D_DESC_WORD_WRAP = true;
    private static final int D_ARROW_PADDING_X = 4;
    private static final int D_TITLE_ICON_PADDING_X = 4;
    private static final int D_WINDOW_CORNER_RADIUS = 6;
    private static final int D_MIN_WINDOW_HEIGHT = 120;
    private static final boolean D_CHAPTER_CARDS = true;
    private static final int D_CARD_HEIGHT = 26;
    private static final int D_CARD_GAP = 4;
    private static final boolean D_SHOW_PROGRESS = true;
    private static final boolean D_PROGRESS_BAR = true;
    private static final int D_TRANSITION_MS = 120;
    private static final boolean D_CARD_SCALE = true;
    private static final int D_CARD_MIN_WIDTH = 170;

    // must be initialised before the static block below, which builds the default file
    private static final String NL = System.lineSeparator();

    static {
        Properties props = new Properties();
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("ftbq-linespacing.properties");
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, defaultFileContents(), StandardCharsets.UTF_8);
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // unreadable file, or a malformed \\uXXXX escape: whatever was loaded is used,
            // everything else falls back to its default
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
        boolean descWordWrap = parseBool(props, "desc-word-wrap", D_DESC_WORD_WRAP);
        int arrowPaddingX = parseInt(props, "arrow-padding-x", D_ARROW_PADDING_X);
        int titleIconPaddingX = parseInt(props, "title-icon-padding-x", D_TITLE_ICON_PADDING_X);
        int windowCornerRadius = parseInt(props, "window-corner-radius", D_WINDOW_CORNER_RADIUS);
        int minWindowHeight = parseInt(props, "min-window-height", D_MIN_WINDOW_HEIGHT);

        LINE_SPACING = clamp(lineSpacing, 9, 20);
        PARAGRAPH_GAP = clamp(paragraphGap, 0, 12);
        TITLE_SCALE = clampF(titleScale, 0.5f, 2.0f);
        SUBTITLE_SCALE = clampF(subtitleScale, 0.5f, 2.0f);
        TITLE_PADDING_Y = clamp(titlePaddingY, 0, 20);
        DESC_PADDING_X = clamp(descPaddingX, 0, 40);
        DESC_PADDING_TOP = clamp(descPaddingTop, 0, 40);
        DESC_PADDING_BOTTOM = clamp(descPaddingBottom, 0, 40);
        DESC_WORD_WRAP = descWordWrap;
        ARROW_PADDING_X = clamp(arrowPaddingX, 0, 30);
        TITLE_ICON_PADDING_X = clamp(titleIconPaddingX, 0, 30);
        WINDOW_CORNER_RADIUS = clamp(windowCornerRadius, 0, 16);
        MIN_WINDOW_HEIGHT = clamp(minWindowHeight, 0, 400);
        CHAPTER_CARDS = chapterCards;
        CARD_HEIGHT = clamp(cardHeight, 20, 40);
        CARD_GAP = clamp(cardGap, 0, 10);
        SHOW_PROGRESS = showProgress;
        PROGRESS_BAR = progressBar;
        TRANSITION_MS = clamp(transitionMs, 0, 500);
        CARD_SCALE = cardScale;
        CARD_MIN_WIDTH = clamp(cardMinWidth, 100, 400);
    }

    /**
     * The commented default config, grouped by the part of the UI each option affects.
     * Written only when the file does not exist yet.
     */
    private static String defaultFileContents() {
        StringBuilder sb = new StringBuilder();
        comment(sb, "Chapter Cards UI with Spacing - client config");
        comment(sb, "");
        comment(sb, "This file is only generated when it does not exist. Delete it to restore the");
        comment(sb, "defaults and to pick up options added by newer versions of the mod.");
        comment(sb, "Changes take effect after restarting the game. Out-of-range values are clamped.");

        section(sb, "Quest window: title");
        entry(sb, "title-scale", D_TITLE_SCALE,
                "Font scale of the quest title. 0.5-2.0 (FTB Quests default: 1.0)");
        entry(sb, "title-padding-y", D_TITLE_PADDING_Y,
                "Space in px above and below the quest title. 0-20 (FTB Quests default: 4)");
        entry(sb, "title-icon-padding-x", D_TITLE_ICON_PADDING_X,
                "Horizontal inset in px of the title-row icons from the window edges",
                "(quest icon on the left, pin/close buttons on the right). 0-30");

        section(sb, "Quest window: subtitle & description");
        entry(sb, "subtitle-scale", D_SUBTITLE_SCALE,
                "Font scale of the quest subtitle. 0.5-2.0 (FTB Quests default: 1.0)");
        entry(sb, "line-spacing", D_LINE_SPACING,
                "Distance in px between wrapped lines of the subtitle/description. 9-20",
                "(9 = the font height, i.e. FTB Quests' default; 16 = about 1.8x)");
        entry(sb, "paragraph-gap", D_PARAGRAPH_GAP,
                "Extra px between description entries, on top of line-spacing. 0-12",
                "(0 = entries are spaced like wrapped lines; line breaks inside one entry never get it)");
        entry(sb, "desc-word-wrap", D_DESC_WORD_WRAP,
                "Break subtitle/description lines at spaces only, so CJK text stays flush left",
                "instead of every line being stretched to the panel edge.",
                "false = FTB Quests' default line breaking");
        entry(sb, "desc-padding-x", D_DESC_PADDING_X,
                "Horizontal inset in px on each side of the subtitle/description text. 0-40");
        entry(sb, "desc-padding-top", D_DESC_PADDING_TOP,
                "Extra space in px above the subtitle/description text. 0-40");
        entry(sb, "desc-padding-bottom", D_DESC_PADDING_BOTTOM,
                "Extra space in px below the subtitle/description text. 0-40 (FTB Quests default: 2)");

        section(sb, "Quest window: frame");
        entry(sb, "window-corner-radius", D_WINDOW_CORNER_RADIUS,
                "Corner radius in px of the quest window. 0-16 (0 = square corners)");
        entry(sb, "min-window-height", D_MIN_WINDOW_HEIGHT,
                "Minimum quest window height in px, so short quests are not cut off. 0-400 (0 = off)");
        entry(sb, "arrow-padding-x", D_ARROW_PADDING_X,
                "Horizontal inset in px of the left/right dependency arrows from the window edges. 0-30");

        section(sb, "Chapter list: cards");
        entry(sb, "chapter-cards", D_CHAPTER_CARDS,
                "Card-style chapter list. false = FTB Quests' default list",
                "(every option below this one is then ignored)");
        entry(sb, "card-height", D_CARD_HEIGHT,
                "Height of each chapter card in px. 20-40");
        entry(sb, "card-gap", D_CARD_GAP,
                "Vertical gap between chapter cards in px. 0-10");
        entry(sb, "card-min-width", D_CARD_MIN_WIDTH,
                "Minimum card width in px; the chapter panel widens to fit. 100-400");
        entry(sb, "show-progress", D_SHOW_PROGRESS,
                "Show the completion percentage under the chapter title");
        entry(sb, "progress-bar", D_PROGRESS_BAR,
                "Tint the card background with a subtle progress bar");

        section(sb, "Chapter list: animation");
        entry(sb, "transition-ms", D_TRANSITION_MS,
                "Duration in ms of the hover/click and slide animations. 0-500 (0 = no animation)");
        entry(sb, "card-scale", D_CARD_SCALE,
                "Slightly enlarge cards on hover and shrink them on click (needs transition-ms > 0)");
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title) {
        String rule = "-".repeat(72);
        sb.append(NL);
        comment(sb, rule);
        comment(sb, title);
        comment(sb, rule);
    }

    private static void entry(StringBuilder sb, String key, Object defaultValue, String... description) {
        sb.append(NL);
        for (String line : description) {
            comment(sb, line);
        }
        sb.append(key).append('=').append(defaultValue).append(NL);
    }

    private static void comment(StringBuilder sb, String line) {
        sb.append(line.isEmpty() ? "#" : "# " + line).append(NL);
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
