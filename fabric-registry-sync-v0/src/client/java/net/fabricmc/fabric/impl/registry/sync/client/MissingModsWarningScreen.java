package net.fabricmc.fabric.impl.registry.sync.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Map;

public class MissingModsWarningScreen extends Screen {
    private final Runnable pOnFail;
    private final Map<String, String[]> missingMods;
    private final Runnable openAnyway;
    private final Path modsDir;
    protected MissingModListWidget list;

    public MissingModsWarningScreen(Runnable pOnFail, Map<String, String[]> missingMods, Runnable openAnyway) {
        super(Component.literal("Missing Mods Warning").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        this.pOnFail = pOnFail;
        this.missingMods = missingMods;
        this.openAnyway = openAnyway;
        this.modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    @Override
    protected void init() {
        int listWidth = Math.min(440, this.width - 16);
        int listTop = 80;
        int listHeight = this.height - listTop - 45;

        // Add title and warning text
        this.addRenderableWidget(new net.minecraft.client.gui.components.StringWidget(
                this.width / 2 - 200, 10, 400, 15, this.title, this.font));
        
        net.minecraft.client.gui.components.MultiLineTextWidget warningText = new net.minecraft.client.gui.components.MultiLineTextWidget(
                Component.literal("This world was saved with mods that are currently missing or mismatched. Loading it may cause corruption or missing blocks!").withStyle(ChatFormatting.GRAY), this.font);
        warningText.setMaxWidth(400);
        warningText.setCentered(true);
        warningText.setPosition(this.width / 2 - warningText.getWidth() / 2, 30);
        this.addRenderableWidget(warningText);

        // Add scrollable list
        this.list = new MissingModListWidget(this.minecraft, listWidth, listHeight, listTop, 20);
        for (Map.Entry<String, String[]> entry : this.missingMods.entrySet()) {
            this.list.addModEntry(new MissingModListWidget.ModEntry(this.list, entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        this.list.setX((this.width - listWidth) / 2);
        this.addRenderableWidget(this.list); // MUST use addRenderableWidget so it renders!

        int buttonWidth = Math.min(135, this.width / 3 - 20);
        int upperButtonHeight = this.height - 30;
        
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_PROCEED, this::loadAnyway)
                .bounds(this.width / 2 - buttonWidth - 10, upperButtonHeight, buttonWidth, 20)
                .build());
                
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_TO_TITLE, button -> this.pOnFail.run())
                .bounds(this.width / 2 + 10, upperButtonHeight, buttonWidth, 20)
                .build());
    }

    private void loadAnyway(Button b) {
        this.openAnyway.run();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.extractMenuBackground(guiGraphics);
        
        int listWidth = Math.min(440, this.width - 16);
        int rowWidth = Math.min(400, listWidth - 16);
        int left = (this.width - listWidth) / 2 + listWidth / 2 - rowWidth / 2;
        int listTop = 80;
        
        int col1 = left + 10;
        int col2 = left + (int)(rowWidth * 0.45);
        int col3 = left + (int)(rowWidth * 0.75);
        
        guiGraphics.text(this.font, Component.literal("Mod ID").withStyle(ChatFormatting.UNDERLINE), col1, listTop - 12, 0xFFFFFFFF);
        guiGraphics.text(this.font, Component.literal("Saved With").withStyle(ChatFormatting.UNDERLINE), col2, listTop - 12, 0xFFFFFFFF);
        guiGraphics.text(this.font, Component.literal("Installed").withStyle(ChatFormatting.UNDERLINE), col3, listTop - 12, 0xFFFFFFFF);
        
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public static class MissingModListWidget extends ObjectSelectionList<MissingModListWidget.ModEntry> {

        public MissingModListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addModEntry(ModEntry entry) {
            this.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, this.getWidth() - 16);
        }



        public static class ModEntry extends ObjectSelectionList.Entry<ModEntry> {
            private final MissingModListWidget list;
            private final String modid;
            private final String savedVersion;
            private final String installedVersion;

            public ModEntry(MissingModListWidget list, String modid, String savedVersion, String installedVersion) {
                this.list = list;
                this.modid = modid;
                this.savedVersion = savedVersion;
                this.installedVersion = installedVersion;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
                int left = this.getContentX();
                int top = this.getContentY();
                
                int rowWidth = this.list.getRowWidth();
                int col1 = left + 10;
                int col2 = left + (int)(rowWidth * 0.45);
                int col3 = left + (int)(rowWidth * 0.75);
                
                guiGraphics.text(Minecraft.getInstance().font, this.modid, col1, top + 2, 0xFFFFFFFF);
                guiGraphics.text(Minecraft.getInstance().font, this.savedVersion, col2, top + 2, 0xFFAAAAAA);
                
                int installedColor = "None".equals(this.installedVersion) ? 0xFFFF5555 : 0xFFFFFF55;
                guiGraphics.text(Minecraft.getInstance().font, this.installedVersion, col3, top + 2, installedColor);
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.modid + " " + this.savedVersion + " " + this.installedVersion);
            }
        }
    }
}
