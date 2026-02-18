package org.runetale.testing.junit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@WithHytaleLogger
class WithHytaleLoggerTest {

	@Test
	void setsHytaleLogManagerPropertyDuringTestExecution() {
		assertThat(System.getProperty("java.util.logging.manager"))
				.isEqualTo("com.hypixel.hytale.logger.backend.HytaleLogManager");
	}
}
