package org.runetale.skills.crafting.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class CraftingExternalConfigBootstrapContractTest {

	@Test
	void seedMissingDefaultsCopiesCraftingConfig(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		CraftingExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(layout.resolveConfigResourcePath("Skills/Config/crafting.json")).exists();
	}

	@Test
	void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
		Path craftingPath = layout.resolveConfigResourcePath("Skills/Config/crafting.json");
		Files.createDirectories(craftingPath.getParent());
		Files.writeString(craftingPath, "{\"bench\":{\"anvilId\":\"Custom_Anvil\"}}\n");

		CraftingExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(Files.readString(craftingPath)).isEqualTo("{\"bench\":{\"anvilId\":\"Custom_Anvil\"}}\n");
	}
}
