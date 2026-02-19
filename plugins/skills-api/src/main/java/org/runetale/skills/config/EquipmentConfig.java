package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

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

    private static final String RESOURCE_PATH = "Skills/Config/equipment.properties";
    private static final String LOCATION_ALIAS_PREFIX = "locationAlias.";

    @Nonnull
    public static EquipmentConfig load(@Nonnull Path externalConfigRoot) {
        Properties properties = ConfigResourceLoader.loadProperties(RESOURCE_PATH, externalConfigRoot);

        int defaultRequiredLevel = Math.max(1, ConfigResourceLoader.intValue(properties, "defaultRequiredLevel", 1));

        return new EquipmentConfig(
                ConfigResourceLoader.stringValue(properties, "tag.skillRequired", "EquipSkillRequirement"),
                ConfigResourceLoader.stringValue(properties, "tag.levelRequirement", "EquipLevelRequirement"),
                ConfigResourceLoader.stringValue(properties, "tag.valueSeparator", ":"),
                booleanValue(properties, "requireLocationMatchBetweenTags", false),
                defaultRequiredLevel,
                booleanValue(properties, "enforce.armor", true),
                booleanValue(properties, "enforce.activeHand", false),
                booleanValue(properties, "enforce.activeHandReconcile", false),
                booleanValue(properties, "enforce.toolUse.blockDamage", true),
                booleanValue(properties, "enforce.toolUse.breakBlock", true),
                booleanValue(properties, "enforce.toolUse.entityDamage", true),
                ConfigResourceLoader.intValue(properties, "activeSection.hotbar", -1),
                ConfigResourceLoader.intValue(properties, "activeSection.tools", -8),
                Math.max(1, ConfigResourceLoader.intValue(properties, "activeSelectionSlots.hotbar", 9)),
                Math.max(1, ConfigResourceLoader.intValue(properties, "activeSelectionSlots.tools", 9)),
                (float) Math.max(0.05D, ConfigResourceLoader.doubleValue(properties, "armorScanTickSeconds", 0.25D)),
                Math.max(0L, ConfigResourceLoader.longValue(properties, "notification.cooldownMillis", 1500L)),
                ConfigResourceLoader.stringValue(
                        properties,
                        "notification.messageTemplate",
                        "[Skills] %s level %d/%d required to equip %s in %s."),
                parseLocationAliases(properties),
                ConfigResourceLoader.stringValue(properties, "debug.pluginKey", "skills-equipment"));
    }

    private static boolean booleanValue(@Nonnull Properties properties, @Nonnull String key, boolean defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    @Nonnull
    private static Map<String, List<String>> parseLocationAliases(@Nonnull Properties properties) {
        Map<String, List<String>> aliasMap = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(LOCATION_ALIAS_PREFIX)) {
                continue;
            }

            String locationKey = normalizeToken(key.substring(LOCATION_ALIAS_PREFIX.length()));
            if (locationKey.isEmpty()) {
                continue;
            }

            List<String> aliases = parseCsvTokens(properties.getProperty(key));
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
    private static List<String> parseCsvTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<String> parsed = new ArrayList<>();
        for (String token : raw.split(",")) {
            String normalized = normalizeToken(token);
            if (!normalized.isEmpty()) {
                parsed.add(normalized);
            }
        }
        return parsed.isEmpty() ? List.of() : List.copyOf(parsed);
    }

    @Nonnull
    private static String normalizeToken(@Nonnull String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
