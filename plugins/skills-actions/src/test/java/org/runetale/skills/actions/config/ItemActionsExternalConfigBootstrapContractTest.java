package org.runetale.skills.actions.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class ItemActionsExternalConfigBootstrapContractTest {

    @Test
    void seedMissingDefaultsCopiesItemActionsConfig(@TempDir Path tempDir) {
        SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

        ItemActionsExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(layout.resolveConfigResourcePath("Skills/Config/item-actions.json")).exists();
    }

    @Test
    void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
        SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
        Path actionsPath = layout.resolveConfigResourcePath("Skills/Config/item-actions.json");
        Files.createDirectories(actionsPath.getParent());
        Files.writeString(actionsPath, "{\"actions\":[]}\n");

        ItemActionsExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(Files.readString(actionsPath)).isEqualTo("{\"actions\":[]}\n");
    }
}
