package org.runetale.testing.ecs;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.EcsEvent;
import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class RecordingComponentAccessorTest {

	@Test
	void recordsEntityAndWorldInvocations() {
		RecordingComponentAccessor<String> accessor = new RecordingComponentAccessor<>("test-store");
		TestEvent entityEvent = new TestEvent("entity");
		TestEvent worldEvent = new TestEvent("world");

		accessor.invoke((Ref<String>) null, entityEvent);
		accessor.invoke(worldEvent);

		assertThat(accessor.getExternalData()).isEqualTo("test-store");
		assertThat(accessor.getEntityInvocations()).hasSize(1);
		assertThat(accessor.getEntityInvocations().getFirst().event()).isEqualTo(entityEvent);
		assertThat(accessor.getWorldInvocations()).containsExactly(worldEvent);
	}

	@Test
	void clearInvocationsRemovesRecordedEvents() {
		RecordingComponentAccessor<String> accessor = new RecordingComponentAccessor<>("test-store");

		accessor.invoke((Ref<String>) null, new TestEvent("entity"));
		accessor.invoke(new TestEvent("world"));
		accessor.clearInvocations();

		assertThat(accessor.getEntityInvocations()).isEmpty();
		assertThat(accessor.getWorldInvocations()).isEmpty();
	}

	private static final class TestEvent extends EcsEvent {

		private final String id;

		private TestEvent(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}
	}
}
