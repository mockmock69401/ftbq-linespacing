package io.github.mockmock69401.ftbqlinespacing;

/**
 * Duck interface mixed into {@code dev.ftb.mods.ftblibrary.ui.TextField} by
 * {@code TextFieldMixin}, so a single text field can be opted in to word-level
 * line wrapping without changing how every other FTB Library text field wraps.
 */
public interface WordWrapTarget {

    /** Marks this text field as one that may only break lines at spaces. */
    void ftbqls$setWordWrap(boolean wordWrap);
}
