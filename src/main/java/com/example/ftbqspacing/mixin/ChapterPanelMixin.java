package com.example.ftbqspacing.mixin;

import com.example.ftbqspacing.SlideAnim;
import com.example.ftbqspacing.SpacingConfig;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import net.minecraft.client.gui.GuiGraphics;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 1) Replaces the zero-gap WidgetLayout.VERTICAL with a spaced layout for cards.
 * 2) Animates chapter list entries sliding into their new positions after a
 *    group is expanded/collapsed (refreshWidgets recreates all buttons, so
 *    entries are tracked by chapter/group id across rebuilds).
 */
@Mixin(value = ChapterPanel.class, remap = false)
public abstract class ChapterPanelMixin extends Panel {

    @Unique
    private static final WidgetLayout CARD_LAYOUT =
            new WidgetLayout.Vertical(2, SpacingConfig.CARD_GAP, 4);

    /** last laid-out target Y per stable entry key ("c"+chapterId / "g"+groupId) */
    @Unique
    private final Map<String, Integer> ftbqls$lastY = new HashMap<>();

    /** running slide animations, keyed by stable entry id so they survive
     *  widget recreation and re-layouts without resetting */
    @Unique
    private final Map<String, SlideAnim> ftbqls$slides = new HashMap<>();

    protected ChapterPanelMixin(Panel panel) {
        super(panel);
    }

    @Redirect(
            method = "alignWidgets",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/ftb/mods/ftblibrary/ui/WidgetLayout;VERTICAL:Ldev/ftb/mods/ftblibrary/ui/WidgetLayout;",
                    opcode = Opcodes.GETSTATIC
            ),
            require = 0
    )
    private WidgetLayout ftbqls$cardLayout() {
        return SpacingConfig.CHAPTER_CARDS ? CARD_LAYOUT : WidgetLayout.VERTICAL;
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"), require = 0)
    private void ftbqls$captureSlides(CallbackInfo ci) {
        if (!SpacingConfig.CHAPTER_CARDS || SpacingConfig.TRANSITION_MS <= 0) {
            return;
        }
        long now = System.nanoTime();
        boolean firstLayout = ftbqls$lastY.isEmpty();
        Map<String, Integer> current = new HashMap<>();

        for (Widget w : widgets) {
            String key = ftbqls$keyOf(w);
            if (key == null) {
                continue;
            }
            int toY = w.getPosY();
            current.put(key, toY);

            SlideAnim anim = ftbqls$slides.get(key);
            Integer prevY = ftbqls$lastY.get(key);

            if (anim != null) {
                // an animation is already running for this entry:
                // keep it going (re-layouts must not reset it)
                anim.widget = w; // widget may have been recreated
                if (anim.to != toY) {
                    // target moved again - retarget smoothly from the
                    // currently displayed position
                    anim.from = anim.currentY(now);
                    anim.to = toY;
                    anim.start = now;
                }
                w.setY(anim.currentY(now));
            } else if (prevY != null) {
                if (prevY != toY) {
                    // entry moved (rows above expanded/collapsed)
                    anim = new SlideAnim(w, now, prevY, toY);
                    ftbqls$slides.put(key, anim);
                    w.setY(prevY); // apply start position NOW to avoid a
                                   // one-frame flash at the final position
                }
            } else if (!firstLayout) {
                // newly revealed entry - slide down into place
                anim = new SlideAnim(w, now, toY - 12, toY);
                ftbqls$slides.put(key, anim);
                w.setY(toY - 12);
            }
        }

        // drop animations for entries that no longer exist (collapsed away)
        ftbqls$slides.keySet().retainAll(current.keySet());
        ftbqls$lastY.clear();
        ftbqls$lastY.putAll(current);
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), require = 0)
    private void ftbqls$advanceSlides(GuiGraphics graphics, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        if (ftbqls$slides.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Iterator<SlideAnim> it = ftbqls$slides.values().iterator();
        while (it.hasNext()) {
            SlideAnim a = it.next();
            if (a.isDone(now)) {
                a.widget.setY(a.to);
                it.remove();
            } else {
                a.widget.setY(a.currentY(now));
            }
        }
    }

    @Unique
    private String ftbqls$keyOf(Widget w) {
        if (w instanceof ChapterPanel.ChapterButton cb) {
            return "c" + ((ChapterButtonAccessor) cb).ftbqls$chapter().id;
        }
        if (w instanceof ChapterPanel.ChapterGroupButton gb) {
            return "g" + gb.group.id;
        }
        return null;
    }
}
