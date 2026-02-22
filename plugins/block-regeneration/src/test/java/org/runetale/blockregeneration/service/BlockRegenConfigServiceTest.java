package org.runetale.blockregeneration.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.blockregeneration.config.BlockRegenPathLayout;
import org.runetale.blockregeneration.domain.BlockRegenConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRegenConfigServiceTest {

    @Test
    void loadParsesCanonicalSchema(@TempDir Path tempDir) throws IOException {
        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("block-regeneration-data"));
        Path blocksPath = layout.resolveConfigResourcePath("BlockRegen/config/blocks.json");
        Files.createDirectories(blocksPath.getParent());
        Files.writeString(blocksPath, """
                {
                  "version": 2,
                  "enabled": true,
                  "respawnTickMillis": 250,
                  "notifyCooldownMillis": 1200,
                  "definitions": [
                    {
                      "id": "oak",
                      "enabled": true,
                      "blockId": "Tree_Oak",
                      "interactedBlockId": "Tree_Oak_Stump",
                      "gathering": { "type": "Specific", "amount": 3 },
                      "respawn": { "type": "Set", "millis": 5000 }
                    }
                  ]
                }
                """);

        BlockRegenConfig config = new BlockRegenConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.version()).isEqualTo(2);
        assertThat(config.respawnTickMillis()).isEqualTo(250L);
        assertThat(config.notifyCooldownMillis()).isEqualTo(1200L);
        assertThat(config.definitions()).hasSize(1);
        assertThat(config.definitions().get(0).blockIdPattern()).isEqualTo("Tree_Oak");
        assertThat(config.definitions().get(0).gatheringTrigger().amount()).isEqualTo(3);
        assertThat(config.definitions().get(0).respawnDelay().millis()).isEqualTo(5000L);
    }

    @Test
    void loadParsesAliasSchemaWithArrayObjects(@TempDir Path tempDir) throws IOException {
        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("block-regeneration-data"));
        Path blocksPath = layout.resolveConfigResourcePath("BlockRegen/config/blocks.json");
        Files.createDirectories(blocksPath.getParent());
        Files.writeString(blocksPath, """
                {
                  "definitions": [
                    {
                      "id": "iron",
                      "Block_ID": "Ore_Iron_*",
                      "Interacted block": "Empty_Ore_Vein",
                      "Respawn": [
                        { "Type": "Random", "Seconds_Min": 3, "Seconds_Max": 7 }
                      ],
                      "Gathering": [
                        { "Type": "Specific", "Amount": 1 }
                      ]
                    }
                  ]
                }
                """);

        BlockRegenConfig config = new BlockRegenConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.definitions()).hasSize(1);
        assertThat(config.definitions().get(0).blockIdPattern()).isEqualTo("Ore_Iron_*");
        assertThat(config.definitions().get(0).interactedBlockId()).isEqualTo("Empty_Ore_Vein");
        assertThat(config.definitions().get(0).respawnDelay().millisMin()).isEqualTo(3000L);
        assertThat(config.definitions().get(0).respawnDelay().millisMax()).isEqualTo(7000L);
    }
}
