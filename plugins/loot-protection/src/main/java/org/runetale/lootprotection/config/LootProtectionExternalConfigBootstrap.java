package org.runetale.lootprotection.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LootProtectionExternalConfigBootstrap {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_RESOURCE = "LootProtection/config/loot-protection.json";

    private LootProtectionExternalConfigBootstrap() {
    }

    public static void seedMissingDefaults(@Nonnull LootProtectionPathLayout pathLayout) {
        Path configRoot = pathLayout.pluginConfigRoot();
        LOGGER.atInfo().log("[LootProtection] Seeding external config defaults configRoot=%s", configRoot);

        try {
            Files.createDirectories(configRoot);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[LootProtection] Failed to initialize config directory=%s", configRoot);
            return;
        }

        if (copyClasspathResourceIfMissing(pathLayout, CONFIG_RESOURCE)) {
            LOGGER.atInfo().log("[LootProtection] External config bootstrap complete copied=1 root=%s", configRoot);
            return;
        }

        LOGGER.atInfo().log("[LootProtection] External config bootstrap complete copied=0 root=%s", configRoot);
    }

    private static boolean copyClasspathResourceIfMissing(
            @Nonnull LootProtectionPathLayout pathLayout,
            @Nonnull String resourcePath) {
        Path outputPath = pathLayout.resolveConfigResourcePath(resourcePath);
        if (Files.exists(outputPath)) {
            LOGGER.atFine().log("[LootProtection] Bootstrap skipped existing config file: %s", outputPath);
            return false;
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[LootProtection] Failed to create config directory for %s", outputPath);
            return false;
        }

        try (InputStream input = openClasspathResource(resourcePath)) {
            if (input == null) {
                LOGGER.atWarning().log("[LootProtection] Missing bundled bootstrap resource=%s", resourcePath);
                return false;
            }

            Files.copy(input, outputPath);
            LOGGER.atInfo().log("[LootProtection] Seeded default config file: %s", outputPath);
            return true;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[LootProtection] Failed writing bootstrap resource=%s path=%s",
                    resourcePath,
                    outputPath);
            return false;
        }
    }

    @Nullable
    private static InputStream openClasspathResource(@Nonnull String resourcePath) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader selectedClassLoader = LootProtectionExternalConfigBootstrap.class.getClassLoader();
        if (selectedClassLoader == null) {
            selectedClassLoader = contextClassLoader;
        }
        if (selectedClassLoader == null) {
            return null;
        }

        InputStream selectedInput = selectedClassLoader.getResourceAsStream(resourcePath);
        if (selectedInput == null && contextClassLoader != null && contextClassLoader != selectedClassLoader) {
            selectedInput = contextClassLoader.getResourceAsStream(resourcePath);
        }
        return selectedInput;
    }
}
