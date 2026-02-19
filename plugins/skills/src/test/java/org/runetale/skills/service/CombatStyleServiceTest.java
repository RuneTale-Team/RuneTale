package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.CombatStyleType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CombatStyleServiceTest {

	@Test
	void returnsAccurateByDefaultAndSupportsSetClear() {
		CombatStyleService service = new CombatStyleService();
		UUID playerId = UUID.randomUUID();

		assertThat(service.getCombatStyle(playerId)).isEqualTo(CombatStyleType.ACCURATE);

		service.setCombatStyle(playerId, CombatStyleType.DEFENSIVE);
		assertThat(service.getCombatStyle(playerId)).isEqualTo(CombatStyleType.DEFENSIVE);

		service.clear(playerId);
		assertThat(service.getCombatStyle(playerId)).isEqualTo(CombatStyleType.ACCURATE);
	}
}
