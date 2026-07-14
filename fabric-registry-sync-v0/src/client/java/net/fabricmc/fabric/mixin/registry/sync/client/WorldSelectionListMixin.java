package net.fabricmc.fabric.mixin.registry.sync.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldSelectionListMixin {

    @Shadow @Final private LevelSummary summary;
    @Shadow @Final private Minecraft minecraft;

    // Shift the play button 8px to the right to make room for the warning icon on the left.
    // This targets the last blitSprite call in extractContent, the "else" branch that draws
    // the join sprite when the world has no other issues (compatible, not locked, etc.).
    @Redirect(
        method = "extractContent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
            ordinal = 8  // 9th blitSprite in extractContent, the join/play sprite in the final else branch
        )
    )
    private void shiftJoinSprite(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        net.fabricmc.fabric.impl.registry.sync.client.MissingModsChecker.checkMissingMods(this.summary);
        net.fabricmc.fabric.impl.registry.sync.client.MissingModsAccessor accessor = (net.fabricmc.fabric.impl.registry.sync.client.MissingModsAccessor) this.summary;
        
        int xOffset = accessor.fabric_hasMissingMods() ? 6 : 0;
        graphics.blitSprite(pipeline, sprite, x + xOffset, y, width, height);
    }

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void showModWarning(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovered, float partialTick, CallbackInfo ci) {
        net.fabricmc.fabric.impl.registry.sync.client.MissingModsChecker.checkMissingMods(this.summary);
        net.fabricmc.fabric.impl.registry.sync.client.MissingModsAccessor accessor = (net.fabricmc.fabric.impl.registry.sync.client.MissingModsAccessor) this.summary;

        if (accessor.fabric_hasMissingMods()) {
            if (!isHovered) return;

            int left = ((ObjectSelectionList.Entry<?>)(Object)this).getContentX();
            int top = ((ObjectSelectionList.Entry<?>)(Object)this).getContentY();

            int relX = mouseX - left;
            int relY = mouseY - top;
            boolean isOverIcon = relX >= 0 && relX < 32 && relY >= 0 && relY < 32;

            Identifier warningSprite = isOverIcon
                    ? Identifier.parse("world_list/warning_highlighted")
                    : Identifier.parse("world_list/warning");
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, warningSprite, left, top, 32, 32);

            if (isOverIcon) {
                graphics.setTooltipForNextFrame(minecraft.font.split(Component.literal("Missing Mods: " + accessor.fabric_getMissingMods().size()).withStyle(net.minecraft.ChatFormatting.GOLD), 175), mouseX, mouseY);
            }
        }
    }
}
