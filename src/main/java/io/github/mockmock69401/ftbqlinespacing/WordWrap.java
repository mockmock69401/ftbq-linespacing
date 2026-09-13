package io.github.mockmock69401.ftbqlinespacing;

import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Greedy line wrapping that only breaks at spaces.
 *
 * <p>FTB Library wraps a {@code TextField} with the vanilla string splitter, which
 * breaks at whatever the font's line-break algorithm considers a legal boundary.
 * For Latin text that is (almost) always a space, so lines come out ragged on the
 * right. For Korean/Japanese/Chinese, every syllable is a legal boundary — the
 * Modern UI text engine in particular uses ICU line breaking — so each line is
 * filled right up to the panel edge and the paragraph reads as if it were
 * justified, even though the text is drawn flush left.
 *
 * <p>This splitter never breaks inside a run of non-space characters, which gives
 * the ragged right edge of ordinary left-aligned text. A single "word" wider than
 * the available width still falls back to breaking mid-word so it cannot overflow.
 */
public final class WordWrap {

    private WordWrap() {
    }

    /**
     * Drop-in replacement for {@link Theme#listFormattedStringToWidth(FormattedText, int)}.
     * Falls back to the theme's own splitter for empty text.
     */
    public static List<FormattedText> split(Theme theme, FormattedText text, int maxWidth) {
        StringBuilder chars = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        text.visit((style, string) -> {
            chars.append(string);
            for (int i = 0; i < string.length(); i++) {
                styles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);

        int length = chars.length();
        if (length == 0) {
            return theme.listFormattedStringToWidth(text, maxWidth);
        }

        List<FormattedText> lines = new ArrayList<>();
        int paragraphStart = 0;
        for (int i = 0; i <= length; i++) {
            if (i == length || chars.charAt(i) == '\n') {
                wrapParagraph(theme, chars, styles, paragraphStart, i, maxWidth, lines);
                paragraphStart = i + 1;
            }
        }
        return lines;
    }

    private static void wrapParagraph(Theme theme, CharSequence chars, List<Style> styles,
                                      int start, int end, int maxWidth, List<FormattedText> out) {
        if (start >= end) {
            out.add(FormattedText.EMPTY);
            return;
        }
        int lineStart = start;
        while (lineStart < end) {
            int lineEnd = findBreak(theme, chars, styles, lineStart, end, maxWidth);
            int trimmed = lineEnd;
            while (trimmed > lineStart && chars.charAt(trimmed - 1) == ' ') {
                trimmed--;
            }
            out.add(slice(chars, styles, lineStart, trimmed));
            lineStart = lineEnd;
        }
    }

    /**
     * Returns the index the next line starts at: one past the trailing spaces of the
     * last word that still fits. Always greater than {@code start} so wrapping
     * terminates.
     */
    private static int findBreak(Theme theme, CharSequence chars, List<Style> styles,
                                 int start, int end, int maxWidth) {
        int lastBreak = -1;
        int i = start;
        while (i < end) {
            int wordEnd = i;
            while (wordEnd < end && chars.charAt(wordEnd) == ' ') {
                wordEnd++;
            }
            while (wordEnd < end && chars.charAt(wordEnd) != ' ') {
                wordEnd++;
            }
            int afterSpaces = wordEnd;
            while (afterSpaces < end && chars.charAt(afterSpaces) == ' ') {
                afterSpaces++;
            }

            // trailing spaces never count towards the line width
            if (theme.getStringWidth(slice(chars, styles, start, wordEnd)) > maxWidth) {
                return lastBreak > start ? lastBreak : breakInsideWord(theme, chars, styles, start, wordEnd, maxWidth);
            }

            lastBreak = afterSpaces;
            i = afterSpaces;
        }
        return end;
    }

    /** Last resort for a single word wider than the whole line: break it mid-word. */
    private static int breakInsideWord(Theme theme, CharSequence chars, List<Style> styles,
                                       int start, int limit, int maxWidth) {
        int fits = -1;
        int end = nextIndex(chars, start, limit);
        while (true) {
            if (theme.getStringWidth(slice(chars, styles, start, end)) > maxWidth) {
                break;
            }
            fits = end;
            if (end >= limit) {
                break;
            }
            end = nextIndex(chars, end, limit);
        }
        // never return start, or wrapParagraph would spin forever
        return fits > start ? fits : nextIndex(chars, start, limit);
    }

    /** Index of the next code point, keeping surrogate pairs together. */
    private static int nextIndex(CharSequence chars, int i, int limit) {
        int next = i + 1;
        if (next < limit && Character.isHighSurrogate(chars.charAt(i)) && Character.isLowSurrogate(chars.charAt(next))) {
            next++;
        }
        return next;
    }

    /** Rebuilds {@code [from, to)} as formatted text, merging neighbouring equal styles. */
    private static FormattedText slice(CharSequence chars, List<Style> styles, int from, int to) {
        if (from >= to) {
            return FormattedText.EMPTY;
        }
        List<FormattedText> parts = new ArrayList<>();
        int runStart = from;
        for (int i = from + 1; i <= to; i++) {
            if (i == to || !styles.get(i).equals(styles.get(runStart))) {
                parts.add(FormattedText.of(chars.subSequence(runStart, i).toString(), styles.get(runStart)));
                runStart = i;
            }
        }
        return parts.size() == 1 ? parts.get(0) : FormattedText.composite(parts);
    }
}
