package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.domain.ToolTier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ToolingConfigTest {

	@Test
	void loadUsesClasspathDefaultsForFamilyAndTierDetection(@TempDir Path tempDir) {
		ToolingConfig config = ToolingConfig.load(tempDir);

		assertThat(config.defaultKeyword()).isNotBlank();
		assertThat(config.matchesToolFamily("", "tool_hatchet")).isFalse();
		assertThat(config.matchesToolFamily("tool_hatchet_wood", "")).isFalse();
		String normalizedDefaultKeyword = config.defaultKeyword().trim().toLowerCase(Locale.ROOT)
				.replace('-', '_')
				.replace(' ', '_');
		assertThat(config.matchesToolFamily("x_" + normalizedDefaultKeyword + "_y", normalizedDefaultKeyword)).isTrue();
		assertThat(config.detectTier("tool_hatchet_unknown")).isEqualTo(ToolTier.NONE);
	}

	@Test
	void loadRespectsExternalOverrides(@TempDir Path tempDir) throws IOException {
		write(tempDir, "Config/gathering.json", """
				{
				  "tooling": {
				    "keywordDefault": "Custom_Hammer",
				    "families": {
				      "custom_hammer": ["custom_hammer", "rune_hammer"]
				    },
				    "tiers": {
				      "MITHRIL": ["ultra"]
				    }
				  }
				}
				""");

		ToolingConfig config = ToolingConfig.load(tempDir);

		assertThat(config.defaultKeyword()).isEqualTo("Custom_Hammer");
		assertThat(config.matchesToolFamily("rune_hammer_ultra", "custom_hammer")).isTrue();
		assertThat(config.matchesToolFamily("tool_hatchet_ultra", "custom_hammer")).isFalse();
		assertThat(config.detectTier("rune_hammer_ultra")).isEqualTo(ToolTier.MITHRIL);
		assertThat(config.detectTier("tool_hatchet_wood")).isEqualTo(ToolTier.NONE);
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}
}
