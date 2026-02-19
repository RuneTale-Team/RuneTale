package org.runetale.skills.gathering.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class GatheringExternalConfigBootstrapContractTest {

	@Test
	void seedMissingDefaultsCopiesGatheringResources(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		GatheringExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(layout.resolveConfigResourcePath("Skills/Config/tooling.properties")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/Config/heuristics.properties")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/tool-tier-defaults.properties")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/xp-profile-defaults.properties")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/Nodes/index.list")).exists();
	}

	@Test
	void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
		Path heuristicsPath = layout.resolveConfigResourcePath("Skills/Config/heuristics.properties");
		Files.createDirectories(heuristicsPath.getParent());
		Files.writeString(heuristicsPath, "nodeCandidateTokens=ore,rock\n");

		GatheringExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(Files.readString(heuristicsPath)).isEqualTo("nodeCandidateTokens=ore,rock\n");
	}

	@Test
	void seedMissingDefaultsCopiesAllNodeResourcesFromIndex(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		GatheringExternalConfigBootstrap.seedMissingDefaults(layout);

		Path indexPath = layout.resolveConfigResourcePath("Skills/Nodes/index.list");
		List<String> entries = Files.readAllLines(indexPath).stream()
				.map(String::trim)
				.filter(line -> !line.isBlank() && !line.startsWith("#"))
				.toList();

		assertThat(entries).isNotEmpty();
		for (String entry : entries) {
			assertThat(layout.resolveConfigResourcePath("Skills/Nodes/" + entry))
					.as("node resource %s", entry)
					.exists();
		}
	}
}
