package org.runetale.skills.combat.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.config.CombatConfig;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class CombatExternalConfigBootstrapContractTest {

	@Test
	void seedMissingDefaultsCopiesCombatConfig(@TempDir Path tempDir) {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));

		CombatExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(layout.resolveConfigResourcePath("Skills/Config/combat.json")).exists();
	}

	@Test
	void seedMissingDefaultsDoesNotOverwriteExistingCombatConfig(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
		Path combatPath = layout.resolveConfigResourcePath("Skills/Config/combat.json");
		Files.createDirectories(combatPath.getParent());
		Files.writeString(combatPath, "{\"xpPerDamage\":13.0}\n");

		CombatExternalConfigBootstrap.seedMissingDefaults(layout);

		assertThat(Files.readString(combatPath)).isEqualTo("{\"xpPerDamage\":13.0}\n");
	}

	@Test
	void seedMissingDefaultsMigratesLegacySkillsCombatSection(@TempDir Path tempDir) throws IOException {
		SkillsPathLayout layout = SkillsPathLayout.fromDataDirectory(tempDir.resolve("mods").resolve("skills-data"));
		Path legacyPath = layout.resolveConfigResourcePath("Skills/Config/skills.json");
		Files.createDirectories(legacyPath.getParent());
		Files.writeString(legacyPath, """
				{
				  "combat": {
				    "xpPerDamage": 11.0,
				    "source": {
				      "ranged": "legacy:ranged"
				    }
				  }
				}
				""");

		CombatExternalConfigBootstrap.seedMissingDefaults(layout);

		Path combatPath = layout.resolveConfigResourcePath("Skills/Config/combat.json");
		assertThat(combatPath).exists();

		CombatConfig config = CombatConfig.load(layout.pluginConfigRoot());
		assertThat(config.xpPerDamage()).isEqualTo(11.0D);
		assertThat(config.sourceRanged()).isEqualTo("legacy:ranged");
	}
}
