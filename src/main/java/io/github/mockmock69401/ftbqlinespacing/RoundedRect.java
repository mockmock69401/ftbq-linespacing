package io.github.mockmock69401.ftbqlinespacing;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.CombinedIcon;
import dev.ftb.mods.ftblibrary.icon.HollowRectangleIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconWithParent;
import net.minecraft.client.gui.GuiGraphics;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Draws an FTB Library {@link Icon} into a rectangle with its corners rounded off.
 *
 * <p>There is no way to mask an arbitrary icon — it may be a tiled texture, a
 * colour, or a composite — so the icon is drawn several times under a scissor
 * instead: once for the straight middle band, then one scanline at a time through
 * each corner band, inset by the circle equation. The theme's own border is part
 * of the icon, so its straight runs get clipped along with the background and the
 * corner arcs are painted back on in the same colour (found by walking the icon
 * tree for a {@code HollowRectangleIcon}).
 *
 * <p>Scissor rectangles are in absolute GUI pixels and ignore the pose matrix.
 * That is fine here: FTB Library's screen only ever translates along Z, so a
 * widget's {@code x}/{@code y} are already absolute.
 */
public final class RoundedRect {

    // r only depends on the (static-final) config radius and the window's w/h, so
    // there are at most a handful of distinct values; cache the per-row insets to
    // avoid re-running sqrt() for every corner scanline every frame.
    private static final int[][] INSET_CACHE = new int[SpacingConfig.WINDOW_CORNER_RADIUS + 1][];

    // Render-thread only: caches the border colour found by walking an icon's tree,
    // keyed by identity since the same theme Icon instance is drawn every frame.
    private static final Map<Icon, Color4I> BORDER_COLOR_CACHE = new IdentityHashMap<>();

    private RoundedRect() {
    }

    public static void draw(Icon icon, GuiGraphics graphics, int x, int y, int w, int h, int radius) {
        int r = Math.min(radius, Math.min(w, h) / 2);
        if (r <= 0) {
            icon.draw(graphics, x, y, w, h);
            return;
        }

        int[] insets = insets(r);

        if (h > r * 2) {
            graphics.enableScissor(x, y + r, x + w, y + h - r);
            icon.draw(graphics, x, y, w, h);
            graphics.disableScissor();
        }

        for (int i = 0; i < r; i++) {
            int inset = insets[i];

            graphics.enableScissor(x + inset, y + i, x + w - inset, y + i + 1);
            icon.draw(graphics, x, y, w, h);
            graphics.disableScissor();

            graphics.enableScissor(x + inset, y + h - 1 - i, x + w - inset, y + h - i);
            icon.draw(graphics, x, y, w, h);
            graphics.disableScissor();
        }

        Color4I border = findBorderColor(icon);
        if (border != null && !border.isEmpty()) {
            drawCornerArcs(graphics, x, y, w, h, insets, border);
        }
    }

    /** Horizontal inset of each scanline in a corner band, top row first. */
    private static int[] insets(int r) {
        if (r >= 0 && r < INSET_CACHE.length) {
            int[] cached = INSET_CACHE[r];
            if (cached == null) {
                cached = computeInsets(r);
                INSET_CACHE[r] = cached;
            }
            return cached;
        }
        return computeInsets(r);
    }

    private static int[] computeInsets(int r) {
        int[] insets = new int[r];
        for (int i = 0; i < r; i++) {
            double dy = r - (i + 0.5);
            insets[i] = (int) Math.round(r - Math.sqrt((double) r * r - dy * dy));
        }
        return insets;
    }

    /**
     * Paints the staircase of border pixels that the clipped icon could not draw:
     * for each scanline, the run between this row's inset and the row above's.
     */
    private static void drawCornerArcs(GuiGraphics graphics, int x, int y, int w, int h,
                                       int[] insets, Color4I border) {
        int previous = insets.length;
        for (int i = 0; i < insets.length; i++) {
            int inset = insets[i];
            int runWidth = Math.max(inset + 1, previous) - inset;
            int top = y + i;
            int bottom = y + h - 1 - i;

            border.draw(graphics, x + inset, top, runWidth, 1);
            border.draw(graphics, x + w - inset - runWidth, top, runWidth, 1);
            border.draw(graphics, x + inset, bottom, runWidth, 1);
            border.draw(graphics, x + w - inset - runWidth, bottom, runWidth, 1);

            previous = inset;
        }
    }

    /** The theme's border colour, or null if this background has no border. */
    private static Color4I findBorderColor(Icon icon) {
        if (BORDER_COLOR_CACHE.containsKey(icon)) {
            return BORDER_COLOR_CACHE.get(icon);
        }
        Color4I found = searchBorderColor(icon);
        BORDER_COLOR_CACHE.put(icon, found);
        return found;
    }

    private static Color4I searchBorderColor(Icon icon) {
        if (icon instanceof HollowRectangleIcon rectangle) {
            return rectangle.color;
        }
        if (icon instanceof CombinedIcon combined) {
            for (Icon child : combined.list) {
                Color4I found = searchBorderColor(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (icon instanceof IconWithParent wrapped) {
            return searchBorderColor(wrapped.parent);
        }
        return null;
    }
}
