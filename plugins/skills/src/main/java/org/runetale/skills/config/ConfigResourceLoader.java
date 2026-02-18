package org.runetale.skills.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class ConfigResourceLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ConfigResourceLoader() {
    }

    @Nonnull
    static Properties loadProperties(@Nonnull String resourcePath) {
        return loadProperties(resourcePath, null);
    }

    @Nonnull
    static Properties loadProperties(@Nonnull String resourcePath, @Nullable Path externalConfigRoot) {
        Properties properties = new Properties();
        LOGGER.atFine().log("[Skills] Loading config resource=%s externalRoot=%s",
                resourcePath,
                externalConfigRoot == null ? "<none>" : externalConfigRoot);

        Path externalPath = resolveExternalPath(resourcePath, externalConfigRoot);
        if (externalPath != null) {
            LOGGER.atFine().log("[Skills] Resolved external config candidate resource=%s path=%s exists=%s",
                    resourcePath,
                    externalPath,
                    Files.isRegularFile(externalPath));
        }
        if (externalPath != null && Files.isRegularFile(externalPath)) {
            try (InputStream input = Files.newInputStream(externalPath)) {
                properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                LOGGER.atInfo().log("[Skills] Loaded external config path=%s entries=%d", externalPath, properties.size());
                return properties;
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("[Skills] Failed loading external config path=%s; falling back", externalPath);
            }
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader configClassLoader = ConfigResourceLoader.class.getClassLoader();
        ClassLoader selectedClassLoader = configClassLoader != null ? configClassLoader : contextClassLoader;
        LOGGER.atFine().log("[Skills] Resolving classpath config resource=%s selectedCL=%s contextCL=%s configCL=%s",
                resourcePath,
                describeClassLoader(selectedClassLoader),
                describeClassLoader(contextClassLoader),
                describeClassLoader(configClassLoader));
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
            LOGGER.atInfo().log("[Skills][Diag] Config resource miss via selectedCL=%s resource=%s contextVisible=%s configVisible=%s",
                    describeClassLoader(selectedClassLoader),
                    resourcePath,
                    probeResourceVisible(contextClassLoader, resourcePath),
                    probeResourceVisible(configClassLoader, resourcePath));
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

    @Nullable
    private static Path resolveExternalPath(@Nonnull String resourcePath, @Nullable Path externalConfigRoot) {
        if (externalConfigRoot == null) {
            return null;
        }

        String relative = SkillsPathLayout.externalRelativeResourcePath(resourcePath);
        return externalConfigRoot.resolve(relative.replace('/', File.separatorChar));
    }

    @Nonnull
    static String stringValue(@Nonnull Properties properties, @Nonnull String key, @Nonnull String defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            LOGGER.atFiner().log("[Skills] Missing string config key=%s; using default=%s", key, defaultValue);
            return defaultValue;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            LOGGER.atFiner().log("[Skills] Blank string config key=%s; using default=%s", key, defaultValue);
            return defaultValue;
        }
        return trimmed;
    }

    static int intValue(@Nonnull Properties properties, @Nonnull String key, int defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            LOGGER.atFiner().log("[Skills] Missing integer config key=%s; using default=%d", key, defaultValue);
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
            LOGGER.atFiner().log("[Skills] Missing long config key=%s; using default=%d", key, defaultValue);
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
            LOGGER.atFiner().log("[Skills] Missing decimal config key=%s; using default=%f", key, defaultValue);
            return defaultValue;
        }

        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            LOGGER.atWarning().log("[Skills] Invalid decimal config key=%s value=%s; using default=%f", key, raw, defaultValue);
            return defaultValue;
        }
    }

    @Nonnull
    private static String describeClassLoader(@Nullable ClassLoader classLoader) {
        if (classLoader == null) {
            return "<null>";
        }
        return classLoader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(classLoader));
    }

    private static boolean probeResourceVisible(@Nullable ClassLoader classLoader, @Nonnull String resourcePath) {
        if (classLoader == null) {
            return false;
        }
        return classLoader.getResource(resourcePath) != null;
    }
}
