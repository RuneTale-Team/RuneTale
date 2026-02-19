package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkillSessionStatsServiceTest {

	@Test
	void recordGainTracksLatestSkillAndNormalizesNegativeGain() {
		SkillSessionStatsService service = new SkillSessionStatsService();
		UUID playerId = UUID.randomUUID();

		service.recordGain(playerId, SkillType.MINING, -10L);

		assertThat(service.getMostRecentGain(playerId)).isZero();
		assertThat(service.getMostRecentSkill(playerId)).isEqualTo(SkillType.MINING);

		service.recordGain(playerId, SkillType.WOODCUTTING, 42L);
		assertThat(service.getMostRecentGain(playerId)).isEqualTo(42L);
		assertThat(service.getMostRecentSkill(playerId)).isEqualTo(SkillType.WOODCUTTING);
	}

	@Test
	void clearRemovesTrackedState() {
		SkillSessionStatsService service = new SkillSessionStatsService();
		UUID playerId = UUID.randomUUID();

		service.recordGain(playerId, SkillType.SMITHING, 5L);
		service.clear(playerId);

		assertThat(service.getMostRecentGain(playerId)).isZero();
		assertThat(service.getMostRecentSkill(playerId)).isNull();
	}
}
