package org.runetale.blockregeneration.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.blockregeneration.config.BlockRegenPathLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRegenCoordinatorServiceTest {

    @Test
    void handleSuccessfulInteractionReturnsNotMatchedWhenDisabled(@TempDir Path tempDir) throws IOException {
        BlockRegenCoordinatorService coordinator = createCoordinator(tempDir, """
                {
                  "enabled": false,
                  "definitions": [
                    {
                      "id": "oak",
                      "blockId": "Tree_Oak",
                      "placeholderBlockId": "Tree_Oak_Stump",
                      "gathering": { "type": "Specific", "amount": 1 },
                      "respawn": { "type": "Set", "millis": 1000 }
                    }
                  ]
                }
                """);

        coordinator.initialize();

        BlockRegenCoordinatorService.HandleOutcome outcome = coordinator.handleSuccessfulInteraction(
                "break",
                "world",
                1,
                2,
                3,
                "Tree_Oak",
                100L);

        assertThat(outcome.matched()).isFalse();
    }

    @Test
    void reloadClearsRuntimeState(@TempDir Path tempDir) throws IOException {
        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("block-regeneration-data"));
        Path blocksPath = layout.resolveConfigResourcePath("BlockRegen/config/blocks.json");
        Files.createDirectories(blocksPath.getParent());
        Files.writeString(blocksPath, """
                {
                  "enabled": true,
                  "definitions": [
                    {
                      "id": "oak",
                      "blockId": "Tree_Oak",
                      "placeholderBlockId": "Tree_Oak_Stump",
                      "gathering": { "type": "Specific", "amount": 1 },
                      "respawn": { "type": "Set", "millis": 1000 }
                    }
                  ]
                }
                """);

        BlockRegenCoordinatorService coordinator = new BlockRegenCoordinatorService(
                new BlockRegenConfigService(layout.pluginConfigRoot()),
                new BlockRegenDefinitionService(),
                new BlockRegenRuntimeService(new Random(1L)),
                new BlockRegenPlacementQueueService());
        coordinator.initialize();
        coordinator.handleSuccessfulInteraction("break", "world", 1, 2, 3, "Tree_Oak", 10L);

        assertThat(coordinator.inspectState("world", 1, 2, 3)).isNotNull();

        coordinator.reload();

        assertThat(coordinator.inspectState("world", 1, 2, 3)).isNull();
    }

    @Test
    void clearRuntimeStateAtRemovesSpecificPosition(@TempDir Path tempDir) throws IOException {
        BlockRegenCoordinatorService coordinator = createCoordinator(tempDir, """
                {
                  "enabled": true,
                  "definitions": [
                    {
                      "id": "oak",
                      "blockId": "Tree_Oak",
                      "placeholderBlockId": "Tree_Oak_Stump",
                      "gathering": { "type": "Specific", "amount": 1 },
                      "respawn": { "type": "Set", "millis": 1000 }
                    }
                  ]
                }
                """);

        coordinator.initialize();
        coordinator.handleSuccessfulInteraction("break", "world", 7, 8, 9, "Tree_Oak", 10L);
        assertThat(coordinator.inspectState("world", 7, 8, 9)).isNotNull();

        coordinator.clearRuntimeStateAt("world", 7, 8, 9);

        assertThat(coordinator.inspectState("world", 7, 8, 9)).isNull();
    }

    private static BlockRegenCoordinatorService createCoordinator(Path tempDir, String json) throws IOException {
        BlockRegenPathLayout layout = BlockRegenPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("block-regeneration-data"));
        Path blocksPath = layout.resolveConfigResourcePath("BlockRegen/config/blocks.json");
        Files.createDirectories(blocksPath.getParent());
        Files.writeString(blocksPath, json);
        return new BlockRegenCoordinatorService(
                new BlockRegenConfigService(layout.pluginConfigRoot()),
                new BlockRegenDefinitionService(),
                new BlockRegenRuntimeService(new Random(1L)),
                new BlockRegenPlacementQueueService());
    }
}
