package org.runetale.testing.junit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractTestAnnotationTest {

	@Test
	void contractTestCarriesContractTag() {
		Tag tag = ContractTest.class.getAnnotation(Tag.class);

		assertThat(tag).isNotNull();
		assertThat(tag.value()).isEqualTo("contract");
	}
}
