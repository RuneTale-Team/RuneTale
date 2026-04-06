package org.runetale.starterkit.config;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;

public final class StarterKitPathLayout {

    private static final String RUNETALE_NAMESPACE = "runetale";
    private static final String PLUGIN_ID = "starterkit";

    private final Path modsRoot;
    private final Path pluginConfigRoot;

    private StarterKitPathLayout(@Nonnull Path modsRoot, @Nonnull Path pluginConfigRoot) {
        this.modsRoot = modsRoot;
        this.pluginConfigRoot = pluginConfigRoot;
    }

    @Nonnull
    public static StarterKitPathLayout fromDataDirectory(@Nonnull Path dataDirectory) {
        Path modsRoot = dataDirectory.getParent();
        if (modsRoot == null) {
            modsRoot = dataDirectory;
        }
        Path configRoot = modsRoot.resolve(RUNETALE_NAMESPACE).resolve("config").resolve(PLUGIN_ID);
        return new StarterKitPathLayout(modsRoot, configRoot);
    }

    @Nonnull
    public Path modsRoot() {
        return this.modsRoot;
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
        if (resourcePath.startsWith("StarterKit/")) {
            return resourcePath.substring("StarterKit/".length());
        }
        return resourcePath;
    }
}
