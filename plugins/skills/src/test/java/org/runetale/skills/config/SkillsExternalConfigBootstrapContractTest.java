package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class SkillsExternalConfigBootstrapContractTest {

	@Test
	void seedMissingDefaultsCopiesExpectedCoreResources(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		SkillsExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(layout.resolveConfigResourcePath("Skills/Config/skills.json")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/crafting.json")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/gathering.json")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/equipment.json")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Nodes/nodes.json")).doesNotExist();
	}

	@Test
	void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
		Path xpPath = layout.resolveConfigResourcePath("Skills/Config/skills.json");
		Files.createDirectories(xpPath.getParent());
		Files.writeString(xpPath, "{\"xp\":{\"maxLevel\":12,\"roundingMode\":\"FLOOR\"}}\n");

		SkillsExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(Files.readString(xpPath)).isEqualTo("{\"xp\":{\"maxLevel\":12,\"roundingMode\":\"FLOOR\"}}\n");
	}

	@Test
	void seedMissingDefaultsLeavesFeatureConfigsForFeaturePlugins(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		SkillsExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(layout.resolveConfigResourcePath("Skills/Config/crafting.json")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/gathering.json")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/equipment.json")).doesNotExist();
	}
}
