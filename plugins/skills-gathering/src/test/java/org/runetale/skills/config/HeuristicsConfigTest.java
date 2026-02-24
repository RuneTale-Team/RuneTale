package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicsConfigTest {

	@Test
	void loadUsesClasspathDefaultsWhenExternalFileMissing(@TempDir Path tempDir) {
		HeuristicsConfig config = HeuristicsConfig.load(tempDir);

		assertThat(config.nodeCandidateTokens())
				.isNotEmpty()
				.allSatisfy(token -> {
					assertThat(token).isNotBlank();
					assertThat(token).isEqualTo(token.toLowerCase(Locale.ROOT));
				});
	}

	@Test
	void loadNormalizesConfiguredTokensToLowercase(@TempDir Path tempDir) throws IOException {
		write(tempDir, "Config/gathering.json", """
				{
				  "heuristics": {
				    "nodeCandidateTokens": ["Log", "TREE", "Ore_Chunk"]
				  }
				}
				""");

		HeuristicsConfig config = HeuristicsConfig.load(tempDir);

		assertThat(config.nodeCandidateTokens()).containsExactly("log", "tree", "ore_chunk");
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}
}
