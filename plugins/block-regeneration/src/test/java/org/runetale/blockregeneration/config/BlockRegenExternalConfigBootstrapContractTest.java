package org.runetale.blockregeneration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class BlockRegenExternalConfigBootstrapContractTest {

    @Test
    void seedMissingDefaultsCopiesBlockRegenConfig(@TempDir Path tempDir) {
        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("block-regeneration-data"));

        BlockRegenExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(layout.resolveConfigResourcePath("BlockRegen/config/blocks.json")).exists();
    }

    @Test
    void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("block-regeneration-data"));
        Path blocksPath = layout.resolveConfigResourcePath("BlockRegen/config/blocks.json");
        Files.createDirectories(blocksPath.getParent());
        Files.writeString(blocksPath, "{\"version\":99}\n");

        BlockRegenExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(Files.readString(blocksPath)).isEqualTo("{\"version\":99}\n");
    }
}
