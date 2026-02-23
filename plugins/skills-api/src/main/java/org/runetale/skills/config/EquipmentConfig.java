package org.runetale.skills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record EquipmentConfig(
        @Nonnull String tagSkillRequired,
        @Nonnull String tagLevelRequirement,
        @Nonnull String tagValueSeparator,
        boolean requireLocationMatchBetweenTags,
        int defaultRequiredLevel,
        boolean enforceArmor,
        boolean enforceActiveHand,
        boolean enforceActiveHandReconcile,
        boolean enforceToolUseBlockDamage,
        boolean enforceToolUseBreakBlock,
        boolean enforceToolUseEntityDamage,
        int activeSectionHotbar,
        int activeSectionTools,
        int activeSelectionSlotsHotbar,
        int activeSelectionSlotsTools,
        float armorScanTickSeconds,
        long notificationCooldownMillis,
        @Nonnull String notificationMessageTemplate,
        @Nonnull Map<String, List<String>> locationAliases,
        @Nonnull String debugPluginKey) {

    private static final String RESOURCE_PATH = "Skills/Config/equipment.json";

    @Nonnull
    public static EquipmentConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject tagConfig = ConfigResourceLoader.objectValue(root, "tag");
        JsonObject enforceConfig = ConfigResourceLoader.objectValue(root, "enforce");
        JsonObject toolUseConfig = ConfigResourceLoader.objectValue(enforceConfig, "toolUse");
        JsonObject activeSectionConfig = ConfigResourceLoader.objectValue(root, "activeSection");
        JsonObject activeSelectionSlotsConfig = ConfigResourceLoader.objectValue(root, "activeSelectionSlots");
        JsonObject notificationConfig = ConfigResourceLoader.objectValue(root, "notification");
        JsonObject debugConfig = ConfigResourceLoader.objectValue(root, "debug");

        int defaultRequiredLevel = Math.max(1, ConfigResourceLoader.intValue(root, "defaultRequiredLevel", 1));

        return new EquipmentConfig(
                ConfigResourceLoader.stringValue(tagConfig, "skillRequired", "EquipSkillRequirement"),
                ConfigResourceLoader.stringValue(tagConfig, "levelRequirement", "EquipLevelRequirement"),
                ConfigResourceLoader.stringValue(tagConfig, "valueSeparator", ":"),
                ConfigResourceLoader.booleanValue(root, "requireLocationMatchBetweenTags", false),
                defaultRequiredLevel,
                ConfigResourceLoader.booleanValue(enforceConfig, "armor", true),
                ConfigResourceLoader.booleanValue(enforceConfig, "activeHand", false),
                ConfigResourceLoader.booleanValue(enforceConfig, "activeHandReconcile", false),
                ConfigResourceLoader.booleanValue(toolUseConfig, "blockDamage", true),
                ConfigResourceLoader.booleanValue(toolUseConfig, "breakBlock", true),
                ConfigResourceLoader.booleanValue(toolUseConfig, "entityDamage", true),
                ConfigResourceLoader.intValue(activeSectionConfig, "hotbar", -1),
                ConfigResourceLoader.intValue(activeSectionConfig, "tools", -8),
                Math.max(1, ConfigResourceLoader.intValue(activeSelectionSlotsConfig, "hotbar", 9)),
                Math.max(1, ConfigResourceLoader.intValue(activeSelectionSlotsConfig, "tools", 9)),
                (float) Math.max(0.05D, ConfigResourceLoader.doubleValue(root, "armorScanTickSeconds", 0.25D)),
                Math.max(0L, ConfigResourceLoader.longValue(notificationConfig, "cooldownMillis", 1500L)),
                ConfigResourceLoader.stringValue(
                        notificationConfig,
                        "messageTemplate",
                        "[Skills] %s level %d/%d required to equip %s in %s."),
                parseLocationAliases(ConfigResourceLoader.objectValue(root, "locationAliases")),
                ConfigResourceLoader.stringValue(debugConfig, "pluginKey", "skills-equipment"));
    }

    @Nonnull
    private static Map<String, List<String>> parseLocationAliases(@Nonnull JsonObject aliasesObject) {
        Map<String, List<String>> aliasMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : aliasesObject.entrySet()) {
            String locationKey = normalizeToken(entry.getKey());
            if (locationKey.isEmpty()) {
                continue;
            }

            List<String> aliases = parseTokens(entry.getValue());
            if (aliases.isEmpty()) {
                continue;
            }

            aliasMap.put(locationKey, aliases);
        }

        if (aliasMap.isEmpty()) {
            aliasMap.put("mainhand", List.of("mainhand", "main_hand", "hand", "right_hand", "right"));
            aliasMap.put("offhand", List.of("offhand", "off_hand", "left_hand", "left"));
            aliasMap.put("head", List.of("head", "helmet"));
            aliasMap.put("chest", List.of("chest", "body", "torso"));
            aliasMap.put("hands", List.of("hands", "gloves"));
            aliasMap.put("legs", List.of("legs", "pants"));
        }

        return Map.copyOf(aliasMap);
    }

    @Nonnull
    private static List<String> parseTokens(@Nonnull JsonElement element) {
        List<String> parsed = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement tokenElement : element.getAsJsonArray()) {
                if (tokenElement == null || tokenElement.isJsonNull()) {
                    continue;
                }
                String normalized = normalizeToken(tokenElement.getAsString());
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        } else if (element.isJsonPrimitive()) {
            String raw = element.getAsString();
            for (String token : raw.split(",")) {
                String normalized = normalizeToken(token);
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        }

        return parsed.isEmpty() ? List.of() : List.copyOf(parsed);
    }

    @Nonnull
    private static String normalizeToken(@Nonnull String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
