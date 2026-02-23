package org.runetale.skills.equipment.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class EquipmentExternalConfigBootstrapContractTest {

    @Test
    void seedMissingDefaultsCopiesEquipmentConfig(@TempDir Path tempDir) {
        SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

        EquipmentExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(layout.resolveConfigResourcePath("Skills/Config/equipment.json")).exists();
    }

    @Test
    void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
        SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
        Path equipmentPath = layout.resolveConfigResourcePath("Skills/Config/equipment.json");
        Files.createDirectories(equipmentPath.getParent());
        Files.writeString(equipmentPath, "{\"enforce\":{\"armor\":false}}\n");

        EquipmentExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(Files.readString(equipmentPath)).isEqualTo("{\"enforce\":{\"armor\":false}}\n");
    }
}
