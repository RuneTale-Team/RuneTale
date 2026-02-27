package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.CombatConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConstitutionHealthServiceTest {

	@Test
	void constitutionHealthBonusStartsAfterConfiguredBaseLevel() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatConfig config = mock(CombatConfig.class);
		ConstitutionHealthService service = new ConstitutionHealthService(runtimeApi, config);

		when(config.constitutionBaseLevel()).thenReturn(10);
		when(config.constitutionHealthPerLevel()).thenReturn(10.0D);

		assertThat(service.constitutionHealthBonus(1)).isZero();
		assertThat(service.constitutionHealthBonus(10)).isZero();
		assertThat(service.constitutionHealthBonus(11)).isEqualTo(10.0D);
		assertThat(service.constitutionHealthBonus(25)).isEqualTo(150.0D);
	}
}
