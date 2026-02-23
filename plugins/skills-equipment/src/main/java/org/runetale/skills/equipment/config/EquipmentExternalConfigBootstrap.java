package org.runetale.skills.equipment.config;

import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.skills.config.SkillsPathLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EquipmentExternalConfigBootstrap {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String EQUIPMENT_CONFIG_RESOURCE = "Skills/Config/equipment.json";

    private EquipmentExternalConfigBootstrap() {
    }

    public static void seedMissingDefaults(@Nonnull SkillsPathLayout pathLayout) {
        Path configRoot = pathLayout.pluginConfigRoot();
        LOGGER.atInfo().log("[Skills Equipment] Seeding external config defaults configRoot=%s", configRoot);

        try {
            Files.createDirectories(configRoot);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills Equipment] Failed to initialize config directory=%s", configRoot);
            return;
        }

        if (copyClasspathResourceIfMissing(pathLayout, EQUIPMENT_CONFIG_RESOURCE)) {
            LOGGER.atInfo().log("[Skills Equipment] External config bootstrap complete copied=1 root=%s", configRoot);
            return;
        }

        LOGGER.atInfo().log("[Skills Equipment] External config bootstrap complete copied=0 root=%s", configRoot);
    }

    private static boolean copyClasspathResourceIfMissing(
            @Nonnull SkillsPathLayout pathLayout,
            @Nonnull String resourcePath) {
        Path outputPath = pathLayout.resolveConfigResourcePath(resourcePath);
        if (Files.exists(outputPath)) {
            LOGGER.atFine().log("[Skills Equipment] Bootstrap skipped existing config file: %s", outputPath);
            return false;
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills Equipment] Failed to create config directory for %s", outputPath);
            return false;
        }

        try (InputStream input = openClasspathResource(resourcePath)) {
            if (input == null) {
                LOGGER.atWarning().log("[Skills Equipment] Missing bundled bootstrap resource=%s", resourcePath);
                return false;
            }

            Files.copy(input, outputPath);
            LOGGER.atInfo().log("[Skills Equipment] Seeded default config file: %s", outputPath);
            return true;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills Equipment] Failed writing bootstrap resource=%s path=%s", resourcePath,
                    outputPath);
            return false;
        }
    }

    @Nullable
    private static InputStream openClasspathResource(@Nonnull String resourcePath) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader selectedClassLoader = EquipmentExternalConfigBootstrap.class.getClassLoader();
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
