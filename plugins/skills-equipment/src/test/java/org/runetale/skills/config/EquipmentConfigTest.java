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

        assertThat(config.tagSkillRequired()).isEqualTo("EquipSkillRequired");
        assertThat(config.tagLevelRequirement()).isEqualTo("EquipLevelRequirement");
        assertThat(config.enforceArmor()).isTrue();
        assertThat(config.enforceActiveHand()).isTrue();
        assertThat(config.activeSectionHotbar()).isEqualTo(-1);
        assertThat(config.activeSectionTools()).isEqualTo(-8);
        assertThat(config.locationAliases()).containsKeys("mainhand", "head", "chest", "hands", "legs");
    }

    @Test
    void loadRespectsExternalOverrides(@TempDir Path tempDir) throws IOException {
        write(tempDir, "Config/equipment.properties", """
                enforce.armor=false
                enforce.activeHand=true
                activeSection.hotbar=-5
                armorScanTickSeconds=0.75
                tag.valueSeparator==>
                locationAlias.mainhand=main,mh
                """);

        EquipmentConfig config = EquipmentConfig.load(tempDir);

        assertThat(config.enforceArmor()).isFalse();
        assertThat(config.enforceActiveHand()).isTrue();
        assertThat(config.activeSectionHotbar()).isEqualTo(-5);
        assertThat(config.armorScanTickSeconds()).isEqualTo(0.75F);
        assertThat(config.tagValueSeparator()).isEqualTo("=>");
        assertThat(config.locationAliases().get("mainhand")).containsExactly("main", "mh");
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path path = root.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
