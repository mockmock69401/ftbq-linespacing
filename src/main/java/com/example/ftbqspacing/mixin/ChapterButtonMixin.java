package com.example.ftbqspacing.mixin;

import com.example.ftbqspacing.SpacingConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns each chapter entry in the sidebar into a card:
 * bordered box, vertically-centered icon, title + completion percentage,
 * gold accent for the selected chapter.
 */
@Mixin(value = ChapterPanel.ChapterButton.class, remap = false)
public abstract class ChapterButtonMixin extends ChapterPanel.ListButton {

    @Shadow
    @Final
    private Chapter chapter;

    // card palette (dark, matches FTBQ default theme)
    @Unique
    private static final Color4I FILL = Color4I.rgb(0x26292E);
    @Unique
    private static final Color4I FILL_HOVER = Color4I.rgb(0x33383F);
    @Unique
    private static final Color4I FILL_SELECTED = Color4I.rgb(0x2C3138);
    @Unique
    private static final Color4I BORDER = Color4I.rgb(0x4A5058);
    @Unique
    private static final Color4I BORDER_HOVER = Color4I.rgb(0x707882);
    @Unique
    private static final Color4I ACCENT = Color4I.rgb(0xD9B45C);
    @Unique
    private static final Color4I PCT_DONE = Color4I.rgb(0x86D77E);
    @Unique
    private static final Color4I PCT_PARTIAL = Color4I.rgb(0x9AA1A9);
    @Unique
    private static final Color4I BAR_TINT = Color4I.rgb(0x6FBF6A).withAlpha(34);
    @Unique
    private static final Color4I BAR_TINT_DONE = Color4I.rgb(0x6FBF6A).withAlpha(24);

    @Unique
    private static final Color4I CLICK_FLASH = Color4I.rgb(0xFFFFFF);

    @Unique
    private float ftbqls$hoverAnim;
    @Unique
    private long ftbqls$lastFrameNanos;
    @Unique
    private long ftbqls$clickNanos;

    protected ChapterButtonMixin(ChapterPanel panel, Component title, Icon icon) {
        super(panel, title, icon);
    }

    @Unique
    private static Color4I ftbqls$lerp(Color4I a, Color4I b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        return Color4I.rgb(
                a.redi() + Math.round((b.redi() - a.redi()) * t),
                a.greeni() + Math.round((b.greeni() - a.greeni()) * t),
                a.bluei() + Math.round((b.bluei() - a.bluei()) * t));
    }

    @Unique
    private static float ftbqls$smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    @Inject(method = "onClicked", at = @At("HEAD"))
    private void ftbqls$onClick(MouseButton button, CallbackInfo ci) {
        ftbqls$clickNanos = System.nanoTime();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ftbqls$cardHeight(ChapterPanel panel, Chapter c, CallbackInfo ci) {
        if (SpacingConfig.CHAPTER_CARDS) {
            setHeight(SpacingConfig.CARD_HEIGHT);
        }
    }

    @Inject(method = "getActualWidth", at = @At("HEAD"), cancellable = true)
    private void ftbqls$cardWidth(QuestScreen screen, CallbackInfoReturnable<Integer> cir) {
        if (!SpacingConfig.CHAPTER_CARDS) {
            return;
        }
        int indent = chapter.getGroup().isDefaultGroup() ? 0 : 7;
        int textW = Math.max(screen.getTheme().getStringWidth(title), SpacingConfig.SHOW_PROGRESS ? 34 : 0);
        // 4 (left inset) + indent + 6 + 14 (icon) + 6 + text + 6 + 12 (status icon) + 6 (right inset),
        // but never narrower than the configured minimum card width
        cir.setReturnValue(Math.max(textW + indent + 60, SpacingConfig.CARD_MIN_WIDTH));
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void ftbqls$drawCard(GuiGraphics graphics, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        if (!SpacingConfig.CHAPTER_CARDS) {
            return;
        }
        ci.cancel();
        GuiHelper.setupDrawing();

        int indent = chapter.getGroup().isDefaultGroup() ? 0 : 7;
        int cx = x + 4 + indent;
        int cw = w - 10 - indent;  // left inset 4, right inset 6 for visual balance
        int cy = y;
        int ch = h;

        TeamData data = ClientQuestFile.INSTANCE.selfTeamData;
        Chapter selected = ((QuestScreenAccessor) getGui()).ftbqls$getSelectedChapter();
        boolean isSelected = selected != null && selected.id == chapter.id;
        boolean hover = isMouseOver();
        boolean hasChildren = chapter.hasAnyVisibleChildren();
        int progress = hasChildren ? data.getRelativeProgress(chapter) : 0;

        // advance hover animation (time-based, frame-rate independent)
        long now = System.nanoTime();
        if (SpacingConfig.TRANSITION_MS <= 0) {
            ftbqls$hoverAnim = hover ? 1f : 0f;
        } else {
            long frameNs = ftbqls$lastFrameNanos == 0 ? 0 : now - ftbqls$lastFrameNanos;
            float step = Math.min(1f, frameNs / (SpacingConfig.TRANSITION_MS * 1_000_000f));
            ftbqls$hoverAnim = Math.max(0f, Math.min(1f, ftbqls$hoverAnim + (hover ? step : -step)));
        }
        ftbqls$lastFrameNanos = now;
        float t = ftbqls$smoothstep(ftbqls$hoverAnim);

        // subtle scale: grows slightly on hover, dips briefly on click
        float scale = 1f;
        if (SpacingConfig.CARD_SCALE && SpacingConfig.TRANSITION_MS > 0) {
            scale += 0.02f * t;
            if (ftbqls$clickNanos != 0) {
                float ft = (now - ftbqls$clickNanos) / 150_000_000f;
                if (ft < 1f) {
                    scale -= 0.04f * (1f - ft);
                }
            }
        }
        boolean scaled = scale != 1f;
        if (scaled) {
            float pivotX = cx + cw / 2f;
            float pivotY = cy + ch / 2f;
            graphics.pose().pushPose();
            graphics.pose().translate(pivotX, pivotY, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.pose().translate(-pivotX, -pivotY, 0);
        }

        // card body
        Color4I baseFill = isSelected ? FILL_SELECTED : FILL;
        ftbqls$lerp(baseFill, FILL_HOVER, t).draw(graphics, cx, cy, cw, ch);

        // subtle progress tint across the background (inside the border)
        if (SpacingConfig.PROGRESS_BAR && progress > 0) {
            int barW = (int) Math.round((cw - 2) * (progress / 100.0));
            if (barW > 0) {
                (progress >= 100 ? BAR_TINT_DONE : BAR_TINT).draw(graphics, cx + 1, cy + 1, barW, ch - 2);
            }
        }

        // border + selection accent
        Color4I border = isSelected ? ACCENT : ftbqls$lerp(BORDER, BORDER_HOVER, t);
        GuiHelper.drawHollowRect(graphics, cx, cy, cw, ch, border, false);
        if (isSelected) {
            ACCENT.draw(graphics, cx, cy, 2, ch);
        }

        // brief click flash, fading out over ~150ms
        if (SpacingConfig.TRANSITION_MS > 0 && ftbqls$clickNanos != 0) {
            float ft = (now - ftbqls$clickNanos) / 150_000_000f;
            if (ft < 1f) {
                CLICK_FLASH.withAlpha(Math.round(45 * (1f - ft))).draw(graphics, cx, cy, cw, ch);
            } else {
                ftbqls$clickNanos = 0;
            }
        }

        // chapter icon, vertically centered
        icon.draw(graphics, cx + 6, cy + (ch - 14) / 2, 14, 14);

        int textX = cx + 26;
        Color4I titleColor = chapter.getProgressColor(data, !hover);

        if (hasChildren && SpacingConfig.SHOW_PROGRESS) {
            theme.drawString(graphics, title, textX, cy + ch / 2 - 9, titleColor, 0);
            Color4I pctColor = progress >= 100 ? PCT_DONE : PCT_PARTIAL;
            theme.drawString(graphics, progress + " %", textX, cy + ch / 2 + 1, pctColor, 0);
        } else {
            theme.drawString(graphics, title, textX, cy + (ch - theme.getFontHeight()) / 2, titleColor, 0);
        }

        GuiHelper.setupDrawing();

        // right-side status icon (same logic as vanilla)
        if (!hasChildren) {
            ThemeProperties.CLOSE_ICON.get().draw(graphics, cx + cw - 14, cy + (ch - 8) / 2, 8, 8);
        } else if (data.hasUnclaimedRewards(Minecraft.getInstance().player.getUUID(), chapter)) {
            ThemeProperties.ALERT_ICON.get().draw(graphics, cx + cw - 14, cy + (ch - 8) / 2, 8, 8);
        }

        if (scaled) {
            graphics.pose().popPose();
        }
    }
}
