package io.github.mockmock69401.ftbqlinespacing.mixin;

import io.github.mockmock69401.ftbqlinespacing.SpacingConfig;
import com.mojang.math.Axis;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animated group headers: hover highlight and text color fade in/out,
 * and the collapse arrow rotates smoothly between collapsed (right)
 * and expanded (down) states.
 *
 * <p>{@code priority = 900} so this HEAD cancel runs before other add-ons' — see
 * {@link ChapterButtonMixin}.
 */
@Mixin(value = ChapterPanel.ChapterGroupButton.class, remap = false, priority = 900)
public abstract class ChapterGroupButtonMixin extends ChapterPanel.ListButton {

    @Unique
    private static final Color4I TEXT_IDLE = Color4I.rgb(0xAAAAAA);
    @Unique
    private static final Color4I TEXT_HOVER = Color4I.rgb(0xFFFFFF);

    @Unique
    private float ftbqls$hoverAnim;
    @Unique
    private float ftbqls$collapseAnim = -1f; // -1 = uninitialized, snap on first frame
    @Unique
    private long ftbqls$lastFrameNanos;

    protected ChapterGroupButtonMixin(ChapterPanel panel, Component title, Icon icon) {
        super(panel, title, icon);
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void ftbqls$drawAnimated(GuiGraphics graphics, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        if (!SpacingConfig.CHAPTER_CARDS) {
            return;
        }
        ci.cancel();
        GuiHelper.setupDrawing();

        ChapterPanel.ChapterGroupButton self = (ChapterPanel.ChapterGroupButton) (Object) this;
        boolean hover = isMouseOver();
        boolean expanded = !self.group.isGuiCollapsed();

        // advance animations (time-based)
        long now = System.nanoTime();
        float target = expanded ? 1f : 0f;
        if (SpacingConfig.TRANSITION_MS <= 0) {
            ftbqls$hoverAnim = hover ? 1f : 0f;
            ftbqls$collapseAnim = target;
        } else {
            if (ftbqls$collapseAnim < 0f) {
                ftbqls$collapseAnim = target; // first frame: no rotation animation
            }
            long frameNs = ftbqls$lastFrameNanos == 0 ? 0 : now - ftbqls$lastFrameNanos;
            float step = Math.min(1f, frameNs / (SpacingConfig.TRANSITION_MS * 1_000_000f));
            ftbqls$hoverAnim = Math.max(0f, Math.min(1f, ftbqls$hoverAnim + (hover ? step : -step)));
            ftbqls$collapseAnim = Math.max(0f, Math.min(1f,
                    ftbqls$collapseAnim + (expanded ? step : -step)));
        }
        ftbqls$lastFrameNanos = now;
        float t = ftbqls$smoothstep(ftbqls$hoverAnim);
        float ct = ftbqls$smoothstep(ftbqls$collapseAnim);

        // hover highlight, fading
        int highlightAlpha = Math.round(40 * t);
        if (highlightAlpha > 0) {
            Color4I.WHITE.withAlpha(highlightAlpha).draw(graphics, x + 1, y, w - 2, h);
        }

        Color4I color = ftbqls$lerp(TEXT_IDLE, TEXT_HOVER, t);

        // arrow: down-pointing (expanded) icon, rotated -90 deg when collapsed
        float angle = -90f * (1f - ct);
        float pivotX = x + 7f;
        float pivotY = y + 9f;
        graphics.pose().pushPose();
        graphics.pose().translate(pivotX, pivotY, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.pose().translate(-pivotX, -pivotY, 0);
        ChapterPanel.ARROW_EXPANDED.withColor(color).draw(graphics, x + 3, y + 5, 8, 8);
        graphics.pose().popPose();

        theme.drawString(graphics, title, x + 15, y + 5, color, 0);

        if (ClientQuestFile.INSTANCE != null && ClientQuestFile.INSTANCE.canEdit()) {
            ThemeProperties.ADD_ICON.get().draw(graphics, x + w - 14, y + 3, 12, 12);
        }
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
}
