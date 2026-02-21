package org.runetale.skills.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CombatStyleTypeTest {

	@Test
	void tryParseSupportsAliasesAndExactModeNames() {
		assertThat(CombatStyleType.tryParse("attack")).isEqualTo(CombatStyleType.ACCURATE);
		assertThat(CombatStyleType.tryParse("strength")).isEqualTo(CombatStyleType.AGGRESSIVE);
		assertThat(CombatStyleType.tryParse("defense")).isEqualTo(CombatStyleType.DEFENSIVE);
		assertThat(CombatStyleType.tryParse("defence")).isEqualTo(CombatStyleType.DEFENSIVE);
		assertThat(CombatStyleType.tryParse("controlled")).isEqualTo(CombatStyleType.CONTROLLED);
	}

	@Test
	void tryParseReturnsNullForUnknownOrBlankValues() {
		assertThat(CombatStyleType.tryParse(null)).isNull();
		assertThat(CombatStyleType.tryParse("   ")).isNull();
		assertThat(CombatStyleType.tryParse("berserk")).isNull();
	}
}
