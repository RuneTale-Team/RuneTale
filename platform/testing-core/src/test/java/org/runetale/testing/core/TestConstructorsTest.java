package org.runetale.testing.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestConstructorsTest {

	@Test
	void instantiateNoArgsCanBuildObjectUsingPrivateConstructor() {
		PrivateFixture fixture = TestConstructors.instantiateNoArgs(PrivateFixture.class);

		assertThat(fixture).isNotNull();
		assertThat(fixture.value).isEqualTo("ok");
	}

	private static final class PrivateFixture {

		private final String value;

		private PrivateFixture() {
			this.value = "ok";
		}
	}
}
