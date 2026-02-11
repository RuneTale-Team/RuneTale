package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.testing.core.TestPlayerIds;
import org.runetale.testing.junit.ContractTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class CombatStyleServiceContractTest {

	@Test
	void getCombatStyleDefaultsToAccurate() {
		CombatStyleService service = new CombatStyleService();
		UUID playerId = TestPlayerIds.fromKey("combat-default");

		assertThat(service.getCombatStyle(playerId)).isEqualTo(CombatStyleType.ACCURATE);
	}

	@Test
	void setCombatStyleOverridesDefault() {
		CombatStyleService service = new CombatStyleService();
		UUID playerId = TestPlayerIds.fromKey("combat-set");

		service.setCombatStyle(playerId, CombatStyleType.AGGRESSIVE);

		assertThat(service.getCombatStyle(playerId)).isEqualTo(CombatStyleType.AGGRESSIVE);
	}

	@Test
	void clearRestoresDefaultStyleForPlayerOnly() {
		CombatStyleService service = new CombatStyleService();
		UUID aliceId = TestPlayerIds.fromKey("combat-alice");
		UUID bobId = TestPlayerIds.fromKey("combat-bob");

		service.setCombatStyle(aliceId, CombatStyleType.DEFENSIVE);
		service.setCombatStyle(bobId, CombatStyleType.AGGRESSIVE);
		service.clear(aliceId);

		assertThat(service.getCombatStyle(aliceId)).isEqualTo(CombatStyleType.ACCURATE);
		assertThat(service.getCombatStyle(bobId)).isEqualTo(CombatStyleType.AGGRESSIVE);
	}
}
