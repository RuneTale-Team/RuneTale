package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;

public final class SkillsPathLayout {

    private static final String RUNETALE_NAMESPACE = "runetale";
    private static final String SKILLS_PLUGIN_ID = "skills";

    private final Path modsRoot;
    private final Path pluginRuntimeRoot;
    private final Path pluginConfigRoot;

    private SkillsPathLayout(
            @Nonnull Path modsRoot,
            @Nonnull Path pluginRuntimeRoot,
            @Nonnull Path pluginConfigRoot) {
        this.modsRoot = modsRoot;
        this.pluginRuntimeRoot = pluginRuntimeRoot;
        this.pluginConfigRoot = pluginConfigRoot;
    }

    @Nonnull
    public static SkillsPathLayout fromDataDirectory(@Nonnull Path dataDirectory) {
        Path modsRoot = dataDirectory.getParent();
        if (modsRoot == null) {
            modsRoot = dataDirectory;
        }

        Path runetaleRoot = modsRoot.resolve(RUNETALE_NAMESPACE);
        Path configRoot = runetaleRoot.resolve("config").resolve(SKILLS_PLUGIN_ID);
        Path runtimeRoot = runetaleRoot.resolve(SKILLS_PLUGIN_ID);
        return new SkillsPathLayout(modsRoot, runtimeRoot, configRoot);
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
        if (resourcePath.startsWith("Skills/")) {
            return resourcePath.substring("Skills/".length());
        }
        return resourcePath;
    }
}
