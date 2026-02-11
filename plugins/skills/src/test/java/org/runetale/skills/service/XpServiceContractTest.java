package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class XpServiceContractTest {

	@Test
	void xpThresholdsAreMonotonicAndAnchoredAtZero() {
		XpService service = new XpService();

		assertThat(service.xpForLevel(1)).isZero();
		assertThat(service.xpForLevel(2)).isGreaterThan(service.xpForLevel(1));
		assertThat(service.xpForLevel(3)).isGreaterThan(service.xpForLevel(2));
		assertThat(service.xpForLevel(4)).isGreaterThan(service.xpForLevel(3));
	}

	@Test
	void levelForXpMatchesThresholdBoundaries() {
		XpService service = new XpService();
		long levelTwoThreshold = service.xpForLevel(2);
		long levelThreeThreshold = service.xpForLevel(3);

		assertThat(service.levelForXp(0L)).isEqualTo(1);
		assertThat(service.levelForXp(levelTwoThreshold - 1L)).isEqualTo(1);
		assertThat(service.levelForXp(levelTwoThreshold)).isEqualTo(2);
		assertThat(service.levelForXp(levelThreeThreshold - 1L)).isEqualTo(2);
		assertThat(service.levelForXp(levelThreeThreshold)).isEqualTo(3);
	}

	@Test
	void addXpClampsNegativeInputsAndRoundsGain() {
		XpService service = new XpService();

		assertThat(service.addXp(-100L, 10.4D)).isEqualTo(10L);
		assertThat(service.addXp(5L, -3.9D)).isEqualTo(5L);
		assertThat(service.addXp(5L, 3.5D)).isEqualTo(9L);
	}
}
