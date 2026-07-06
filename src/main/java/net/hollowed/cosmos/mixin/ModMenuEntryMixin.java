package net.hollowed.cosmos.mixin;

import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.hollowed.cosmos.Cosmos;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModsScreen.class)
public abstract class ModMenuEntryMixin extends Screen {

    @Shadow private ModListEntry selected;

    @Shadow private int rightPaneX;

    protected ModMenuEntryMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ModListEntry selectedEntry = this.selected;
        if (selectedEntry != null) {
            Mod mod = selectedEntry.getMod();
            int imageOffset = 36;
            Component name = Component.literal(mod.getTranslatedName());
            FormattedText trimmedName = name;
            int maxNameWidth = this.width - (this.rightPaneX + imageOffset);
            if (this.font.width(name) > maxNameWidth) {
                FormattedText ellipsis = FormattedText.of("...");
                trimmedName = FormattedText.composite(this.font.substrByWidth(name, maxNameWidth - this.font.width(ellipsis)), ellipsis);
            }

            // Custom color logic
            int nameColor = 0xFF72B1FF;

            if ("cosmos".equals(mod.getId())) {
                drawContext.text(this.font, Language.getInstance().getVisualOrder(trimmedName), this.rightPaneX + imageOffset, 49, nameColor, true);
                drawContext.blit(RenderPipelines.GUI_TEXTURED, Cosmos.id("cosmos_small_icon.png"), this.rightPaneX + imageOffset + 36, 45, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}