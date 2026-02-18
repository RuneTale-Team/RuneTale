package org.runetale.testing.ecs;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Recording accessor with in-memory component storage for contract tests.
 */
public class InMemoryComponentAccessor<ECS_TYPE> extends RecordingComponentAccessor<ECS_TYPE> {

	private final Map<Ref<ECS_TYPE>, Map<ComponentType<ECS_TYPE, ?>, Component<ECS_TYPE>>> componentsByRef = new IdentityHashMap<>();
	private final Map<ComponentType<ECS_TYPE, ?>, Supplier<? extends Component<ECS_TYPE>>> factoriesByType = new IdentityHashMap<>();

	public InMemoryComponentAccessor(ECS_TYPE externalData) {
		super(externalData);
	}

	public <T extends Component<ECS_TYPE>> void registerFactory(ComponentType<ECS_TYPE, T> componentType,
			Supplier<T> factory) {
		this.factoriesByType.put(componentType, factory);
	}

	@Override
	public <T extends Component<ECS_TYPE>> T getComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType) {
		Map<ComponentType<ECS_TYPE, ?>, Component<ECS_TYPE>> byType = this.componentsByRef.get(ref);
		if (byType == null) {
			return null;
		}

		return cast(byType.get(componentType));
	}

	@Override
	public <T extends Component<ECS_TYPE>> T ensureAndGetComponent(Ref<ECS_TYPE> ref,
			ComponentType<ECS_TYPE, T> componentType) {
		T existing = getComponent(ref, componentType);
		if (existing != null) {
			return existing;
		}

		T created = createFromFactory(componentType);
		putComponent(ref, componentType, created);
		return created;
	}

	@Override
	public <T extends Component<ECS_TYPE>> void putComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType,
			T component) {
		this.componentsByRef.computeIfAbsent(ref, ignored -> new IdentityHashMap<>()).put(componentType, component);
	}

	@Override
	public <T extends Component<ECS_TYPE>> void addComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType,
			T component) {
		putComponent(ref, componentType, component);
	}

	@Override
	public <T extends Component<ECS_TYPE>> T addComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType) {
		T created = createFromFactory(componentType);
		putComponent(ref, componentType, created);
		return created;
	}

	@Override
	public <T extends Component<ECS_TYPE>> void removeComponent(Ref<ECS_TYPE> ref,
			ComponentType<ECS_TYPE, T> componentType) {
		Map<ComponentType<ECS_TYPE, ?>, Component<ECS_TYPE>> byType = this.componentsByRef.get(ref);
		if (byType == null || byType.remove(componentType) == null) {
			throw new IllegalStateException("Component not found for removal");
		}
	}

	@Override
	public <T extends Component<ECS_TYPE>> void tryRemoveComponent(Ref<ECS_TYPE> ref,
			ComponentType<ECS_TYPE, T> componentType) {
		Map<ComponentType<ECS_TYPE, ?>, Component<ECS_TYPE>> byType = this.componentsByRef.get(ref);
		if (byType != null) {
			byType.remove(componentType);
		}
	}

	private <T extends Component<ECS_TYPE>> T createFromFactory(ComponentType<ECS_TYPE, T> componentType) {
		Supplier<? extends Component<ECS_TYPE>> factory = this.factoriesByType.get(componentType);
		if (factory == null) {
			throw new IllegalStateException(
					"No component factory registered for type@"
							+ Integer.toHexString(System.identityHashCode(componentType)));
		}

		return cast(factory.get());
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object value) {
		return (T) value;
	}
}
