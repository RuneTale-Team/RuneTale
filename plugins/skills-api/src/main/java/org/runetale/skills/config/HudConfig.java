package org.runetale.skills.config;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public record HudConfig(
        long toastDurationMillis,
        long toastFadeDurationMillis,
        @Nonnull String rootBackgroundFaded,
        @Nonnull String innerBackgroundFaded,
        float toastExpiryTickSeconds) {

    private static final String RESOURCE_PATH = "Skills/Config/skills.json";

    @Nonnull
    public static HudConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject hudConfig = ConfigResourceLoader.objectValue(root, "hud");
        JsonObject toastConfig = ConfigResourceLoader.objectValue(hudConfig, "toast");
        JsonObject fadeConfig = ConfigResourceLoader.objectValue(toastConfig, "fade");

        long toastDurationMillis = Math.max(1L, ConfigResourceLoader.longValue(toastConfig, "durationMillis", 1400L));
        long toastFadeDurationMillis = Math.max(1L, ConfigResourceLoader.longValue(toastConfig, "fadeDurationMillis", 180L));
        String rootBackgroundFaded = ConfigResourceLoader.stringValue(fadeConfig, "rootBackground", "#1b314b");
        String innerBackgroundFaded = ConfigResourceLoader.stringValue(fadeConfig, "innerBackground", "#0a1421");
        float toastExpiryTickSeconds = (float) Math.max(0.01D,
                ConfigResourceLoader.doubleValue(toastConfig, "expiryTickSeconds", 0.1D));

        return new HudConfig(
                toastDurationMillis,
                toastFadeDurationMillis,
                rootBackgroundFaded,
                innerBackgroundFaded,
                toastExpiryTickSeconds);
    }
}
