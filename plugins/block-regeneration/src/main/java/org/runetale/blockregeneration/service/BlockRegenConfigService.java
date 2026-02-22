package org.runetale.blockregeneration.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.blockregeneration.config.BlockRegenPathLayout;
import org.runetale.blockregeneration.domain.BlockRegenConfig;
import org.runetale.blockregeneration.domain.BlockRegenDefinition;
import org.runetale.blockregeneration.domain.GatheringTrigger;
import org.runetale.blockregeneration.domain.RespawnDelay;

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

public class BlockRegenConfigService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String BLOCK_REGEN_CONFIG_RESOURCE = "BlockRegen/config/blocks.json";

    @Nonnull
    private final Path configRoot;

    public BlockRegenConfigService(@Nonnull Path configRoot) {
        this.configRoot = configRoot;
    }

    @Nonnull
    public BlockRegenConfig load() {
        Path configPath = this.configRoot.resolve(BlockRegenPathLayout.externalRelativeResourcePath(BLOCK_REGEN_CONFIG_RESOURCE));
        try (InputStream input = openInput(configPath)) {
            if (input == null) {
                LOGGER.atWarning().log("[BlockRegen] Config missing at path=%s and classpath fallback unavailable", configPath);
                return BlockRegenConfig.defaults();
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    LOGGER.atWarning().log("[BlockRegen] Config root must be object path=%s", configPath);
                    return BlockRegenConfig.defaults();
                }
                BlockRegenConfig config = parseConfig(parsed.getAsJsonObject());
                LOGGER.atInfo().log("[BlockRegen] Loaded config definitions=%d enabled=%s path=%s",
                        config.definitions().size(),
                        config.enabled(),
                        configPath);
                return config;
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[BlockRegen] Failed loading config path=%s", configPath);
            return BlockRegenConfig.defaults();
        }
    }

    @Nullable
    private InputStream openInput(@Nonnull Path configPath) throws IOException {
        if (Files.isRegularFile(configPath)) {
            return Files.newInputStream(configPath);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader serviceClassLoader = BlockRegenConfigService.class.getClassLoader();
        ClassLoader classLoader = serviceClassLoader != null ? serviceClassLoader : contextClassLoader;
        if (classLoader == null) {
            return null;
        }

        InputStream selectedInput = classLoader.getResourceAsStream(BLOCK_REGEN_CONFIG_RESOURCE);
        if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
            selectedInput = contextClassLoader.getResourceAsStream(BLOCK_REGEN_CONFIG_RESOURCE);
        }
        return selectedInput;
    }

    @Nonnull
    private BlockRegenConfig parseConfig(@Nonnull JsonObject root) {
        int version = intValue(root, List.of("version"), BlockRegenConfig.DEFAULT_VERSION);
        boolean enabled = booleanValue(root, List.of("enabled"), true);
        long tickMillis = longValue(root, List.of("respawnTickMillis"), BlockRegenConfig.DEFAULT_RESPAWN_TICK_MILLIS);
        long notifyCooldown = longValue(root, List.of("notifyCooldownMillis"), BlockRegenConfig.DEFAULT_NOTIFY_COOLDOWN_MILLIS);

        List<BlockRegenDefinition> definitions = parseDefinitions(root);
        return new BlockRegenConfig(
                Math.max(1, version),
                enabled,
                Math.max(1L, tickMillis),
                Math.max(100L, notifyCooldown),
                definitions);
    }

    @Nonnull
    private List<BlockRegenDefinition> parseDefinitions(@Nonnull JsonObject root) {
        JsonArray definitionsArray = arrayValue(root, List.of("definitions"));
        if (definitionsArray == null) {
            return List.of();
        }

        List<BlockRegenDefinition> definitions = new ArrayList<>();
        int index = 0;
        for (JsonElement element : definitionsArray) {
            if (!element.isJsonObject()) {
                index++;
                continue;
            }
            BlockRegenDefinition parsed = parseDefinition(element.getAsJsonObject(), index);
            if (parsed != null) {
                definitions.add(parsed);
            }
            index++;
        }
        return List.copyOf(definitions);
    }

    @Nullable
    private BlockRegenDefinition parseDefinition(@Nonnull JsonObject object, int index) {
        String fallbackId = "definition_" + index;
        String id = stringValue(object, List.of("id", "ID"), fallbackId);
        boolean enabled = booleanValue(object, List.of("enabled", "Enabled"), true);
        String blockId = stringValue(object, List.of("blockId", "Block_ID", "BlockId"), "");
        String interactedBlockId = stringValue(object,
                List.of("interactedBlockId", "Interacted block", "interacted_block", "interactedBlock"),
                "");
        if (blockId.isBlank() || interactedBlockId.isBlank()) {
            LOGGER.atWarning().log("[BlockRegen] Skipping definition id=%s due to missing block id or interacted block", id);
            return null;
        }

        JsonObject gatheringObject = objectValue(object, List.of("gathering", "Gathering"));
        GatheringTrigger gatheringTrigger = parseGathering(gatheringObject);

        JsonObject respawnObject = objectValue(object, List.of("respawn", "Respawn"));
        RespawnDelay respawnDelay = parseRespawn(respawnObject);

        return new BlockRegenDefinition(
                id,
                enabled,
                blockId,
                interactedBlockId,
                gatheringTrigger,
                respawnDelay);
    }

    @Nonnull
    private GatheringTrigger parseGathering(@Nullable JsonObject object) {
        if (object == null) {
            return new GatheringTrigger(GatheringTrigger.Type.SPECIFIC, 1, 1, 1);
        }

        GatheringTrigger.Type type = GatheringTrigger.Type.parse(
                stringValue(object, List.of("type", "Type"), "Specific"),
                GatheringTrigger.Type.SPECIFIC);
        int amount = Math.max(1, intValue(object, List.of("amount", "Amount"), 1));
        int amountMin = Math.max(1, intValue(object, List.of("amountMin", "Amount_min", "AmountMin"), amount));
        int amountMax = Math.max(amountMin,
                intValue(object, List.of("amountMax", "Amount_max", "AmountMax"), Math.max(amount, amountMin)));
        return new GatheringTrigger(type, amount, amountMin, amountMax);
    }

    @Nonnull
    private RespawnDelay parseRespawn(@Nullable JsonObject object) {
        if (object == null) {
            return new RespawnDelay(RespawnDelay.Type.SET, 5000L, 5000L, 5000L);
        }

        RespawnDelay.Type type = RespawnDelay.Type.parse(
                stringValue(object, List.of("type", "Type"), "Set"),
                RespawnDelay.Type.SET);
        long millis = Math.max(1L,
                longValue(object, List.of("millis", "Millis"),
                        secondsToMillis(longValue(object, List.of("seconds", "Seconds"), 5L))));
        long millisMin = Math.max(1L,
                longValue(object, List.of("millisMin", "Millis_Min", "MillisMin"),
                        secondsToMillis(longValue(object, List.of("secondsMin", "Seconds_Min"), millis / 1000L))));
        long millisMax = Math.max(millisMin,
                longValue(object, List.of("millisMax", "Millis_Max", "MillisMax"),
                        secondsToMillis(longValue(object, List.of("secondsMax", "Seconds_Max"), Math.max(millisMin / 1000L, millis / 1000L)))));
        return new RespawnDelay(type, millis, millisMin, millisMax);
    }

    private static long secondsToMillis(long seconds) {
        return Math.max(1L, seconds) * 1000L;
    }

    @Nullable
    private static JsonArray arrayValue(@Nonnull JsonObject object, @Nonnull List<String> keys) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray();
            }
        }
        return null;
    }

    @Nullable
    private static JsonObject objectValue(@Nonnull JsonObject object, @Nonnull List<String> keys) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            if (element.isJsonArray() && element.getAsJsonArray().size() > 0) {
                JsonElement first = element.getAsJsonArray().get(0);
                if (first != null && first.isJsonObject()) {
                    return first.getAsJsonObject();
                }
            }
        }
        return null;
    }

    @Nonnull
    private static String stringValue(@Nonnull JsonObject object, @Nonnull List<String> keys, @Nonnull String fallback) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return fallback;
    }

    private static int intValue(@Nonnull JsonObject object, @Nonnull List<String> keys, int fallback) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
                continue;
            }
            try {
                return element.getAsInt();
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static long longValue(@Nonnull JsonObject object, @Nonnull List<String> keys, long fallback) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
                continue;
            }
            try {
                return element.getAsLong();
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean booleanValue(@Nonnull JsonObject object, @Nonnull List<String> keys, boolean fallback) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
                continue;
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
        return fallback;
    }
}
