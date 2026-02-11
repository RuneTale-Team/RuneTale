package org.runetale.testing.ecs;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.event.EntityEventType;
import com.hypixel.hytale.component.event.WorldEventType;
import com.hypixel.hytale.component.system.EcsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal fake {@link ComponentAccessor} that records dispatched events.
 */
public class RecordingComponentAccessor<ECS_TYPE> implements ComponentAccessor<ECS_TYPE> {

	private final List<EntityInvocation<ECS_TYPE>> entityInvocations = new ArrayList<>();
	private final List<EcsEvent> worldInvocations = new ArrayList<>();
	private final ECS_TYPE externalData;

	public RecordingComponentAccessor(ECS_TYPE externalData) {
		this.externalData = externalData;
	}

	public List<EntityInvocation<ECS_TYPE>> getEntityInvocations() {
		return Collections.unmodifiableList(entityInvocations);
	}

	public List<EcsEvent> getWorldInvocations() {
		return Collections.unmodifiableList(worldInvocations);
	}

	public void clearInvocations() {
		entityInvocations.clear();
		worldInvocations.clear();
	}

	@Override
	public ECS_TYPE getExternalData() {
		return externalData;
	}

	@Override
	public <Event extends EcsEvent> void invoke(Ref<ECS_TYPE> ref, Event event) {
		entityInvocations.add(new EntityInvocation<>(ref, event));
	}

	@Override
	public <Event extends EcsEvent> void invoke(EntityEventType<ECS_TYPE, Event> type, Ref<ECS_TYPE> ref, Event event) {
		entityInvocations.add(new EntityInvocation<>(ref, event));
	}

	@Override
	public <Event extends EcsEvent> void invoke(Event event) {
		worldInvocations.add(event);
	}

	@Override
	public <Event extends EcsEvent> void invoke(WorldEventType<ECS_TYPE, Event> type, Event event) {
		worldInvocations.add(event);
	}

	@Override
	public <T extends Component<ECS_TYPE>> T getComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType) {
		throw unsupported();
	}

	@Override
	public <T extends Component<ECS_TYPE>> T ensureAndGetComponent(Ref<ECS_TYPE> ref,
			ComponentType<ECS_TYPE, T> componentType) {
		throw unsupported();
	}

	@Override
	public Archetype<ECS_TYPE> getArchetype(Ref<ECS_TYPE> ref) {
		throw unsupported();
	}

	@Override
	public <T extends Resource<ECS_TYPE>> T getResource(ResourceType<ECS_TYPE, T> resourceType) {
		throw unsupported();
	}

	@Override
	public <T extends Component<ECS_TYPE>> void putComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType,
			T component) {
		throw unsupported();
	}

	@Override
	public <T extends Component<ECS_TYPE>> void addComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType,
			T component) {
		throw unsupported();
	}

	@Override
	public <T extends Component<ECS_TYPE>> T addComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, T> componentType) {
		throw unsupported();
	}

	@Override
	public Ref<ECS_TYPE>[] addEntities(Holder<ECS_TYPE>[] holders, AddReason reason) {
		throw unsupported();
	}

	@Override
	public Ref<ECS_TYPE> addEntity(Holder<ECS_TYPE> holder, AddReason reason) {
		throw unsupported();
	}

	@Override
	public Holder<ECS_TYPE> removeEntity(Ref<ECS_TYPE> ref, Holder<ECS_TYPE> holder, RemoveReason reason) {
		throw unsupported();
	}

	@Override
	public <T extends Component<ECS_TYPE>> void removeComponent(Ref<ECS_TYPE> ref,
			ComponentType<ECS_TYPE, T> componentType) {
		throw unsupported();
	}

	@Override
	public <T extends Component<ECS_TYPE>> void tryRemoveComponent(Ref<ECS_TYPE> ref,
			ComponentType<ECS_TYPE, T> componentType) {
		throw unsupported();
	}

	private static UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException("Not implemented in RecordingComponentAccessor");
	}

	public record EntityInvocation<ECS_TYPE>(Ref<ECS_TYPE> ref, EcsEvent event) {
	}
}
