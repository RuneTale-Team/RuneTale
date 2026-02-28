package org.runetale.lootprotection.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.lootprotection.config.LootProtectionConfig;
import org.runetale.lootprotection.config.LootProtectionPathLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class LootProtectionConfigService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_RESOURCE = "LootProtection/config/loot-protection.json";

    @Nonnull
    private final Path configRoot;

    public LootProtectionConfigService(@Nonnull Path configRoot) {
        this.configRoot = configRoot;
    }

    @Nonnull
    public LootProtectionConfig load() {
        Path configPath = this.configRoot.resolve(LootProtectionPathLayout.externalRelativeResourcePath(CONFIG_RESOURCE));
        try (InputStream input = openInput(configPath)) {
            if (input == null) {
                LOGGER.atWarning().log("[LootProtection] Config missing at path=%s and classpath fallback unavailable", configPath);
                return LootProtectionConfig.defaults();
            }

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    LOGGER.atWarning().log("[LootProtection] Config root must be object path=%s", configPath);
                    return LootProtectionConfig.defaults();
                }

                LootProtectionConfig config = parseConfig(parsed.getAsJsonObject()).normalized();
                LOGGER.atInfo().log(
                        "[LootProtection] Loaded config enabled=%s blockBreak=%s kill=%s lockTimeout=%d path=%s",
                        config.enabled(),
                        config.protectBlockBreakDrops(),
                        config.protectKillDrops(),
                        config.ownerLock().timeoutMillis(),
                        configPath);
                return config;
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[LootProtection] Failed loading config path=%s", configPath);
            return LootProtectionConfig.defaults();
        }
    }

    @Nonnull
    private LootProtectionConfig parseConfig(@Nonnull JsonObject root) {
        LootProtectionConfig defaults = LootProtectionConfig.defaults();

        boolean enabled = booleanValue(root, "enabled", defaults.enabled());
        boolean protectBlockBreakDrops = booleanValue(root, "protectBlockBreakDrops", defaults.protectBlockBreakDrops());
        boolean protectKillDrops = booleanValue(root, "protectKillDrops", defaults.protectKillDrops());

        JsonObject blockOwnershipObject = objectValue(root, "blockOwnership");
        LootProtectionConfig.BlockOwnership defaultBlockOwnership = defaults.blockOwnership();
        LootProtectionConfig.BlockOwnership blockOwnership = new LootProtectionConfig.BlockOwnership(
                booleanValue(blockOwnershipObject, "enabled", defaultBlockOwnership.enabled()),
                longValue(blockOwnershipObject, "inactivityResetMillis", defaultBlockOwnership.inactivityResetMillis()),
                longValue(blockOwnershipObject, "notifyCooldownMillis", defaultBlockOwnership.notifyCooldownMillis()));

        JsonObject dropClaimObject = objectValue(root, "dropClaim");
        LootProtectionConfig.DropClaim defaultDropClaim = defaults.dropClaim();
        LootProtectionConfig.DropClaim dropClaim = new LootProtectionConfig.DropClaim(
                longValue(dropClaimObject, "windowMillis", defaultDropClaim.windowMillis()),
                doubleValue(dropClaimObject, "matchRadius", defaultDropClaim.matchRadius()));

        JsonObject ownerLockObject = objectValue(root, "ownerLock");
        LootProtectionConfig.OwnerLock defaultOwnerLock = defaults.ownerLock();
        LootProtectionConfig.OwnerLock ownerLock = new LootProtectionConfig.OwnerLock(
                booleanValue(ownerLockObject, "enabled", defaultOwnerLock.enabled()),
                longValue(ownerLockObject, "timeoutMillis", defaultOwnerLock.timeoutMillis()),
                longValue(ownerLockObject, "retryIntervalMillis", defaultOwnerLock.retryIntervalMillis()),
                longValue(ownerLockObject, "inventoryFullNotifyCooldownMillis",
                        defaultOwnerLock.inventoryFullNotifyCooldownMillis()));

        return new LootProtectionConfig(
                enabled,
                protectBlockBreakDrops,
                protectKillDrops,
                blockOwnership,
                dropClaim,
                ownerLock);
    }

    @Nullable
    private InputStream openInput(@Nonnull Path configPath) throws IOException {
        if (Files.isRegularFile(configPath)) {
            return Files.newInputStream(configPath);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader serviceClassLoader = LootProtectionConfigService.class.getClassLoader();
        ClassLoader classLoader = serviceClassLoader != null ? serviceClassLoader : contextClassLoader;
        if (classLoader == null) {
            return null;
        }

        InputStream selectedInput = classLoader.getResourceAsStream(CONFIG_RESOURCE);
        if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
            selectedInput = contextClassLoader.getResourceAsStream(CONFIG_RESOURCE);
        }
        return selectedInput;
    }

    @Nullable
    private static JsonObject objectValue(@Nonnull JsonObject object, @Nonnull String key) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return null;
    }

    private static boolean booleanValue(@Nullable JsonObject object, @Nonnull String key, boolean fallback) {
        if (object == null) {
            return fallback;
        }
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

    private static long longValue(@Nullable JsonObject object, @Nonnull String key, long fallback) {
        if (object == null) {
            return fallback;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double doubleValue(@Nullable JsonObject object, @Nonnull String key, double fallback) {
        if (object == null) {
            return fallback;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
