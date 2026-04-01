package org.runetale.starterkit.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StarterKitPathLayoutTest {

    @Test
    void fromDataDirectoryBuildsExpectedRoots(@TempDir Path tempDir) {
        Path dataDirectory = tempDir.resolve("mods").resolve("starterkit-data");

        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(dataDirectory);

        assertThat(layout.modsRoot()).isEqualTo(tempDir.resolve("mods"));
        assertThat(layout.pluginConfigRoot())
                .isEqualTo(tempDir.resolve("mods").resolve("runetale").resolve("config").resolve("starterkit"));
    }

    @Test
    void externalRelativeResourcePathStripsStarterKitPrefix() {
        assertThat(StarterKitPathLayout.externalRelativeResourcePath("StarterKit/config/kit.json"))
                .isEqualTo("config/kit.json");
        assertThat(StarterKitPathLayout.externalRelativeResourcePath("config/kit.json"))
                .isEqualTo("config/kit.json");
    }

    @Test
    void resolveConfigResourcePathMapsIntoPluginConfigRoot(@TempDir Path tempDir) {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));

        Path resolved = layout.resolveConfigResourcePath("StarterKit/config/kit.json");

        assertThat(resolved).isEqualTo(layout.pluginConfigRoot().resolve("config").resolve("kit.json"));
    }
}
