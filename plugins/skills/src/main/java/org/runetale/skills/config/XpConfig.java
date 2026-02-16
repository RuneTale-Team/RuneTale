package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.util.Properties;

public record XpConfig(
        int maxLevel,
        double levelTermMultiplier,
        double growthScale,
        double growthBase,
        double growthDivisor,
        int pointsDivisor,
        @Nonnull XpRoundingMode roundingMode) {

    private static final String RESOURCE_PATH = "Skills/Config/xp.properties";

    @Nonnull
    public static XpConfig load() {
        Properties properties = ConfigResourceLoader.loadProperties(RESOURCE_PATH);

        int maxLevel = Math.max(2, ConfigResourceLoader.intValue(properties, "maxLevel", 99));
        double levelTermMultiplier = Math.max(0.0D, ConfigResourceLoader.doubleValue(properties, "levelTermMultiplier", 1.0D));
        double growthScale = Math.max(0.0D, ConfigResourceLoader.doubleValue(properties, "growthScale", 300.0D));
        double growthBase = Math.max(1.000001D, ConfigResourceLoader.doubleValue(properties, "growthBase", 2.0D));
        double growthDivisor = Math.max(0.000001D, ConfigResourceLoader.doubleValue(properties, "growthDivisor", 7.0D));
        int pointsDivisor = Math.max(1, ConfigResourceLoader.intValue(properties, "pointsDivisor", 4));
        XpRoundingMode roundingMode = XpRoundingMode.fromConfig(
                ConfigResourceLoader.stringValue(properties, "roundingMode", XpRoundingMode.NEAREST.name()));

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
