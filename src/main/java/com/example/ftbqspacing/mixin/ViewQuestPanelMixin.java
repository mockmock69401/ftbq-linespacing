package com.example.ftbqspacing.mixin;

import com.example.ftbqspacing.SpacingConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * FTB Quests 1.20.1 (v2001.x) hardcodes the quest description line spacing.
 *
 * Targets in dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel:
 *  - addWidgets():           titleField...setSpacing(9), subtitle field .setSpacing(9),
 *                            and panelText.align(new WidgetLayout.Vertical(0, 1, 2))
 *  - addDescriptionText():   description TextField .setSpacing(9)
 *
 * The class is referenced by string so FTB Quests is not needed at compile time.
 * remap = false because both FTB Quests and FTB Library are unobfuscated mod code.
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel", remap = false)
public abstract class ViewQuestPanelMixin {

    /**
     * Replaces every setSpacing(9) call (title, subtitle, description paragraphs).
     * TextField.setSpacing is called before setText, so the widget height is
     * recalculated with the new spacing automatically.
     */
    @ModifyArg(
            method = {"addWidgets", "addDescriptionText"},
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/TextField;setSpacing(I)Ldev/ftb/mods/ftblibrary/ui/TextField;"
            ),
            require = 0
    )
    private int ftbqls$lineSpacing(int spacing) {
        return SpacingConfig.LINE_SPACING;
    }

    /**
     * Replaces the middle argument of new WidgetLayout.Vertical(0, 1, 2),
     * which controls the vertical gap between description paragraphs.
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
        return SpacingConfig.PARAGRAPH_GAP;
    }
}
