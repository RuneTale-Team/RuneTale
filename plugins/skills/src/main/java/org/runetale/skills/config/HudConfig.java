package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Properties;

public record HudConfig(
        long toastDurationMillis,
        long toastFadeDurationMillis,
        @Nonnull String rootBackgroundFaded,
        @Nonnull String innerBackgroundFaded,
        float toastExpiryTickSeconds) {

    private static final String RESOURCE_PATH = "Skills/Config/hud.properties";

    @Nonnull
    public static HudConfig load(@Nonnull Path externalConfigRoot) {
        Properties properties = ConfigResourceLoader.loadProperties(RESOURCE_PATH, externalConfigRoot);

        long toastDurationMillis = Math.max(1L, ConfigResourceLoader.longValue(properties, "toast.durationMillis", 1400L));
        long toastFadeDurationMillis = Math.max(1L, ConfigResourceLoader.longValue(properties, "toast.fadeDurationMillis", 180L));
        String rootBackgroundFaded = ConfigResourceLoader.stringValue(properties, "toast.fade.rootBackground", "#1b314b");
        String innerBackgroundFaded = ConfigResourceLoader.stringValue(properties, "toast.fade.innerBackground", "#0a1421");
        float toastExpiryTickSeconds = (float) Math.max(0.01D,
                ConfigResourceLoader.doubleValue(properties, "toast.expiryTickSeconds", 0.1D));

        return new HudConfig(
                toastDurationMillis,
                toastFadeDurationMillis,
                rootBackgroundFaded,
                innerBackgroundFaded,
                toastExpiryTickSeconds);
    }
}
