package io.github.mockmock69401.ftbqlinespacing.mixin;

import io.github.mockmock69401.ftbqlinespacing.WordWrap;
import io.github.mockmock69401.ftbqlinespacing.WordWrapTarget;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Lets a single {@link TextField} opt in to word-level line wrapping.
 *
 * <p>{@code setText} wraps through {@code Theme.listFormattedStringToWidth}, which
 * hands off to the vanilla (or Modern UI) string splitter. That splitter breaks
 * CJK text between syllables, so every line ends flush against the panel edge and
 * the paragraph looks justified. When the flag is set — only ever by
 * {@code ViewQuestPanelMixin}, for the quest subtitle and description fields — the call is
 * redirected to {@link WordWrap} instead, which breaks at spaces only.
 *
 * <p>The target carries a full descriptor — Sinytra Connector's mixin adapter parses
 * every {@code @Redirect} INVOKE descriptor and crashes on a name-only target. The
 * descriptor mentions a Minecraft type, so the {@code @At} alone opts back in to
 * remapping ({@code remap = true}) and the refmap carries the production name; the
 * mixin and {@code method} stay {@code remap = false}.
 *
 * <p>{@code method = "setText"} matches both overloads; the {@code String} one just
 * delegates and has no splitter call, hence {@code require = 0}.
 */
@Mixin(value = TextField.class, remap = false)
public abstract class TextFieldMixin implements WordWrapTarget {

    @Unique
    private boolean ftbqls$wordWrap;

    @Override
    public void ftbqls$setWordWrap(boolean wordWrap) {
        this.ftbqls$wordWrap = wordWrap;
    }

    @Redirect(
            method = "setText",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/Theme;listFormattedStringToWidth(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
                    remap = true
            ),
            require = 0
    )
    private List<FormattedText> ftbqls$wrapAtWordBoundaries(Theme theme, FormattedText text, int maxWidth) {
        if (!ftbqls$wordWrap || maxWidth <= 0) {
            return theme.listFormattedStringToWidth(text, maxWidth);
        }
        return WordWrap.split(theme, text, maxWidth);
    }
}
