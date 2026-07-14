package net.fabricmc.fabric.impl.registry.sync.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class MissingModsChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("MissingModsChecker");
    private static final String FILE_NAME = "mod-list.json";

    public static void checkMissingMods(LevelSummary summary) {
        if (!(summary instanceof MissingModsAccessor accessor)) {
            return;
        }

        if (accessor.fabric_hasCheckedMissingMods()) {
            return;
        }
        accessor.fabric_setCheckedMissingMods();

        Minecraft client = Minecraft.getInstance();
        if (client.getLevelSource() == null) {
            return;
        }

        // Get the world folder
        Path worldDir = client.getLevelSource().getBaseDir().resolve(summary.getLevelId());
        Path modListPath = worldDir.resolve("fabric").resolve(FILE_NAME);

        if (!Files.exists(modListPath)) {
            return;
        }

        Map<String, String[]> missingMods = getMissingModsForPath(modListPath);
        if (missingMods != null) {
            accessor.fabric_setMissingMods(missingMods);
        }
    }

    public static Map<String, String[]> getMissingModsForPath(Path modListPath) {
        if (!Files.exists(modListPath)) {
            return null;
        }

        Map<String, String[]> missingMods = new HashMap<>();

        try (Reader reader = Files.newBufferedReader(modListPath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("mods")) {
                JsonArray mods = root.getAsJsonArray("mods");
                collectMissingMods(mods, missingMods);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse " + FILE_NAME, e);
        }

        return missingMods;
    }

    private static void collectMissingMods(JsonArray mods, Map<String, String[]> missingMods) {
        for (int i = 0; i < mods.size(); i++) {
            JsonObject mod = mods.get(i).getAsJsonObject();
            if (mod.has("id") && mod.has("version")) {
                String id = mod.get("id").getAsString();
                String savedVersionStr = mod.get("version").getAsString();

                if ("minecraft".equals(id)) {
                    // Ignore Minecraft itself since versions handled by world converter
                    continue;
                }

                ModContainer container = FabricLoader.getInstance().getModContainer(id).orElse(null);
                if (container == null) {
                    // Completely missing
                    missingMods.put(id, new String[]{savedVersionStr, "None"});
                } else {
                    // Check for version mismatch
                    String currentVersionStr = container.getMetadata().getVersion().getFriendlyString();
                    if (!savedVersionStr.equals(currentVersionStr)) {
                        missingMods.put(id, new String[]{savedVersionStr, currentVersionStr});
                    }
                }
            }
            if (mod.has("children")) {
                collectMissingMods(mod.getAsJsonArray("children"), missingMods);
            }
        }
    }
}
