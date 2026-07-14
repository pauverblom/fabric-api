package net.fabricmc.fabric.mixin.registry.sync.client;

import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.impl.registry.sync.client.MissingModsAccessor;
import net.fabricmc.fabric.impl.registry.sync.client.MissingModsChecker;
import net.fabricmc.fabric.impl.registry.sync.client.MissingModsWarningScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    @Shadow
    public abstract void openWorld(String levelId, Runnable onFail);

    @org.spongepowered.asm.mixin.Unique
    private static final ThreadLocal<Boolean> bypassCheck = ThreadLocal.withInitial(() -> false);

    @Inject(method = "openWorld", at = @At("HEAD"), cancellable = true)
    private void checkMissingModsBeforeLoading(String levelId, Runnable onFail, CallbackInfo ci) {
        if (bypassCheck.get()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        java.nio.file.Path worldDir = client.getLevelSource().getBaseDir().resolve(levelId);
        java.nio.file.Path modListPath = worldDir.resolve("fabric").resolve("mod-list.json");

        java.util.Map<String, String[]> missingMods = MissingModsChecker.getMissingModsForPath(modListPath);
        if (missingMods != null && !missingMods.isEmpty()) {
            client.gui.setScreen(new MissingModsWarningScreen(
                    onFail,
                    missingMods,
                    () -> {
                        bypassCheck.set(true);
                        try {
                            this.openWorld(levelId, onFail);
                        } finally {
                            bypassCheck.set(false);
                        }
                    }
            ));
            ci.cancel();
        }
    }
}
