package io.github.mockmock69401.ftbqlinespacing.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = QuestScreen.class, remap = false)
public interface QuestScreenAccessor {

    @Accessor("selectedChapter")
    Chapter ftbqls$getSelectedChapter();

    @Accessor("file")
    ClientQuestFile ftbqls$getFile();
}
