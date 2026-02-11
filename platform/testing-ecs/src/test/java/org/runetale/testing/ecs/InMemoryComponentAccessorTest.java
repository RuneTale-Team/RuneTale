package org.runetale.testing.ecs;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ContractTest
class InMemoryComponentAccessorTest {

	@Test
	void putAndGetComponentRoundTripsStoredValue() {
		InMemoryComponentAccessor<String> accessor = new InMemoryComponentAccessor<>("store");
		Ref<String> ref = mock(Ref.class);
		ComponentType<String, DummyComponent> type = new ComponentType<>();
		DummyComponent component = new DummyComponent(42);

		accessor.putComponent(ref, type, component);

		assertThat(accessor.getComponent(ref, type)).isSameAs(component);
	}

	@Test
	void ensureAndGetComponentCreatesWithRegisteredFactory() {
		InMemoryComponentAccessor<String> accessor = new InMemoryComponentAccessor<>("store");
		Ref<String> ref = mock(Ref.class);
		ComponentType<String, DummyComponent> type = new ComponentType<>();
		accessor.registerFactory(type, () -> new DummyComponent(7));

		DummyComponent created = accessor.ensureAndGetComponent(ref, type);
		DummyComponent fetched = accessor.ensureAndGetComponent(ref, type);

		assertThat(created.getValue()).isEqualTo(7);
		assertThat(fetched).isSameAs(created);
	}

	@Test
	void ensureAndGetComponentFailsWithoutFactory() {
		InMemoryComponentAccessor<String> accessor = new InMemoryComponentAccessor<>("store");
		Ref<String> ref = mock(Ref.class);
		ComponentType<String, DummyComponent> type = new ComponentType<>();

		assertThatThrownBy(() -> accessor.ensureAndGetComponent(ref, type))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No component factory registered");
	}

	private static final class DummyComponent implements Component<String> {

		private final int value;

		private DummyComponent(int value) {
			this.value = value;
		}

		private int getValue() {
			return value;
		}

		@Override
		public Component<String> clone() {
			return new DummyComponent(this.value);
		}
	}
}
