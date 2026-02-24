package org.runetale.skills.config;

import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.protocol.InteractionType;
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

        assertThat(config.actions()).hasSize(1);
        ItemActionsConfig.ItemXpActionDefinition action = config.actions().getFirst();
        assertThat(action.id()).isEqualTo("prayer_bury_bones");
        assertThat(action.itemId()).isEqualTo("RuneTale_Bones");
        assertThat(action.skillType()).isEqualTo(SkillType.PRAYER);
        assertThat(action.experience()).isEqualTo(4.5D);
        assertThat(action.consumeQuantity()).isEqualTo(1);
        assertThat(action.mouseButtonType()).isEqualTo(MouseButtonType.Right);
        assertThat(action.mouseButtonState()).isEqualTo(MouseButtonState.Pressed);
        assertThat(action.matchesItemId("RuneTale_Bones")).isTrue();
        assertThat(action.matchesItemId("runetale:RuneTale_Bones")).isTrue();
        assertThat(action.matchesInteractionType(InteractionType.Secondary)).isTrue();
        assertThat(action.matchesInteractionType(InteractionType.Primary)).isFalse();
        assertThat(config.debugPluginKey()).isEqualTo("skills-actions");
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
        assertThat(config.debugPluginKey()).isEqualTo("skills-prayer");
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path path = root.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
