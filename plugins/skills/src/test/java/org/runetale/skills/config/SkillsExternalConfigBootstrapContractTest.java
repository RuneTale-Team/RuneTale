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

		assertThat(layout.resolveConfigResourcePath("Skills/Config/xp.properties")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/combat.properties")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/hud.properties")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/crafting.properties")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/tooling.properties")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/heuristics.properties")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Nodes/index.list")).doesNotExist();
	}

	@Test
	void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
		Path xpPath = layout.resolveConfigResourcePath("Skills/Config/xp.properties");
		Files.createDirectories(xpPath.getParent());
		Files.writeString(xpPath, "maxLevel=12\nroundingMode=FLOOR\n");

		SkillsExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(Files.readString(xpPath)).isEqualTo("maxLevel=12\nroundingMode=FLOOR\n");
	}

	@Test
	void seedMissingDefaultsLeavesFeatureConfigsForFeaturePlugins(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		SkillsExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(layout.resolveConfigResourcePath("Skills/Config/crafting.properties")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/tooling.properties")).doesNotExist();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/heuristics.properties")).doesNotExist();
	}
}
