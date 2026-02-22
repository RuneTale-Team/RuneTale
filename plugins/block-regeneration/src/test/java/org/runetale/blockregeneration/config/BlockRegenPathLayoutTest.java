package org.runetale.blockregeneration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRegenPathLayoutTest {

    @Test
    void fromDataDirectoryBuildsExpectedRuntimeRoots(@TempDir Path tempDir) {
        Path dataDirectory = tempDir.resolve("mods").resolve("block-regeneration-data");

        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(dataDirectory);

        assertThat(layout.modsRoot()).isEqualTo(tempDir.resolve("mods"));
        assertThat(layout.pluginRuntimeRoot()).isEqualTo(tempDir.resolve("mods"));
        assertThat(layout.pluginConfigRoot())
                .isEqualTo(tempDir.resolve("mods").resolve("runetale").resolve("config").resolve("block-regeneration"));
    }

    @Test
    void externalRelativeResourcePathStripsBlockRegenPrefixOnly() {
        assertThat(BlockRegenPathLayout.externalRelativeResourcePath("BlockRegen/config/blocks.json"))
                .isEqualTo("config/blocks.json");
        assertThat(BlockRegenPathLayout.externalRelativeResourcePath("config/blocks.json"))
                .isEqualTo("config/blocks.json");
    }

    @Test
    void resolveConfigResourcePathMapsIntoPluginConfigRoot(@TempDir Path tempDir) {
        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("block-regeneration-data"));

        Path resolved = layout.resolveConfigResourcePath("BlockRegen/config/blocks.json");

        assertThat(resolved)
                .isEqualTo(layout.pluginConfigRoot().resolve("config/blocks.json"));
    }
}
