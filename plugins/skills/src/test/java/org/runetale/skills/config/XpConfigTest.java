package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class XpConfigTest {

	@Test
	void loadUsesClasspathDefaultsWhenExternalFileMissing(@TempDir Path tempDir) {
		XpConfig config = XpConfig.load(tempDir);

		assertThat(config.maxLevel()).isGreaterThanOrEqualTo(2);
		assertThat(config.levelTermMultiplier()).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.growthScale()).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.growthBase()).isGreaterThan(1.0D);
		assertThat(config.growthDivisor()).isGreaterThan(0.0D);
		assertThat(config.pointsDivisor()).isGreaterThanOrEqualTo(1);
		assertThat(config.roundingMode()).isNotNull();
	}

	@Test
	void loadClampsAndNormalizesInvalidExternalValues(@TempDir Path tempDir) throws IOException {
		write(tempDir, "Config/skills.json", """
				{
				  "xp": {
				    "maxLevel": 1,
				    "levelTermMultiplier": -5.0,
				    "growthScale": -1.0,
				    "growthBase": 0.5,
				    "growthDivisor": 0.0,
				    "pointsDivisor": 0,
				    "roundingMode": "not-a-mode"
				  }
				}
				""");

		XpConfig config = XpConfig.load(tempDir);

		assertThat(config.maxLevel()).isEqualTo(2);
		assertThat(config.levelTermMultiplier()).isEqualTo(0.0D);
		assertThat(config.growthScale()).isEqualTo(0.0D);
		assertThat(config.growthBase()).isEqualTo(1.000001D);
		assertThat(config.growthDivisor()).isEqualTo(0.000001D);
		assertThat(config.pointsDivisor()).isEqualTo(1);
		assertThat(config.roundingMode()).isEqualTo(XpRoundingMode.NEAREST);
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}
}
