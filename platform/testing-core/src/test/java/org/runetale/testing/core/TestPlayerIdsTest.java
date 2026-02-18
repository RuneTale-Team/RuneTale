package org.runetale.testing.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestPlayerIdsTest {

	@Test
	void fromKeyIsDeterministicForSameKey() {
		UUID first = TestPlayerIds.fromKey("alice");
		UUID second = TestPlayerIds.fromKey("alice");

		assertThat(first).isEqualTo(second);
	}

	@Test
	void fromKeyProducesDifferentValuesForDifferentKeys() {
		UUID alice = TestPlayerIds.fromKey("alice");
		UUID bob = TestPlayerIds.fromKey("bob");

		assertThat(alice).isNotEqualTo(bob);
	}
}
