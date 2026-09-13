package io.github.mockmock69401.ftbqlinespacing.mixin;

import io.github.mockmock69401.ftbqlinespacing.RoundedRect;
import io.github.mockmock69401.ftbqlinespacing.SpacingConfig;
import io.github.mockmock69401.ftbqlinespacing.WordWrapTarget;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BlankPanel;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FTB Quests 1.20.1 (v2001.x) hardcodes the quest description layout.
 *
 * Targets in dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel:
 *  - addWidgets():           titleField...setSpacing(9)          (ordinal 0 = title)
 *                            subtitle field .setSpacing(9)        (ordinal 1 = subtitle)
 *                            panelText.setPosAndSize(...)         (BlankPanel #503, ordinal 3)
 *                            new WidgetLayout.Vertical(0, 1, 2)   (paragraph gap = arg index 1)
 *  - addDescriptionText():   description TextField .setSpacing(9)
 *
 * The class is referenced by string so FTB Quests is not needed at compile time.
 * remap = false because both FTB Quests and FTB Library are unobfuscated mod code.
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel", remap = false)
public abstract class ViewQuestPanelMixin {

    @Shadow
    private TextField titleField;

    @Shadow
    private BlankPanel panelContent;

    /**
     * Description paragraphs (addDescriptionText only). The title/subtitle
     * setSpacing calls in addWidgets are handled by the redirects below (the
     * subtitle one also applies the configured line spacing), so this must not target addWidgets
     * (a @ModifyArg and a @Redirect cannot share the same invocation).
     *
     * <p>This is the last call on the description field before setText, so it also
     * doubles as the hook that opts the field in to word-level wrapping. Without
     * it the splitter breaks CJK text between syllables, filling every line to the
     * panel edge so the paragraph reads as justified rather than flush left.
     */
    @Redirect(
            method = "addDescriptionText",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/TextField;setSpacing(I)Ldev/ftb/mods/ftblibrary/ui/TextField;"
            ),
            require = 0
    )
    private TextField ftbqls$lineSpacing(TextField field, int spacing) {
        if (SpacingConfig.DESC_WORD_WRAP && field instanceof WordWrapTarget target) {
            target.ftbqls$setWordWrap(true);
        }
        return field.setSpacing(SpacingConfig.LINE_SPACING);
    }

    /**
     * Replaces the middle argument of new WidgetLayout.Vertical(0, 1, 2), the gap
     * between the widgets stacked in the text panel.
     *
     * <p>Each description entry is its own TextField, whose height counts its last
     * line as only {@code fontHeight - 1} px rather than a full line pitch. The
     * step from one entry to the next is therefore {@code fontHeight - 1 + gap},
     * while wrapped lines (or {@code \n} inside one entry) step by the line spacing.
     * Vanilla's gap of 1 makes the two equal at spacing 9; the gap is derived from
     * the configured spacing the same way, so a quest written as many one-line
     * entries looks the same as one written as a single wrapped entry.
     * {@code paragraph-gap} is extra space on top of that.
     */
    @ModifyArg(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/WidgetLayout$Vertical;<init>(III)V"
            ),
            index = 1,
            require = 0
    )
    private int ftbqls$paragraphGap(int gap) {
        int lineGap = SpacingConfig.LINE_SPACING - (Theme.DEFAULT.getFontHeight() - 1);
        return Math.max(0, lineGap) + SpacingConfig.PARAGRAPH_GAP;
    }

    /**
     * Recomputes the modal window height. There are two branches in addWidgets,
     * each ending in a {@code Math.min(wanted, screen-10)}:
     *  - ordinal 1 (no subtitle/description): vanilla {@code min(contentHeight,
     *    screen-10)} — ignores the title band entirely, so the ContentPanel (which
     *    sits at {@code posY}) overflows the window and the quest is cut / scrolls.
     *  - ordinal 2 (with description): vanilla {@code min(contentHeight +
     *    titleHeight + 12, screen-10)} — assumes the panel sits at
     *    {@code titleHeight + 8}, which a scaled/padded title breaks.
     * Both are rebuilt from the panel's real {@code posY} so the window always
     * covers title + content, floored by {@code min-window-height} and clamped to
     * the screen. (ordinal 0 is icon sizing and is left alone.)
     */
    @Redirect(
            method = "addWidgets",
            at = @At(value = "INVOKE", ordinal = 2, target = "Ljava/lang/Math;min(II)I"),
            require = 0
    )
    private int ftbqls$modalHeightWithDesc(int vanillaContentHeight, int screenCap) {
        return ftbqls$modalHeight(vanillaContentHeight, screenCap, SpacingConfig.DESC_PADDING_BOTTOM);
    }

    @Redirect(
            method = "addWidgets",
            at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/lang/Math;min(II)I"),
            require = 0
    )
    private int ftbqls$modalHeightNoDesc(int vanillaContentHeight, int screenCap) {
        return ftbqls$modalHeight(vanillaContentHeight, screenCap, 0);
    }

    @Unique
    private int ftbqls$modalHeight(int vanillaFallback, int screenCap, int bottomPad) {
        int wanted = panelContent == null
                ? vanillaFallback + bottomPad
                : panelContent.getContentHeight() + panelContent.posY + 4 + bottomPad;
        wanted = Math.max(wanted, SpacingConfig.MIN_WINDOW_HEIGHT);
        return Math.min(wanted, screenCap);
    }

    /**
     * Fits the ContentPanel inside the modal. Vanilla sets its height to
     * {@code this.height - 17}, a constant tuned for the vanilla title band; with a
     * scaled/padded title the panel's Y is larger, so that height overflows the
     * bottom of the window and triggers a scrollbar. Sizing it to
     * {@code this.height - panel.posY} makes the panel end exactly at the window
     * bottom instead. {@code this.height} is recovered from the passed value, which
     * is {@code this.height - 17}. This is the last BlankPanel.setHeight in
     * addWidgets (ordinal 4).
     */
    @Redirect(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 4,
                    target = "Ldev/ftb/mods/ftblibrary/ui/BlankPanel;setHeight(I)V"
            ),
            require = 0
    )
    private void ftbqls$fitContentPanel(BlankPanel panel, int vanillaHeight) {
        int modalHeight = vanillaHeight + 17;
        panel.setHeight(Math.max(1, modalHeight - panel.posY));
    }

    /**
     * Y of {@code titleField.setPosAndSize(27, 4, ...)} (the first TextField
     * setPosAndSize in addWidgets): the gap above the title. Vanilla is 4. The
     * content panel below is pushed down by the matching amount in
     * {@link #ftbqls$contentTop} so the title keeps equal padding above/below.
     */
    @ModifyArg(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Ldev/ftb/mods/ftblibrary/ui/TextField;setPosAndSize(IIII)Ldev/ftb/mods/ftblibrary/ui/Widget;"
            ),
            index = 1,
            require = 0
    )
    private int ftbqls$titleY(int y) {
        return SpacingConfig.TITLE_PADDING_Y;
    }

    /**
     * Y of the first BlankPanel.setPosAndSize in addWidgets (the ContentPanel),
     * originally {@code max(16, titleField.height + 8)}. The vanilla title band is
     * 4 px above + 4 px below; shifting the content down by twice the change in
     * top padding preserves the equal below-title gap.
     */
    @ModifyArg(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Ldev/ftb/mods/ftblibrary/ui/BlankPanel;setPosAndSize(IIII)Ldev/ftb/mods/ftblibrary/ui/Widget;"
            ),
            index = 1,
            require = 0
    )
    private int ftbqls$contentTop(int y) {
        return y + 2 * (SpacingConfig.TITLE_PADDING_Y - 4);
    }

    /**
     * Title field (first setSpacing in addWidgets). setScale is applied before
     * setText, so the field re-measures at the scaled size. Titles are short and
     * centre-aligned and the panel width adapts to the field width, so scaling
     * keeps the title centred without overflowing.
     *
     * <p>The title keeps the vanilla line spacing: {@code line-spacing} is meant for
     * the subtitle/description body text, and a large value would spread a wrapped
     * title across a scaled-up band.
     */
    @Redirect(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Ldev/ftb/mods/ftblibrary/ui/TextField;setSpacing(I)Ldev/ftb/mods/ftblibrary/ui/TextField;"
            ),
            require = 0
    )
    private TextField ftbqls$titleSetup(TextField field, int spacing) {
        if (SpacingConfig.TITLE_SCALE != 1.0f) {
            field.setScale(SpacingConfig.TITLE_SCALE);
        }
        return field.setSpacing(spacing);
    }

    /**
     * Subtitle field (second setSpacing in addWidgets). The subtitle is pinned to
     * the text-panel width (minWidth == maxWidth). TextField wraps at maxWidth in
     * unscaled font pixels but renders at {@code scale}x, so the wrap width is
     * divided by the scale to keep the rendered text inside the panel, while
     * minWidth is left at the full panel width so the centred text stays centred.
     *
     * <p>Like the description, the subtitle opts in to word-level wrapping here so
     * CJK text doesn't break between syllables. {@link io.github.mockmock69401.ftbqlinespacing.WordWrap}
     * trims trailing spaces from each line, so centring is unaffected.
     */
    @Redirect(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 1,
                    target = "Ldev/ftb/mods/ftblibrary/ui/TextField;setSpacing(I)Ldev/ftb/mods/ftblibrary/ui/TextField;"
            ),
            require = 0
    )
    private TextField ftbqls$subtitleSetup(TextField field, int spacing) {
        if (SpacingConfig.DESC_WORD_WRAP && field instanceof WordWrapTarget target) {
            target.ftbqls$setWordWrap(true);
        }
        if (SpacingConfig.SUBTITLE_SCALE != 1.0f) {
            int full = field.maxWidth; // = panel width (minWidth == maxWidth here)
            field.setMinWidth(full);
            field.setMaxWidth(Math.max(1, Math.round(full / SpacingConfig.SUBTITLE_SCALE)));
            field.setScale(SpacingConfig.SUBTITLE_SCALE);
        }
        return field.setSpacing(SpacingConfig.LINE_SPACING);
    }

    /**
     * Insets the subtitle/description text panel. Both fields read this panel's
     * width for their own max width right after this call, so narrowing it here
     * re-wraps them to match the padding.
     */
    @Redirect(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 3,
                    target = "Ldev/ftb/mods/ftblibrary/ui/BlankPanel;setPosAndSize(IIII)Ldev/ftb/mods/ftblibrary/ui/Widget;"
            ),
            require = 0
    )
    private Widget ftbqls$padTextPanel(BlankPanel panel, int x, int y, int w, int h) {
        int padX = SpacingConfig.DESC_PADDING_X;
        int padTop = SpacingConfig.DESC_PADDING_TOP;
        return panel.setPosAndSize(x + padX, y + padTop, Math.max(1, w - padX * 2), h);
    }

    /**
     * Rounds the corners of the quest window. This is the first Icon.draw in
     * drawBackground (ordinal 0) — the whole-window background, which in the
     * default theme is a hollow 1px border over a tiled texture. The descriptor is
     * omitted from the selector so no remapped Minecraft type appears in it.
     */
    @Redirect(
            method = "drawBackground",
            at = @At(value = "INVOKE", ordinal = 0, target = "Ldev/ftb/mods/ftblibrary/icon/Icon;draw"),
            require = 0
    )
    private void ftbqls$roundWindowCorners(Icon icon, GuiGraphics graphics, int x, int y, int w, int h) {
        RoundedRect.draw(icon, graphics, x, y, w, h, SpacingConfig.WINDOW_CORNER_RADIUS);
    }

    /**
     * Vertically centres the quest icon against the title. drawBackground draws it
     * top-aligned at a fixed {@code y + 4}, which drifts out of alignment once the
     * title is scaled or given vertical padding. This is the second Icon.draw in
     * the method (ordinal 1); the first draws the panel background. The descriptor
     * is omitted from the selector so no remapped Minecraft type appears in it.
     */
    @Redirect(
            method = "drawBackground",
            at = @At(value = "INVOKE", ordinal = 1, target = "Ldev/ftb/mods/ftblibrary/icon/Icon;draw"),
            require = 0
    )
    private void ftbqls$centerTitleIcon(Icon icon, GuiGraphics graphics, int x, int y, int w, int h) {
        int cy = y;
        if (titleField != null) {
            cy = titleField.getY() + (titleField.height - h) / 2;
        }
        // inset from the left edge (positive x moves right)
        icon.draw(graphics, x + SpacingConfig.TITLE_ICON_PADDING_X, cy, w, h);
    }

    /**
     * Vertically centres the top-right buttons (close = ordinal 0, pin = ordinal 1
     * of Button.setPosAndSize) against the title and insets them from the right
     * edge (x − padding). Vanilla pins them to {@code y=4}; ordinals 2/3 are the
     * dependency arrows and are left alone. titleField.posY is in the same
     * coordinate space as these siblings (both children of the modal).
     */
    @Redirect(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Ldev/ftb/mods/ftblibrary/ui/Button;setPosAndSize(IIII)Ldev/ftb/mods/ftblibrary/ui/Widget;"
            ),
            require = 0
    )
    private Widget ftbqls$centerCloseButton(Button button, int x, int y, int w, int h) {
        return button.setPosAndSize(x - SpacingConfig.TITLE_ICON_PADDING_X, ftbqls$titleCenterY(y, h), w, h);
    }

    @Redirect(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 1,
                    target = "Ldev/ftb/mods/ftblibrary/ui/Button;setPosAndSize(IIII)Ldev/ftb/mods/ftblibrary/ui/Widget;"
            ),
            require = 0
    )
    private Widget ftbqls$centerPinButton(Button button, int x, int y, int w, int h) {
        return button.setPosAndSize(x - SpacingConfig.TITLE_ICON_PADDING_X, ftbqls$titleCenterY(y, h), w, h);
    }

    @Unique
    private int ftbqls$titleCenterY(int fallback, int h) {
        return titleField == null ? fallback : titleField.posY + (titleField.height - h) / 2;
    }

    /**
     * Insets the ◄ dependencies arrow (X arg of the third Button.setPosAndSize,
     * ordinal 2) from the left edge. Vanilla places it flush at {@code x = 0}.
     */
    @ModifyArg(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 2,
                    target = "Ldev/ftb/mods/ftblibrary/ui/Button;setPosAndSize(IIII)Ldev/ftb/mods/ftblibrary/ui/Widget;"
            ),
            index = 0,
            require = 0
    )
    private int ftbqls$prevArrowX(int x) {
        return x + SpacingConfig.ARROW_PADDING_X;
    }

    /**
     * Insets the ► dependants arrow (X arg of the fourth Button.setPosAndSize,
     * ordinal 3) from the right edge. Vanilla places it flush at {@code x =
     * width - 13}.
     */
    @ModifyArg(
            method = "addWidgets",
            at = @At(
                    value = "INVOKE",
                    ordinal = 3,
                    target = "Ldev/ftb/mods/ftblibrary/ui/Button;setPosAndSize(IIII)Ldev/ftb/mods/ftblibrary/ui/Widget;"
            ),
            index = 0,
            require = 0
    )
    private int ftbqls$nextArrowX(int x) {
        return x - SpacingConfig.ARROW_PADDING_X;
    }
}
