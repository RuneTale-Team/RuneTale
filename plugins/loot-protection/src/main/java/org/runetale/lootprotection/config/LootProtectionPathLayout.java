package org.runetale.lootprotection.config;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;

public final class LootProtectionPathLayout {

    private static final String RUNETALE_NAMESPACE = "runetale";
    private static final String PLUGIN_ID = "loot-protection";

    private final Path modsRoot;
    private final Path pluginRuntimeRoot;
    private final Path pluginConfigRoot;

    private LootProtectionPathLayout(
            @Nonnull Path modsRoot,
            @Nonnull Path pluginRuntimeRoot,
            @Nonnull Path pluginConfigRoot) {
        this.modsRoot = modsRoot;
        this.pluginRuntimeRoot = pluginRuntimeRoot;
        this.pluginConfigRoot = pluginConfigRoot;
    }

    @Nonnull
    public static LootProtectionPathLayout fromDataDirectory(@Nonnull Path dataDirectory) {
        Path modsRoot = dataDirectory.getParent();
        if (modsRoot == null) {
            modsRoot = dataDirectory;
        }

        Path runtimeRoot = modsRoot;
        Path configRoot = modsRoot.resolve(RUNETALE_NAMESPACE).resolve("config").resolve(PLUGIN_ID);
        return new LootProtectionPathLayout(modsRoot, runtimeRoot, configRoot);
    }

    @Nonnull
    public Path modsRoot() {
        return this.modsRoot;
    }

    @Nonnull
    public Path pluginRuntimeRoot() {
        return this.pluginRuntimeRoot;
    }

    @Nonnull
    public Path pluginConfigRoot() {
        return this.pluginConfigRoot;
    }

    @Nonnull
    public Path resolveConfigResourcePath(@Nonnull String resourcePath) {
        String relative = externalRelativeResourcePath(resourcePath);
        return this.pluginConfigRoot.resolve(relative.replace('/', File.separatorChar));
    }

    @Nonnull
    public static String externalRelativeResourcePath(@Nonnull String resourcePath) {
        if (resourcePath.startsWith("LootProtection/")) {
            return resourcePath.substring("LootProtection/".length());
        }
        return resourcePath;
    }
}
