package org.runetale.skills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record CombatConfig(
        double xpPerDamage,
        @Nonnull String sourceRanged,
        @Nonnull String sourceMeleePrefix,
        @Nonnull String sourceMeleeAccurate,
        @Nonnull String sourceMeleeAggressive,
        @Nonnull String sourceMeleeDefensive,
        @Nonnull String sourceMeleeControlledAttack,
        @Nonnull String sourceMeleeControlledStrength,
        @Nonnull String sourceMeleeControlledDefence,
        @Nonnull String sourceBlockDefence,
        @Nonnull List<String> projectileCauseTokens) {

    private static final String RESOURCE_PATH = "Skills/Config/combat.json";
    private static final String LEGACY_RESOURCE_PATH = "Skills/Config/skills.json";

    @Nonnull
    public static CombatConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject combatConfig = loadExternalCombatConfig(externalConfigRoot, RESOURCE_PATH);
        if (combatConfig.entrySet().isEmpty()) {
            combatConfig = loadExternalCombatConfig(externalConfigRoot, LEGACY_RESOURCE_PATH);
        }
        if (combatConfig.entrySet().isEmpty()) {
            combatConfig = loadClasspathCombatConfig(RESOURCE_PATH);
        }
        if (combatConfig.entrySet().isEmpty()) {
            combatConfig = loadClasspathCombatConfig(LEGACY_RESOURCE_PATH);
        }

        double xpPerDamage = Math.max(0.0D, ConfigResourceLoader.doubleValue(combatConfig, "xpPerDamage", 4.0D));
        JsonObject sourceConfig = ConfigResourceLoader.objectValue(combatConfig, "source");
        JsonObject meleeConfig = ConfigResourceLoader.objectValue(sourceConfig, "melee");
        JsonObject controlledConfig = ConfigResourceLoader.objectValue(meleeConfig, "controlled");
        JsonObject blockConfig = ConfigResourceLoader.objectValue(sourceConfig, "block");

        String sourceRanged = ConfigResourceLoader.stringValue(sourceConfig, "ranged", "combat:ranged");
        String sourceMeleePrefix = ConfigResourceLoader.stringValue(meleeConfig, "prefix", "combat:melee:");

        return new CombatConfig(
                xpPerDamage,
                sourceRanged,
                sourceMeleePrefix,
                ConfigResourceLoader.stringValue(meleeConfig, "accurate", "accurate"),
                ConfigResourceLoader.stringValue(meleeConfig, "aggressive", "aggressive"),
                ConfigResourceLoader.stringValue(meleeConfig, "defensive", "defensive"),
                ConfigResourceLoader.stringValue(controlledConfig, "attack", "controlled:attack"),
                ConfigResourceLoader.stringValue(controlledConfig, "strength", "controlled:strength"),
                ConfigResourceLoader.stringValue(controlledConfig, "defence", "controlled:defence"),
                ConfigResourceLoader.stringValue(blockConfig, "defence", "combat:block:defence"),
                parseTokenList(combatConfig, "projectileCauseTokens", List.of("projectile")));
    }

    @Nonnull
    private static JsonObject loadExternalCombatConfig(
            @Nonnull Path externalConfigRoot,
            @Nonnull String resourcePath) {
        Path externalPath = externalConfigRoot.resolve(
                SkillsPathLayout.externalRelativeResourcePath(resourcePath).replace('/', File.separatorChar));
        if (!Files.isRegularFile(externalPath)) {
            return new JsonObject();
        }
        return selectCombatObject(ConfigResourceLoader.loadJsonObject(resourcePath, externalConfigRoot));
    }

    @Nonnull
    private static JsonObject loadClasspathCombatConfig(@Nonnull String resourcePath) {
        return selectCombatObject(ConfigResourceLoader.loadJsonObject(resourcePath));
    }

    @Nonnull
    private static JsonObject selectCombatObject(@Nonnull JsonObject root) {
        JsonObject nestedCombat = ConfigResourceLoader.objectValue(root, "combat");
        if (!nestedCombat.entrySet().isEmpty()) {
            return nestedCombat;
        }
        return root;
    }

    private static List<String> parseTokenList(
            @Nonnull JsonObject object,
            @Nonnull String key,
            @Nonnull List<String> defaultValues) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return List.copyOf(defaultValues);
        }

        List<String> parsed = new ArrayList<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement tokenElement : array) {
                if (tokenElement == null || tokenElement.isJsonNull()) {
                    continue;
                }
                String normalized = tokenElement.getAsString().trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        } else {
            String raw = element.getAsString();
            for (String token : raw.split(",")) {
                String normalized = token.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        }

        return parsed.isEmpty() ? List.copyOf(defaultValues) : List.copyOf(parsed);
    }
}
