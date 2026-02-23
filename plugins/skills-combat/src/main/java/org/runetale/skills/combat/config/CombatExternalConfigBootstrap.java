package org.runetale.skills.combat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.skills.config.SkillsPathLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CombatExternalConfigBootstrap {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String COMBAT_CONFIG_RESOURCE = "Skills/Config/combat.json";
    private static final String LEGACY_SKILLS_CONFIG_RESOURCE = "Skills/Config/skills.json";

    private CombatExternalConfigBootstrap() {
    }

    public static void seedMissingDefaults(@Nonnull SkillsPathLayout pathLayout) {
        Path configRoot = pathLayout.pluginConfigRoot();
        LOGGER.atInfo().log("[Skills Combat] Seeding external config defaults configRoot=%s", configRoot);

        try {
            Files.createDirectories(configRoot);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills Combat] Failed to initialize config directory=%s", configRoot);
            return;
        }

        if (writeCombatConfigIfMissing(pathLayout)) {
            LOGGER.atInfo().log("[Skills Combat] External config bootstrap complete copied=1 root=%s", configRoot);
            return;
        }

        LOGGER.atInfo().log("[Skills Combat] External config bootstrap complete copied=0 root=%s", configRoot);
    }

    private static boolean writeCombatConfigIfMissing(@Nonnull SkillsPathLayout pathLayout) {
        Path outputPath = pathLayout.resolveConfigResourcePath(COMBAT_CONFIG_RESOURCE);
        if (Files.exists(outputPath)) {
            LOGGER.atFine().log("[Skills Combat] Bootstrap skipped existing config file: %s", outputPath);
            return false;
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills Combat] Failed to create config directory for %s", outputPath);
            return false;
        }

        if (migrateLegacyConfigIfPresent(pathLayout, outputPath)) {
            return true;
        }

        try (InputStream input = openClasspathResource(COMBAT_CONFIG_RESOURCE)) {
            if (input == null) {
                LOGGER.atWarning().log("[Skills Combat] Missing bundled bootstrap resource=%s", COMBAT_CONFIG_RESOURCE);
                return false;
            }

            Files.copy(input, outputPath);
            LOGGER.atInfo().log("[Skills Combat] Seeded default config file: %s", outputPath);
            return true;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[Skills Combat] Failed writing bootstrap resource=%s path=%s", COMBAT_CONFIG_RESOURCE, outputPath);
            return false;
        }
    }

    private static boolean migrateLegacyConfigIfPresent(
            @Nonnull SkillsPathLayout pathLayout,
            @Nonnull Path outputPath) {
        Path legacyPath = pathLayout.resolveConfigResourcePath(LEGACY_SKILLS_CONFIG_RESOURCE);
        if (!Files.isRegularFile(legacyPath)) {
            return false;
        }

        try (InputStream input = Files.newInputStream(legacyPath);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return false;
            }

            JsonObject root = parsed.getAsJsonObject();
            JsonElement combatElement = root.get("combat");
            if (combatElement == null || !combatElement.isJsonObject()) {
                return false;
            }

            JsonObject combatObject = combatElement.getAsJsonObject();
            if (combatObject.entrySet().isEmpty()) {
                return false;
            }

            Files.writeString(outputPath, GSON.toJson(combatObject), StandardCharsets.UTF_8);
            LOGGER.atInfo().log("[Skills Combat] Migrated legacy combat config from %s to %s", legacyPath, outputPath);
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("[Skills Combat] Failed migrating legacy combat config from=%s to=%s", legacyPath, outputPath);
            return false;
        }
    }

    @Nullable
    private static InputStream openClasspathResource(@Nonnull String resourcePath) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader selectedClassLoader = CombatExternalConfigBootstrap.class.getClassLoader();
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
