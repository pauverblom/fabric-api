package net.fabricmc.fabric.mixin.registry.sync.client;

import net.fabricmc.fabric.impl.registry.sync.client.MissingModsAccessor;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.Map;

@Mixin(LevelSummary.class)
public class LevelSummaryMixin implements MissingModsAccessor {

    @Unique
    private boolean fabric_checkedMissingMods = false;

    @Unique
    private Map<String, String[]> fabric_missingMods = null;

    @Override
    public boolean fabric_hasCheckedMissingMods() {
        return this.fabric_checkedMissingMods;
    }

    @Override
    public void fabric_setCheckedMissingMods() {
        this.fabric_checkedMissingMods = true;
    }

    @Override
    public boolean fabric_hasMissingMods() {
        return this.fabric_missingMods != null && !this.fabric_missingMods.isEmpty();
    }

    @Override
    public Map<String, String[]> fabric_getMissingMods() {
        return this.fabric_missingMods;
    }

    @Override
    public void fabric_setMissingMods(Map<String, String[]> missingMods) {
        this.fabric_missingMods = missingMods;
    }
}
