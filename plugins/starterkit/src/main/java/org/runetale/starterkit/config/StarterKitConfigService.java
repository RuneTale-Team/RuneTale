package org.runetale.starterkit.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.starterkit.domain.KitItem;
import org.runetale.starterkit.domain.StarterKitConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class StarterKitConfigService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String KIT_CONFIG_RESOURCE = "StarterKit/config/kit.json";

    @Nonnull
    private final Path configRoot;

    public StarterKitConfigService(@Nonnull Path configRoot) {
        this.configRoot = configRoot;
    }

    @Nonnull
    public StarterKitConfig load() {
        Path configPath = this.configRoot.resolve(
                StarterKitPathLayout.externalRelativeResourcePath(KIT_CONFIG_RESOURCE));
        try (InputStream input = openInput(configPath)) {
            if (input == null) {
                LOGGER.atWarning().log(
                        "[StarterKit] Config missing at path=%s and classpath fallback unavailable", configPath);
                return StarterKitConfig.defaults();
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    LOGGER.atWarning().log("[StarterKit] Config root must be object path=%s", configPath);
                    return StarterKitConfig.defaults();
                }
                StarterKitConfig config = parseConfig(parsed.getAsJsonObject());
                LOGGER.atInfo().log("[StarterKit] Loaded config items=%d enabled=%s path=%s",
                        config.items().size(), config.enabled(), configPath);
                return config;
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[StarterKit] Failed loading config path=%s", configPath);
            return StarterKitConfig.defaults();
        }
    }

    @Nullable
    private InputStream openInput(@Nonnull Path configPath) throws IOException {
        if (Files.isRegularFile(configPath)) {
            return Files.newInputStream(configPath);
        }
        return null;
    }

    @Nonnull
    private StarterKitConfig parseConfig(@Nonnull JsonObject root) {
        int version = intValue(root, "version", StarterKitConfig.DEFAULT_VERSION);
        boolean enabled = booleanValue(root, "enabled", true);
        List<KitItem> items = parseItems(root);
        return new StarterKitConfig(Math.max(1, version), enabled, List.copyOf(items));
    }

    @Nonnull
    private List<KitItem> parseItems(@Nonnull JsonObject root) {
        JsonArray itemsArray = arrayValue(root, "items");
        if (itemsArray == null) {
            return List.of();
        }

        List<KitItem> items = new ArrayList<>();
        int index = 0;
        for (JsonElement element : itemsArray) {
            if (!element.isJsonObject()) {
                index++;
                continue;
            }
            KitItem parsed = parseItem(element.getAsJsonObject(), index);
            if (parsed != null) {
                items.add(parsed);
            }
            index++;
        }
        return items;
    }

    @Nullable
    private KitItem parseItem(@Nonnull JsonObject object, int index) {
        String container = stringValue(object, "container", "");
        String itemId = stringValue(object, "itemId", "");
        int quantity = intValue(object, "quantity", 1);

        try {
            return new KitItem(container, itemId, quantity);
        } catch (IllegalArgumentException e) {
            LOGGER.atWarning().log("[StarterKit] Skipping invalid kit item index=%d: %s", index, e.getMessage());
            return null;
        }
    }

    @Nullable
    private static JsonArray arrayValue(@Nonnull JsonObject object, @Nonnull String key) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return null;
    }

    @Nonnull
    private static String stringValue(@Nonnull JsonObject object, @Nonnull String key, @Nonnull String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        String value = element.getAsString();
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback;
    }

    private static int intValue(@Nonnull JsonObject object, @Nonnull String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(@Nonnull JsonObject object, @Nonnull String key, boolean fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            String raw = element.getAsString().toLowerCase(Locale.ROOT);
            if (raw.equals("true") || raw.equals("false")) {
                return Boolean.parseBoolean(raw);
            }
            return fallback;
        }
    }
}
