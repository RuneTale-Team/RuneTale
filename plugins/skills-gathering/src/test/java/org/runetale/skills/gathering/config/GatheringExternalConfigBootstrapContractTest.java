package org.runetale.skills.gathering.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class GatheringExternalConfigBootstrapContractTest {

	@Test
	void seedMissingDefaultsCopiesGatheringResources(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		GatheringExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(layout.resolveConfigResourcePath("Skills/Config/gathering.json")).exists();
		assertThat(layout.resolveConfigResourcePath("Skills/Nodes/nodes.json")).exists();
	}

	@Test
	void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
		Path heuristicsPath = layout.resolveConfigResourcePath("Skills/Config/gathering.json");
		Files.createDirectories(heuristicsPath.getParent());
		Files.writeString(heuristicsPath, "{\"heuristics\":{\"nodeCandidateTokens\":[\"ore\",\"rock\"]}}\n");

		GatheringExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(Files.readString(heuristicsPath)).isEqualTo("{\"heuristics\":{\"nodeCandidateTokens\":[\"ore\",\"rock\"]}}\n");
	}

	@Test
	void seedMissingDefaultsCopiesGroupedNodeDefinitions(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		GatheringExternalConfigBootstrap.seedMissingDefaults(layout);

		Path nodesPath = layout.resolveConfigResourcePath("Skills/Nodes/nodes.json");
		assertThat(nodesPath).exists();

		try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(nodesPath), StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			assertThat(parsed.isJsonObject()).isTrue();
			JsonObject object = parsed.getAsJsonObject();
			assertThat(object.keySet()).contains("woodcutting", "mining");
		}
	}
}
