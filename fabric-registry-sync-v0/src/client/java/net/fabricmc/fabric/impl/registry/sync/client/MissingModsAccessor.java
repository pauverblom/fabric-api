package net.fabricmc.fabric.impl.registry.sync.client;

import java.util.Map;

/**
 * Duck interface to access missing mods data from a LevelSummary.
 */
public interface MissingModsAccessor {
    boolean fabric_hasCheckedMissingMods();
    void fabric_setCheckedMissingMods();
    boolean fabric_hasMissingMods();
    Map<String, String[]> fabric_getMissingMods();
    void fabric_setMissingMods(Map<String, String[]> missingMods);
}
