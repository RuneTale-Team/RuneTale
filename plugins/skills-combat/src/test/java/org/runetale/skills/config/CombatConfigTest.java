package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CombatConfigTest {

	@Test
	void loadUsesClasspathDefaultsWhenExternalFileMissing(@TempDir Path tempDir) {
		CombatConfig config = CombatConfig.load(tempDir);

		assertThat(config.xpPerDamage()).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.constitutionXpPerDamage()).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.constitutionBaseLevel()).isGreaterThanOrEqualTo(1);
		assertThat(config.constitutionHealthPerLevel()).isGreaterThanOrEqualTo(0.0D);
		assertThat(config.sourceRanged()).isNotBlank();
		assertThat(config.sourceMeleePrefix()).isNotBlank();
		assertThat(config.sourceMeleeAccurate()).isNotBlank();
		assertThat(config.sourceMeleeAggressive()).isNotBlank();
		assertThat(config.sourceMeleeDefensive()).isNotBlank();
		assertThat(config.sourceMeleeControlledAttack()).isNotBlank();
		assertThat(config.sourceMeleeControlledStrength()).isNotBlank();
		assertThat(config.sourceMeleeControlledDefence()).isNotBlank();
		assertThat(config.sourceBlockDefence()).isNotBlank();
		assertThat(config.sourceConstitutionDamage()).isNotBlank();
		assertThat(config.sourceConstitutionBaseline()).isNotBlank();
		assertThat(config.projectileCauseTokens())
				.isNotEmpty()
				.allSatisfy(token -> {
					assertThat(token).isNotBlank();
					assertThat(token).isEqualTo(token.toLowerCase(Locale.ROOT));
				});
	}

	@Test
	void loadClampsNegativeXpAndNormalizesProjectileTokens(@TempDir Path tempDir) throws IOException {
		write(tempDir, "Config/combat.json", """
				{
				  "xpPerDamage": -4.5,
				  "constitution": {
				    "xpPerDamage": 3.3,
				    "baseLevel": -12,
				    "healthPerLevel": -10,
				    "source": {
				      "damage": "Combat:Constitution:Damage",
				      "baseline": "Combat:Constitution:Baseline"
				    }
				  },
				  "source": {
				    "ranged": "Combat:RANGED",
				    "melee": {
				      "prefix": "Combat:Melee:"
				    }
				  },
				  "projectileCauseTokens": ["Projectile", "ARROW_HIT", "  "]
				}
				""");

		CombatConfig config = CombatConfig.load(tempDir);

		assertThat(config.xpPerDamage()).isZero();
		assertThat(config.constitutionXpPerDamage()).isEqualTo(3.3D);
		assertThat(config.constitutionBaseLevel()).isEqualTo(1);
		assertThat(config.constitutionHealthPerLevel()).isZero();
		assertThat(config.sourceRanged()).isEqualTo("Combat:RANGED");
		assertThat(config.sourceMeleePrefix()).isEqualTo("Combat:Melee:");
		assertThat(config.sourceConstitutionDamage()).isEqualTo("Combat:Constitution:Damage");
		assertThat(config.sourceConstitutionBaseline()).isEqualTo("Combat:Constitution:Baseline");
		assertThat(config.projectileCauseTokens()).containsExactly("projectile", "arrow_hit");
	}

	@Test
	void loadFallsBackToLegacySkillsJsonWhenCombatJsonMissing(@TempDir Path tempDir) throws IOException {
		write(tempDir, "Config/skills.json", """
				{
				  "combat": {
				    "xpPerDamage": 9.0,
				    "source": {
				      "ranged": "legacy:ranged"
				    }
				  }
				}
				""");

		CombatConfig config = CombatConfig.load(tempDir);

		assertThat(config.xpPerDamage()).isEqualTo(9.0D);
		assertThat(config.sourceRanged()).isEqualTo("legacy:ranged");
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}
}
