package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public record CombatConfig(
        double xpPerDamage,
        @Nonnull String sourceRanged,
        @Nonnull String sourceMeleePrefix,
        @Nonnull String sourceMeleeAccurate,
        @Nonnull String sourceMeleeAggressive,
        @Nonnull String sourceMeleeDefensive,
        @Nonnull String sourceMeleeControlledAttack,
        @Nonnull String sourceMeleeControlledStrength,
        @Nonnull String sourceMeleeControlledDefense,
        @Nonnull String sourceBlockDefense,
        @Nonnull List<String> projectileCauseTokens) {

    private static final String RESOURCE_PATH = "Skills/Config/combat.properties";

    @Nonnull
    public static CombatConfig load(@Nonnull Path externalConfigRoot) {
        Properties properties = ConfigResourceLoader.loadProperties(RESOURCE_PATH, externalConfigRoot);

        double xpPerDamage = Math.max(0.0D, ConfigResourceLoader.doubleValue(properties, "xpPerDamage", 4.0D));
        String sourceRanged = ConfigResourceLoader.stringValue(properties, "source.ranged", "combat:ranged");
        String sourceMeleePrefix = ConfigResourceLoader.stringValue(properties, "source.melee.prefix", "combat:melee:");

        return new CombatConfig(
                xpPerDamage,
                sourceRanged,
                sourceMeleePrefix,
                ConfigResourceLoader.stringValue(properties, "source.melee.accurate", "accurate"),
                ConfigResourceLoader.stringValue(properties, "source.melee.aggressive", "aggressive"),
                ConfigResourceLoader.stringValue(properties, "source.melee.defensive", "defensive"),
                ConfigResourceLoader.stringValue(properties, "source.melee.controlled.attack", "controlled:attack"),
                ConfigResourceLoader.stringValue(properties, "source.melee.controlled.strength", "controlled:strength"),
                ConfigResourceLoader.stringValue(properties, "source.melee.controlled.defense", "controlled:defense"),
                ConfigResourceLoader.stringValue(properties, "source.block.defense", "combat:block:defense"),
                parseCsvLowercase(properties, "projectileCauseTokens", List.of("projectile")));
    }

    private static List<String> parseCsvLowercase(
            @Nonnull Properties properties,
            @Nonnull String key,
            @Nonnull List<String> defaultValues) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return List.copyOf(defaultValues);
        }

        List<String> parsed = new ArrayList<>();
        for (String token : raw.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                parsed.add(normalized);
            }
        }

        return parsed.isEmpty() ? List.copyOf(defaultValues) : List.copyOf(parsed);
    }
}
