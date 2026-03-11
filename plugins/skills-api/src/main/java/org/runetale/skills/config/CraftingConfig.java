package org.runetale.skills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record CraftingConfig(
        @Nonnull String anvilBenchId,
        @Nonnull String furnaceBenchId,
        @Nonnull String fletchingBenchId,
        @Nonnull String spinningWheelBenchId,
        long smithingCraftDurationMillis,
        long smeltingCraftDurationMillis,
        long fletchingCraftDurationMillis,
        int maxCraftCount,
        @Nonnull List<Integer> quantityPresets,
        @Nonnull String quantityAllToken,
        @Nonnull String smeltingOutputContainsToken,
        float pageProgressTickSeconds) {

    private static final String RESOURCE_PATH = "Skills/Config/crafting.json";

    @Nonnull
    public static CraftingConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject benchConfig = ConfigResourceLoader.objectValue(root, "bench");
        JsonObject craftConfig = ConfigResourceLoader.objectValue(root, "craft");
        JsonObject smithingConfig = ConfigResourceLoader.objectValue(root, "smithing");
        JsonObject smeltingConfig = ConfigResourceLoader.objectValue(root, "smelting");

        JsonObject fletchingConfig = ConfigResourceLoader.objectValue(root, "fletching");
        String anvilBenchId = ConfigResourceLoader.stringValue(benchConfig, "anvilId", "RuneTale_Anvil");
        String furnaceBenchId = ConfigResourceLoader.stringValue(benchConfig, "furnaceId", "RuneTale_Furnace");
        String fletchingBenchId = ConfigResourceLoader.stringValue(benchConfig, "fletchingId", "RuneTale_Fletching");
        String spinningWheelBenchId = ConfigResourceLoader.stringValue(benchConfig, "spinningWheelId", "RuneTale_Spinning_Wheel");
        long smithingDuration = Math.max(1L, ConfigResourceLoader.longValue(smithingConfig, "craftDurationMillis", 3000L));
        long smeltingDuration = Math.max(1L, ConfigResourceLoader.longValue(smeltingConfig, "craftDurationMillis", 3000L));
        long fletchingDuration = Math.max(1L, ConfigResourceLoader.longValue(fletchingConfig, "craftDurationMillis", 1800L));
        int maxCraftCount = Math.max(1, ConfigResourceLoader.intValue(craftConfig, "maxCount", 999));
        String quantityAllToken = ConfigResourceLoader.stringValue(craftConfig, "quantityAllToken", "ALL");
        String smeltingOutputContainsToken = ConfigResourceLoader.stringValue(smeltingConfig, "outputContainsToken", "bar_");
        float pageProgressTickSeconds = (float) Math.max(0.01D,
                ConfigResourceLoader.doubleValue(root, "pageProgressTickSeconds", 0.05D));

        return new CraftingConfig(
                anvilBenchId,
                furnaceBenchId,
                fletchingBenchId,
                spinningWheelBenchId,
                smithingDuration,
                smeltingDuration,
                fletchingDuration,
                maxCraftCount,
                parseQuantityPresets(craftConfig, "quantityPresets", List.of(1, 5, 10)),
                quantityAllToken,
                smeltingOutputContainsToken,
                pageProgressTickSeconds);
    }

    @Nonnull
    private static List<Integer> parseQuantityPresets(
            @Nonnull JsonObject object,
            @Nonnull String key,
            @Nonnull List<Integer> defaultValues) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return List.copyOf(defaultValues);
        }

        List<Integer> parsed = new ArrayList<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement valueElement : array) {
                if (valueElement == null || valueElement.isJsonNull()) {
                    continue;
                }
                try {
                    int value = valueElement.getAsInt();
                    if (value > 0) {
                        parsed.add(value);
                    }
                } catch (RuntimeException ignored) {
                }
            }
        } else {
            String raw = element.getAsString();
            for (String token : raw.split(",")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                try {
                    int value = Integer.parseInt(trimmed);
                    if (value > 0) {
                        parsed.add(value);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return parsed.isEmpty() ? List.copyOf(defaultValues) : List.copyOf(parsed);
    }
}
