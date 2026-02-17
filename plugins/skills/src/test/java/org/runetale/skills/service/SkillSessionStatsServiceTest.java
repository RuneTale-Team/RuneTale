package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;
import org.runetale.testing.core.TestPlayerIds;
import org.runetale.testing.junit.ContractTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class SkillSessionStatsServiceTest {

	@Test
	void recordGainClampsNegativeXpAndTracksLastSkill() {
		SkillSessionStatsService service = new SkillSessionStatsService();
		UUID playerId = TestPlayerIds.fromKey("alice");

		service.recordGain(playerId, SkillType.MINING, -25L);

		assertThat(service.getMostRecentGain(playerId)).isZero();
		assertThat(service.getMostRecentSkill(playerId)).isEqualTo(SkillType.MINING);
	}

	@Test
	void clearRemovesRecentSessionStateForPlayerOnly() {
		SkillSessionStatsService service = new SkillSessionStatsService();
		UUID aliceId = TestPlayerIds.fromKey("alice");
		UUID bobId = TestPlayerIds.fromKey("bob");

		service.recordGain(aliceId, SkillType.MINING, 50L);
		service.recordGain(bobId, SkillType.SMITHING, 80L);

		service.clear(aliceId);

		assertThat(service.getMostRecentGain(aliceId)).isZero();
		assertThat(service.getMostRecentSkill(aliceId)).isNull();
		assertThat(service.getMostRecentGain(bobId)).isEqualTo(80L);
		assertThat(service.getMostRecentSkill(bobId)).isEqualTo(SkillType.SMITHING);
	}
}
