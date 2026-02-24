package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HudConfigTest {

	@Test
	void loadUsesClasspathDefaultsWhenExternalFileMissing(@TempDir Path tempDir) {
		HudConfig config = HudConfig.load(tempDir);

		assertThat(config.toastDurationMillis()).isGreaterThanOrEqualTo(1L);
		assertThat(config.toastFadeDurationMillis()).isGreaterThanOrEqualTo(1L);
		assertThat(config.rootBackgroundFaded()).isNotBlank();
		assertThat(config.innerBackgroundFaded()).isNotBlank();
		assertThat(config.toastExpiryTickSeconds()).isGreaterThanOrEqualTo(0.01F);
	}

	@Test
	void loadClampsTimingAndUsesExternalColors(@TempDir Path tempDir) throws IOException {
		write(tempDir, "Config/skills.json", """
				{
				  "hud": {
				    "toast": {
				      "durationMillis": 0,
				      "fadeDurationMillis": -2,
				      "fade": {
				        "rootBackground": "#000000",
				        "innerBackground": "#111111"
				      },
				      "expiryTickSeconds": 0
				    }
				  }
				}
				""");

		HudConfig config = HudConfig.load(tempDir);

		assertThat(config.toastDurationMillis()).isEqualTo(1L);
		assertThat(config.toastFadeDurationMillis()).isEqualTo(1L);
		assertThat(config.rootBackgroundFaded()).isEqualTo("#000000");
		assertThat(config.innerBackgroundFaded()).isEqualTo("#111111");
		assertThat(config.toastExpiryTickSeconds()).isEqualTo(0.01F);
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}
}
