package org.runetale.skills.config;

import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.domain.SkillType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ItemActionsConfigTest {

    @Test
    void loadUsesClasspathDefaultsWhenExternalFileMissing(@TempDir Path tempDir) {
        ItemActionsConfig config = ItemActionsConfig.load(tempDir);

        assertThat(config.actions()).isNotEmpty();
        assertThat(config.actions()).allSatisfy(action -> {
            assertThat(action.id()).isNotBlank();
            assertThat(action.itemId()).isNotBlank();
            assertThat(action.skillType()).isNotNull();
            assertThat(action.experience()).isGreaterThan(0.0D);
            assertThat(action.consumeQuantity()).isGreaterThanOrEqualTo(0);
            assertThat(action.source()).isNotBlank();
            assertThat(action.mouseButtonType()).isNotNull();
            assertThat(action.mouseButtonState()).isNotNull();
            assertThat(action.matchesItemId(action.itemId())).isTrue();
        });
        assertThat(config.debugPluginKey()).isNotBlank();
    }

    @Test
    void loadParsesExternalOverridesAndSkipsInvalidEntries(@TempDir Path tempDir) throws IOException {
        write(tempDir, "Config/item-actions.json", """
                {
                  "actions": [
                    {
                      "id": "prayer_bones",
                      "itemId": "RuneTale_Bones",
                      "skill": "PRAYER",
                      "xp": 7.25,
                      "consumeQuantity": 3,
                      "source": "prayer:custom",
                      "notifyPlayer": false,
                      "cancelInputEvent": false,
                      "allowCreative": true,
                      "targetBlockIds": [
                        "Furniture_Crude_Brazier",
                        "runetale:Furniture_Crude_Brazier"
                      ],
                      "replaceTargetBlockId": "Furniture_Crude_Brazier",
                      "replaceTargetBlockDelayMillis": 1200,
                      "requireTargetBlockMatchForReplacement": false,
                      "trigger": {
                        "mouseButton": "Middle",
                        "mouseState": "Released"
                      }
                    },
                    {
                      "id": "invalid",
                      "itemId": "RuneTale_Bones",
                      "skill": "unknown",
                      "xp": 5.0
                    }
                  ],
                  "debug": {
                    "pluginKey": "skills-prayer"
                  }
                }
                """);

        ItemActionsConfig config = ItemActionsConfig.load(tempDir);

        assertThat(config.actions()).hasSize(1);
        ItemActionsConfig.ItemXpActionDefinition action = config.actions().getFirst();
        assertThat(action.id()).isEqualTo("prayer_bones");
        assertThat(action.itemId()).isEqualTo("RuneTale_Bones");
        assertThat(action.skillType()).isEqualTo(SkillType.PRAYER);
        assertThat(action.experience()).isEqualTo(7.25D);
        assertThat(action.consumeQuantity()).isEqualTo(3);
        assertThat(action.source()).isEqualTo("prayer:custom");
        assertThat(action.notifyPlayer()).isFalse();
        assertThat(action.cancelInputEvent()).isFalse();
        assertThat(action.allowCreative()).isTrue();
        assertThat(action.mouseButtonType()).isEqualTo(MouseButtonType.Middle);
        assertThat(action.mouseButtonState()).isEqualTo(MouseButtonState.Released);
        assertThat(action.targetBlockIds()).containsExactly("Furniture_Crude_Brazier", "runetale:Furniture_Crude_Brazier");
        assertThat(action.replaceTargetBlockId()).isEqualTo("Furniture_Crude_Brazier");
        assertThat(action.replaceTargetBlockDelayMillis()).isEqualTo(1200L);
        assertThat(action.requireTargetBlockMatchForReplacement()).isFalse();
        assertThat(config.debugPluginKey()).isEqualTo("skills-prayer");
    }

    @Test
    void loadAllowsZeroConsumeQuantity(@TempDir Path tempDir) throws IOException {
        write(tempDir, "Config/item-actions.json", """
                {
                  "actions": [
                    {
                      "id": "firemaking_campfire",
                      "itemId": "RuneTale_Tinderbox",
                      "skill": "FIREMAKING",
                      "xp": 40.0,
                      "consumeQuantity": 0,
                      "targetBlockId": "RuneTale_Log",
                      "replaceTargetBlockId": "Furniture_Crude_Brazier",
                      "replaceTargetBlockDelayMillis": 1200
                    }
                  ]
                }
                """);

        ItemActionsConfig config = ItemActionsConfig.load(tempDir);

        assertThat(config.actions()).hasSize(1);
        ItemActionsConfig.ItemXpActionDefinition action = config.actions().getFirst();
        assertThat(action.consumeQuantity()).isZero();
        assertThat(action.requiresItemConsumption()).isFalse();
        assertThat(action.hasTargetBlockReplacement()).isTrue();
    }

    @Test
    void targetBlockMatchingSupportsExactAndNamespacedIds() {
        ItemActionsConfig.ItemXpActionDefinition action = new ItemActionsConfig.ItemXpActionDefinition(
                "firemaking_burn_logs",
                true,
                "RuneTale_Log",
                SkillType.FIREMAKING,
                40.0D,
                1,
                "firemaking:burn",
                true,
                true,
                false,
                MouseButtonType.Right,
                MouseButtonState.Pressed,
                java.util.List.of("Furniture_Crude_Brazier"));

        assertThat(action.matchesTargetBlockId("Furniture_Crude_Brazier")).isTrue();
        assertThat(action.matchesTargetBlockId("runetale:Furniture_Crude_Brazier")).isTrue();
        assertThat(action.matchesTargetBlockId("Furniture_Crude_Torch")).isFalse();
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path path = root.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
