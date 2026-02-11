package org.runetale.skills.testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingStackSanityTest {

	@Mock
	private List<String> values;

	@Test
	void mockitoAndAssertjWorkTogetherInPluginTests() {
		when(values.get(0)).thenReturn("ok");

		String result = values.get(0);

		assertThat(result).isEqualTo("ok");
		verify(values).get(0);
	}
}
