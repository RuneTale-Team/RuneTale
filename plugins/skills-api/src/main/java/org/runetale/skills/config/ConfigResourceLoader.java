package org.runetale.skills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

final class ConfigResourceLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ConfigResourceLoader() {
    }

    @Nonnull
    static JsonObject loadJsonObject(@Nonnull String resourcePath) {
        return loadJsonObject(resourcePath, null);
    }

    @Nonnull
    static JsonObject loadJsonObject(@Nonnull String resourcePath, @Nullable Path externalConfigRoot) {
        JsonObject fallback = new JsonObject();
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
                JsonObject loaded = parseJsonObject(input, resourcePath, externalPath.toString());
                if (loaded != null) {
                    LOGGER.atInfo().log("[Skills] Loaded external config path=%s entries=%d", externalPath, loaded.size());
                    return loaded;
                }
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
            return fallback;
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
            return fallback;
        }

        try (InputStream input = selectedInput) {
            JsonObject loaded = parseJsonObject(input, resourcePath, "classpath");
            if (loaded != null) {
                LOGGER.atInfo().log("[Skills] Loaded config resource=%s entries=%d", resourcePath, loaded.size());
                return loaded;
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills] Failed loading config resource=%s; using defaults", resourcePath);
        }

        return fallback;
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
    static String stringValue(@Nonnull JsonObject object, @Nonnull String key, @Nonnull String defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            LOGGER.atFiner().log("[Skills] Missing string config key=%s; using default=%s", key, defaultValue);
            return defaultValue;
        }

        String trimmed;
        try {
            trimmed = element.getAsString().trim();
        } catch (RuntimeException e) {
            LOGGER.atFiner().log("[Skills] Non-string config key=%s; using default=%s", key, defaultValue);
            return defaultValue;
        }

        if (trimmed.isEmpty()) {
            LOGGER.atFiner().log("[Skills] Blank string config key=%s; using default=%s", key, defaultValue);
            return defaultValue;
        }
        return trimmed;
    }

    static int intValue(@Nonnull JsonObject object, @Nonnull String key, int defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            LOGGER.atFiner().log("[Skills] Missing integer config key=%s; using default=%d", key, defaultValue);
            return defaultValue;
        }

        try {
            return element.getAsInt();
        } catch (RuntimeException e) {
            LOGGER.atWarning().log("[Skills] Invalid integer config key=%s; using default=%d", key, defaultValue);
            return defaultValue;
        }
    }

    static long longValue(@Nonnull JsonObject object, @Nonnull String key, long defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            LOGGER.atFiner().log("[Skills] Missing long config key=%s; using default=%d", key, defaultValue);
            return defaultValue;
        }

        try {
            return element.getAsLong();
        } catch (RuntimeException e) {
            LOGGER.atWarning().log("[Skills] Invalid long config key=%s; using default=%d", key, defaultValue);
            return defaultValue;
        }
    }

    static double doubleValue(@Nonnull JsonObject object, @Nonnull String key, double defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            LOGGER.atFiner().log("[Skills] Missing decimal config key=%s; using default=%f", key, defaultValue);
            return defaultValue;
        }

        try {
            return element.getAsDouble();
        } catch (RuntimeException e) {
            LOGGER.atWarning().log("[Skills] Invalid decimal config key=%s; using default=%f", key, defaultValue);
            return defaultValue;
        }
    }

    static boolean booleanValue(@Nonnull JsonObject object, @Nonnull String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) {
            String raw = element.getAsString();
            if (raw == null) {
                return defaultValue;
            }
            String trimmed = raw.trim();
            if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(trimmed);
            }
            return defaultValue;
        }
    }

    @Nonnull
    static JsonObject objectValue(@Nonnull JsonObject object, @Nonnull String key) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }

    @Nullable
    private static JsonObject parseJsonObject(
            @Nonnull InputStream input,
            @Nonnull String resourcePath,
            @Nonnull String sourceName) {
        try {
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            LOGGER.atWarning().log("[Skills] Config resource root is not JSON object resource=%s source=%s", resourcePath, sourceName);
            return null;
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("[Skills] Failed parsing JSON config resource=%s source=%s", resourcePath, sourceName);
            return null;
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
