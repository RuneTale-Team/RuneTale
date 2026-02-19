package org.runetale.skills.component;

import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;
import org.runetale.testing.core.TestConstructors;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerSkillProfileComponentTest {

	@Test
	void absentSkillDefaultsToZeroXpAndLevelOne() {
		PlayerSkillProfileComponent profile = TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class);

		assertThat(profile.getExperience(SkillType.MINING)).isZero();
		assertThat(profile.getLevel(SkillType.MINING)).isEqualTo(1);
	}

	@Test
	void setUpdatesOnlyTargetSkillProgress() {
		PlayerSkillProfileComponent profile = TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class);

		profile.set(SkillType.MINING, 150L, 12);

		assertThat(profile.getExperience(SkillType.MINING)).isEqualTo(150L);
		assertThat(profile.getLevel(SkillType.MINING)).isEqualTo(12);
		assertThat(profile.getExperience(SkillType.WOODCUTTING)).isZero();
		assertThat(profile.getLevel(SkillType.WOODCUTTING)).isEqualTo(1);
	}

	@Test
	void cloneIsDeepAndIndependent() {
		PlayerSkillProfileComponent profile = TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class);
		profile.set(SkillType.SMITHING, 500L, 20);

		PlayerSkillProfileComponent clone = (PlayerSkillProfileComponent) profile.clone();
		clone.set(SkillType.SMITHING, 5L, 2);

		assertThat(profile.getExperience(SkillType.SMITHING)).isEqualTo(500L);
		assertThat(profile.getLevel(SkillType.SMITHING)).isEqualTo(20);
		assertThat(clone.getExperience(SkillType.SMITHING)).isEqualTo(5L);
		assertThat(clone.getLevel(SkillType.SMITHING)).isEqualTo(2);
	}

	@Test
	void getOrCreateMigratesLegacyDefenseKeyToDefence() {
		PlayerSkillProfileComponent profile = TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class);
		profile.getRawSkillProgressByName().put("DEFENSE", new org.runetale.skills.domain.SkillProgress(123L, 9));

		assertThat(profile.getExperience(SkillType.DEFENCE)).isEqualTo(123L);
		assertThat(profile.getLevel(SkillType.DEFENCE)).isEqualTo(9);
		assertThat(profile.getRawSkillProgressByName()).containsKey("DEFENCE");
		assertThat(profile.getRawSkillProgressByName()).doesNotContainKey("DEFENSE");
	}
}
