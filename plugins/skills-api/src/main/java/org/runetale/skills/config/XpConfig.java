package org.runetale.skills.config;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public record XpConfig(
        int maxLevel,
        double levelTermMultiplier,
        double growthScale,
        double growthBase,
        double growthDivisor,
        int pointsDivisor,
        @Nonnull XpRoundingMode roundingMode) {

    private static final String RESOURCE_PATH = "Skills/Config/skills.json";

    @Nonnull
    public static XpConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject xpConfig = ConfigResourceLoader.objectValue(root, "xp");

        int maxLevel = Math.max(2, ConfigResourceLoader.intValue(xpConfig, "maxLevel", 99));
        double levelTermMultiplier = Math.max(0.0D, ConfigResourceLoader.doubleValue(xpConfig, "levelTermMultiplier", 1.0D));
        double growthScale = Math.max(0.0D, ConfigResourceLoader.doubleValue(xpConfig, "growthScale", 300.0D));
        double growthBase = Math.max(1.000001D, ConfigResourceLoader.doubleValue(xpConfig, "growthBase", 2.0D));
        double growthDivisor = Math.max(0.000001D, ConfigResourceLoader.doubleValue(xpConfig, "growthDivisor", 7.0D));
        int pointsDivisor = Math.max(1, ConfigResourceLoader.intValue(xpConfig, "pointsDivisor", 4));
        XpRoundingMode roundingMode = XpRoundingMode.fromConfig(
                ConfigResourceLoader.stringValue(xpConfig, "roundingMode", XpRoundingMode.NEAREST.name()));

        return new XpConfig(
                maxLevel,
                levelTermMultiplier,
                growthScale,
                growthBase,
                growthDivisor,
                pointsDivisor,
                roundingMode);
    }
}
