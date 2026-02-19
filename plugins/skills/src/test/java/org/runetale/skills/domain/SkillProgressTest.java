package org.runetale.skills.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillProgressTest {

	@Test
	void constructorClampsNegativeValues() {
		SkillProgress progress = new SkillProgress(-10L, -2);

		assertThat(progress.getExperience()).isZero();
		assertThat(progress.getLevel()).isEqualTo(1);
	}

	@Test
	void setClampsNegativeValues() {
		SkillProgress progress = new SkillProgress(100L, 10);

		progress.set(-1L, -4);

		assertThat(progress.getExperience()).isZero();
		assertThat(progress.getLevel()).isEqualTo(1);
	}

	@Test
	void setAppliesNormalizedPositiveValues() {
		SkillProgress progress = new SkillProgress(0L, 1);

		progress.set(250L, 18);

		assertThat(progress.getExperience()).isEqualTo(250L);
		assertThat(progress.getLevel()).isEqualTo(18);
	}
}
