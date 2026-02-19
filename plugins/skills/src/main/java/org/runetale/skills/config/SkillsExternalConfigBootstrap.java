package org.runetale.skills.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkillsExternalConfigBootstrap {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private SkillsExternalConfigBootstrap() {
    }

    public static void seedMissingDefaults(@Nonnull SkillsPathLayout pathLayout) {
        Path configRoot = pathLayout.pluginConfigRoot();
        Path runtimeRoot = pathLayout.pluginRuntimeRoot();
        LOGGER.atInfo().log("[Skills] Seeding external config defaults configRoot=%s runtimeRoot=%s", configRoot, runtimeRoot);

        try {
            Files.createDirectories(configRoot);
            Files.createDirectories(runtimeRoot);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills] Failed to initialize runtime directories config=%s runtime=%s",
                    configRoot,
                    runtimeRoot);
            return;
        }

        int copied = 0;
        for (String resourcePath : defaultResourcePaths()) {
            if (copyClasspathResourceIfMissing(pathLayout, resourcePath)) {
                copied++;
            }
        }

        LOGGER.atInfo().log("[Skills] External config bootstrap complete copied=%d root=%s", copied, configRoot);
    }

    @Nonnull
    private static List<String> defaultResourcePaths() {
        List<String> resources = new ArrayList<>();
        resources.add("Skills/Config/xp.properties");
        resources.add("Skills/Config/combat.properties");
        resources.add("Skills/Config/hud.properties");
        LOGGER.atFine().log("[Skills] Bootstrap resource manifest contains %d resource(s)", resources.size());
        return resources;
    }

    private static boolean copyClasspathResourceIfMissing(
            @Nonnull SkillsPathLayout pathLayout,
            @Nonnull String resourcePath) {
        Path outputPath = pathLayout.resolveConfigResourcePath(resourcePath);
        if (Files.exists(outputPath)) {
            LOGGER.atFine().log("[Skills] Bootstrap skipped existing config file: %s", outputPath);
            return false;
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills] Failed to create config directory for %s", outputPath);
            return false;
        }

        try (InputStream input = openClasspathResource(resourcePath)) {
            if (input == null) {
                LOGGER.atWarning().log("[Skills] Missing bundled bootstrap resource=%s", resourcePath);
                return false;
            }

            Files.copy(input, outputPath);
            LOGGER.atInfo().log("[Skills] Seeded default config file: %s", outputPath);
            return true;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills] Failed writing bootstrap resource=%s path=%s", resourcePath, outputPath);
            return false;
        }
    }

    @Nullable
    private static InputStream openClasspathResource(@Nonnull String resourcePath) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader selectedClassLoader = SkillsExternalConfigBootstrap.class.getClassLoader();
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
