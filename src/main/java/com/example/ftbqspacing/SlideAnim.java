package com.example.ftbqspacing;

import dev.ftb.mods.ftblibrary.ui.Widget;

/**
 * A single vertical slide animation for a chapter-list entry.
 * Keyed by chapter/group id externally, so it survives both widget
 * recreation (refreshWidgets) and mid-flight re-layouts (alignWidgets).
 */
public final class SlideAnim {

    public Widget widget;
    public long start;
    public int from;
    public int to;

    public SlideAnim(Widget widget, long start, int from, int to) {
        this.widget = widget;
        this.start = start;
        this.from = from;
        this.to = to;
    }

    public boolean isDone(long now) {
        return now - start >= durationNs();
    }

    /** Current eased Y position at the given time. */
    public int currentY(long now) {
        float t = (now - start) / (float) durationNs();
        if (t >= 1f) {
            return to;
        }
        if (t <= 0f) {
            return from;
        }
        float s = t * t * (3f - 2f * t);
        return Math.round(from + (to - from) * s);
    }

    private static long durationNs() {
        return (SpacingConfig.TRANSITION_MS + 60) * 1_000_000L;
    }
}
