package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.config.XpConfig;
import org.runetale.skills.config.XpRoundingMode;
import org.runetale.testing.junit.ContractTest;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class XpServiceContractTest {

	@Test
	void xpThresholdsAreMonotonicAndAnchoredAtZero() {
		XpService service = createService(XpRoundingMode.NEAREST);

		assertThat(service.xpForLevel(1)).isZero();
		assertThat(service.xpForLevel(2)).isGreaterThan(service.xpForLevel(1));
		assertThat(service.xpForLevel(3)).isGreaterThan(service.xpForLevel(2));
		assertThat(service.xpForLevel(4)).isGreaterThan(service.xpForLevel(3));
	}

	@Test
	void levelForXpMatchesThresholdBoundaries() {
		XpService service = createService(XpRoundingMode.NEAREST);
		long levelTwoThreshold = service.xpForLevel(2);
		long levelThreeThreshold = service.xpForLevel(3);

		assertThat(service.levelForXp(0L)).isEqualTo(1);
		assertThat(service.levelForXp(levelTwoThreshold - 1L)).isEqualTo(1);
		assertThat(service.levelForXp(levelTwoThreshold)).isEqualTo(2);
		assertThat(service.levelForXp(levelThreeThreshold - 1L)).isEqualTo(2);
		assertThat(service.levelForXp(levelThreeThreshold)).isEqualTo(3);
	}

	@Test
	void addXpRoundsByConfiguredModeAndClampsNegativeInputs() {
		XpService nearest = createService(XpRoundingMode.NEAREST);
		XpService floor = createService(XpRoundingMode.FLOOR);
		XpService ceil = createService(XpRoundingMode.CEIL);

		assertThat(nearest.addXp(-100L, 10.4D)).isEqualTo(10L);
		assertThat(nearest.addXp(5L, -3.9D)).isEqualTo(5L);
		assertThat(nearest.addXp(5L, 3.5D)).isEqualTo(9L);

		assertThat(floor.addXp(5L, 3.9D)).isEqualTo(8L);
		assertThat(ceil.addXp(5L, 3.1D)).isEqualTo(9L);
	}

	private static XpService createService(XpRoundingMode roundingMode) {
		return new XpService(new XpConfig(
				99,
				1.0D,
				300.0D,
				2.0D,
				7.0D,
				4,
				roundingMode));
	}
}
