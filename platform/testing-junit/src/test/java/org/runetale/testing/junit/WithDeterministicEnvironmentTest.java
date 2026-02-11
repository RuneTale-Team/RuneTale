package org.runetale.testing.junit;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@WithDeterministicEnvironment
class WithDeterministicEnvironmentTest {

	@Test
	void setsLocaleRootAndUtcDuringTestExecution() {
		assertThat(Locale.getDefault()).isEqualTo(Locale.ROOT);
		assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ZoneId.of("UTC"));
	}
}
