package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CraftingConfigTest {

	@Test
	void loadUsesClasspathDefaultsWhenExternalFileMissing(@TempDir Path tempDir) {
		CraftingConfig config = CraftingConfig.load(tempDir);

		assertThat(config.anvilBenchId()).isEqualTo("RuneTale_Anvil");
		assertThat(config.furnaceBenchId()).isEqualTo("RuneTale_Furnace");
		assertThat(config.smithingCraftDurationMillis()).isEqualTo(3000L);
		assertThat(config.smeltingCraftDurationMillis()).isEqualTo(3000L);
		assertThat(config.maxCraftCount()).isEqualTo(999);
		assertThat(config.quantityPresets()).containsExactly(1, 5, 10);
		assertThat(config.quantityAllToken()).isEqualTo("ALL");
		assertThat(config.smeltingOutputContainsToken()).isEqualTo("runetale_bar_");
		assertThat(config.pageProgressTickSeconds()).isEqualTo(0.05F);
	}

	@Test
	void loadClampsAndParsesExternalOverrides(@TempDir Path tempDir) throws IOException {
		write(tempDir, "Config/crafting.json", """
				{
				  "bench": {
				    "anvilId": "Custom_Anvil"
				  },
				  "smithing": {
				    "craftDurationMillis": -20
				  },
				  "smelting": {
				    "craftDurationMillis": 0,
				    "outputContainsToken": "ingot"
				  },
				  "craft": {
				    "maxCount": 0,
				    "quantityPresets": [10, "bad", -1, 3],
				    "quantityAllToken": "all"
				  },
				  "pageProgressTickSeconds": 0
				}
				""");

		CraftingConfig config = CraftingConfig.load(tempDir);

		assertThat(config.anvilBenchId()).isEqualTo("Custom_Anvil");
		assertThat(config.furnaceBenchId()).isEqualTo("RuneTale_Furnace");
		assertThat(config.smithingCraftDurationMillis()).isEqualTo(1L);
		assertThat(config.smeltingCraftDurationMillis()).isEqualTo(1L);
		assertThat(config.maxCraftCount()).isEqualTo(1);
		assertThat(config.quantityPresets()).containsExactly(10, 3);
		assertThat(config.quantityAllToken()).isEqualTo("all");
		assertThat(config.smeltingOutputContainsToken()).isEqualTo("ingot");
		assertThat(config.pageProgressTickSeconds()).isEqualTo(0.01F);
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}
}
