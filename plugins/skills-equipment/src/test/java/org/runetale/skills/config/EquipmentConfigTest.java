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

        assertThat(config.tagSkillRequired()).isEqualTo("EquipSkillRequirement");
        assertThat(config.tagLevelRequirement()).isEqualTo("EquipLevelRequirement");
        assertThat(config.enforceArmor()).isTrue();
        assertThat(config.enforceActiveHand()).isFalse();
        assertThat(config.enforceActiveHandReconcile()).isFalse();
        assertThat(config.enforceToolUseBlockDamage()).isTrue();
        assertThat(config.enforceToolUseBreakBlock()).isTrue();
        assertThat(config.enforceToolUseEntityDamage()).isTrue();
        assertThat(config.activeSectionHotbar()).isEqualTo(-1);
        assertThat(config.activeSectionTools()).isEqualTo(-8);
        assertThat(config.activeSelectionSlotsHotbar()).isEqualTo(9);
        assertThat(config.activeSelectionSlotsTools()).isEqualTo(9);
        assertThat(config.locationAliases()).containsKeys("mainhand", "head", "chest", "hands", "legs");
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
