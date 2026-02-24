package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentConfigTest {

    @Test
    void loadUsesClasspathDefaultsWhenExternalFileMissing(@TempDir Path tempDir) {
        EquipmentConfig config = EquipmentConfig.load(tempDir);

        assertThat(config.tagSkillRequired()).isNotBlank();
        assertThat(config.tagLevelRequirement()).isNotBlank();
        assertThat(config.tagValueSeparator()).isNotBlank();
        assertThat(config.defaultRequiredLevel()).isGreaterThanOrEqualTo(1);
        assertThat(config.activeSelectionSlotsHotbar()).isGreaterThanOrEqualTo(1);
        assertThat(config.activeSelectionSlotsTools()).isGreaterThanOrEqualTo(1);
        assertThat(config.armorScanTickSeconds()).isGreaterThanOrEqualTo(0.05F);
        assertThat(config.notificationCooldownMillis()).isGreaterThanOrEqualTo(0L);
        assertThat(config.notificationMessageTemplate()).isNotBlank();
        assertThat(config.locationAliases())
                .isNotEmpty()
                .allSatisfy((location, aliases) -> {
                    assertThat(location).isNotBlank();
                    assertThat(aliases).isNotEmpty().allSatisfy(alias -> assertThat(alias).isNotBlank());
                });
        assertThat(config.debugPluginKey()).isNotBlank();
    }

    @Test
    void loadRespectsExternalOverrides(@TempDir Path tempDir) throws IOException {
        write(tempDir, "Config/equipment.json", """
                {
                  "tag": {
                    "skillRequired": "EquipSkillRequirement"
                  },
                  "enforce": {
                    "armor": false,
                    "activeHand": true,
                    "activeHandReconcile": true,
                    "toolUse": {
                      "entityDamage": false
                    }
                  },
                  "activeSection": {
                    "hotbar": -5
                  },
                  "activeSelectionSlots": {
                    "tools": 7
                  },
                  "armorScanTickSeconds": 0.75,
                  "locationAliases": {
                    "mainhand": ["main", "mh"]
                  }
                }
                """);

        EquipmentConfig config = EquipmentConfig.load(tempDir);

        assertThat(config.enforceArmor()).isFalse();
        assertThat(config.enforceActiveHand()).isTrue();
        assertThat(config.enforceActiveHandReconcile()).isTrue();
        assertThat(config.enforceToolUseEntityDamage()).isFalse();
        assertThat(config.activeSectionHotbar()).isEqualTo(-5);
        assertThat(config.activeSelectionSlotsTools()).isEqualTo(7);
        assertThat(config.armorScanTickSeconds()).isEqualTo(0.75F);
        assertThat(config.tagSkillRequired()).isEqualTo("EquipSkillRequirement");
        assertThat(config.locationAliases().get("mainhand")).containsExactly("main", "mh");
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path path = root.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
