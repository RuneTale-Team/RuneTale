package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPathLayoutTest {

	@Test
	void fromDataDirectoryBuildsExpectedRuntimeRoots(@TempDir Path tempDir) {
		Path dataDirectory = tempDir.resolve("mods").resolve("skills-data");

		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(dataDirectory);

		assertThat(layout.modsRoot()).isEqualTo(tempDir.resolve("mods"));
		assertThat(layout.pluginRuntimeRoot()).isEqualTo(tempDir.resolve("mods").resolve("runetale").resolve("skills"));
		assertThat(layout.pluginConfigRoot())
				.isEqualTo(tempDir.resolve("mods").resolve("runetale").resolve("config").resolve("skills"));
	}

	@Test
	void externalRelativeResourcePathStripsSkillsPrefixOnly() {
		assertThat(SkillsPathLayout.externalRelativeResourcePath("Skills/Config/xp.properties"))
				.isEqualTo("Config/xp.properties");
		assertThat(SkillsPathLayout.externalRelativeResourcePath("Config/xp.properties"))
				.isEqualTo("Config/xp.properties");
	}

	@Test
	void resolveConfigResourcePathMapsIntoPluginConfigRoot(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		Path resolved = layout.resolveConfigResourcePath("Skills/Nodes/mining/copper.properties");

		assertThat(resolved)
				.isEqualTo(layout.pluginConfigRoot().resolve("Nodes/mining/copper.properties"));
	}
}
