package org.runetale.skills.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ToolSpeedThrottleServiceTest {

	@Test
	void allowHitAlwaysPassesForFullEfficiency() {
		ToolSpeedThrottleService throttle = new ToolSpeedThrottleService(10_000L, 256);
		UUID playerId = UUID.randomUUID();

		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 1.0D, 1_000L)).isTrue();
		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 1.0D, 1_001L)).isTrue();
	}

	@Test
	void allowHitThrottlesByAccumulatedEfficiency() {
		ToolSpeedThrottleService throttle = new ToolSpeedThrottleService(10_000L, 256);
		UUID playerId = UUID.randomUUID();

		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 0.40D, 1_000L)).isFalse();
		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 0.40D, 1_001L)).isFalse();
		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 0.40D, 1_002L)).isTrue();
		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 0.40D, 1_003L)).isFalse();
		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 0.40D, 1_004L)).isTrue();
	}

	@Test
	void allowHitResetsAfterStateExpires() {
		ToolSpeedThrottleService throttle = new ToolSpeedThrottleService(100L, 256);
		UUID playerId = UUID.randomUUID();

		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 0.75D, 1_000L)).isFalse();
		assertThat(throttle.allowHit(playerId, "Ore_Iron_A", 1, 2, 3, 0.75D, 1_200L)).isFalse();
	}
}
