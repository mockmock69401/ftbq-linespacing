package io.github.mockmock69401.ftbqlinespacing.mixin;

import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.quest.Chapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ChapterPanel.ChapterButton.class, remap = false)
public interface ChapterButtonAccessor {

    @Accessor("chapter")
    Chapter ftbqls$chapter();
}
