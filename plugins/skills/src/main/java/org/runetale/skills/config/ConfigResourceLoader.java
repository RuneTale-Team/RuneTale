package org.runetale.skills.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class ConfigResourceLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ConfigResourceLoader() {
    }

    @Nonnull
    static Properties loadProperties(@Nonnull String resourcePath) {
        Properties properties = new Properties();

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader configClassLoader = ConfigResourceLoader.class.getClassLoader();
        ClassLoader selectedClassLoader = configClassLoader != null ? configClassLoader : contextClassLoader;
        if (selectedClassLoader == null) {
            LOGGER.atWarning().log("[Skills] No classloader available for config resource=%s", resourcePath);
            return properties;
        }

        InputStream selectedInput = selectedClassLoader.getResourceAsStream(resourcePath);
        if (selectedInput == null && contextClassLoader != null && contextClassLoader != selectedClassLoader) {
            selectedClassLoader = contextClassLoader;
            selectedInput = selectedClassLoader.getResourceAsStream(resourcePath);
        }

        if (selectedInput == null) {
            LOGGER.atWarning().log("[Skills] Missing config resource=%s; using defaults", resourcePath);
            return properties;
        }

        try (InputStream input = selectedInput) {
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            LOGGER.atInfo().log("[Skills] Loaded config resource=%s entries=%d", resourcePath, properties.size());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills] Failed loading config resource=%s; using defaults", resourcePath);
        }

        return properties;
    }

    @Nonnull
    static String stringValue(@Nonnull Properties properties, @Nonnull String key, @Nonnull String defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return defaultValue;
        }

        String trimmed = raw.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    static int intValue(@Nonnull Properties properties, @Nonnull String key, int defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            LOGGER.atWarning().log("[Skills] Invalid integer config key=%s value=%s; using default=%d", key, raw, defaultValue);
            return defaultValue;
        }
    }

    static long longValue(@Nonnull Properties properties, @Nonnull String key, long defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            LOGGER.atWarning().log("[Skills] Invalid long config key=%s value=%s; using default=%d", key, raw, defaultValue);
            return defaultValue;
        }
    }

    static double doubleValue(@Nonnull Properties properties, @Nonnull String key, double defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            LOGGER.atWarning().log("[Skills] Invalid decimal config key=%s value=%s; using default=%f", key, raw, defaultValue);
            return defaultValue;
        }
    }
}
