package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public record CraftingConfig(
        @Nonnull String anvilBenchId,
        @Nonnull String furnaceBenchId,
        long smithingCraftDurationMillis,
        long smeltingCraftDurationMillis,
        int maxCraftCount,
        @Nonnull List<Integer> quantityPresets,
        @Nonnull String quantityAllToken,
        @Nonnull String smeltingOutputContainsToken,
        float pageProgressTickSeconds) {

    private static final String RESOURCE_PATH = "Skills/Config/crafting.properties";

    @Nonnull
    public static CraftingConfig load() {
        Properties properties = ConfigResourceLoader.loadProperties(RESOURCE_PATH);

        String anvilBenchId = ConfigResourceLoader.stringValue(properties, "bench.anvil.id", "RuneTale_Anvil");
        String furnaceBenchId = ConfigResourceLoader.stringValue(properties, "bench.furnace.id", "RuneTale_Furnace");
        long smithingDuration = Math.max(1L, ConfigResourceLoader.longValue(properties, "smithing.craftDurationMillis", 3000L));
        long smeltingDuration = Math.max(1L, ConfigResourceLoader.longValue(properties, "smelting.craftDurationMillis", 3000L));
        int maxCraftCount = Math.max(1, ConfigResourceLoader.intValue(properties, "craft.maxCount", 999));
        String quantityAllToken = ConfigResourceLoader.stringValue(properties, "craft.quantityAllToken", "ALL");
        String smeltingOutputContainsToken = ConfigResourceLoader.stringValue(properties, "smelting.outputContainsToken", "bar_");
        float pageProgressTickSeconds = (float) Math.max(0.01D,
                ConfigResourceLoader.doubleValue(properties, "pageProgressTickSeconds", 0.05D));

        return new CraftingConfig(
                anvilBenchId,
                furnaceBenchId,
                smithingDuration,
                smeltingDuration,
                maxCraftCount,
                parseIntCsv(properties, "craft.quantityPresets", List.of(1, 5, 10)),
                quantityAllToken,
                smeltingOutputContainsToken,
                pageProgressTickSeconds);
    }

    @Nonnull
    private static List<Integer> parseIntCsv(
            @Nonnull Properties properties,
            @Nonnull String key,
            @Nonnull List<Integer> defaultValues) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return List.copyOf(defaultValues);
        }

        List<Integer> parsed = new ArrayList<>();
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

        return parsed.isEmpty() ? List.copyOf(defaultValues) : List.copyOf(parsed);
    }
}
