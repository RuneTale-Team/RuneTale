package org.runetale.skills.testing;

import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.WithDeterministicEnvironment;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@WithDeterministicEnvironment
class DeterministicEnvironmentExtensionTest {

	@Test
	void appliesLocaleRootAndUtcTimezone() {
		assertThat(Locale.getDefault()).isEqualTo(Locale.ROOT);
		assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ZoneId.of("UTC"));
	}
}
