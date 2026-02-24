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
		assertThat(config.detectTier("tool_hatchet_bronze")).isEqualTo(ToolTier.BRONZE);
		assertThat(config.detectTier("tool_hatchet_unknown")).isEqualTo(ToolTier.NONE);
		assertThat(config.noToolEfficiencyMultiplier()).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.mismatchedFamilyEfficiencyMultiplier()).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.efficiencyMultiplierFor(ToolTier.BRONZE)).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.efficiencyMultiplierFor(ToolTier.CRYSTAL)).isGreaterThanOrEqualTo(0.0D);
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
				    },
				    "efficiency": {
				      "noToolMultiplier": 0.05,
				      "mismatchedFamilyMultiplier": 0.15,
				      "defaultTierMultiplier": 0.70,
				      "tierMultipliers": {
				        "MITHRIL": 0.90,
				        "NONE": 0.25
				      }
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
		assertThat(config.noToolEfficiencyMultiplier()).isEqualTo(0.05D);
		assertThat(config.mismatchedFamilyEfficiencyMultiplier()).isEqualTo(0.15D);
		assertThat(config.efficiencyMultiplierFor(ToolTier.MITHRIL)).isEqualTo(0.90D);
		assertThat(config.efficiencyMultiplierFor(ToolTier.NONE)).isEqualTo(0.25D);
		assertThat(config.efficiencyMultiplierFor(ToolTier.RUNE)).isEqualTo(0.70D);
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}
}
