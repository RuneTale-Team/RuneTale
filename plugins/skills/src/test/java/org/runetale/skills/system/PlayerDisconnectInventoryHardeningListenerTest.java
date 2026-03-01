package org.runetale.skills.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerDisconnectInventoryHardeningListenerTest {

	@Test
	void handleSkipsWhenPlayerReferenceMissingOrInvalid() {
		PlayerDisconnectInventoryHardeningListener listener = new PlayerDisconnectInventoryHardeningListener();
		PlayerRef playerRef = mock(PlayerRef.class);
		PlayerDisconnectEvent event = new PlayerDisconnectEvent(playerRef);

		listener.handle(event);

		verify(playerRef).getReference();

		@SuppressWarnings("unchecked")
		Ref<EntityStore> ref = (Ref<EntityStore>) mock(Ref.class);
		when(playerRef.getReference()).thenReturn(ref);
		when(ref.isValid()).thenReturn(false);

		listener.handle(event);

		verify(ref).isValid();
		verify(ref, never()).getStore();
	}

	@Test
	void handleUnregistersInventoryImmediatelyWhenAlreadyOnWorldThread() {
		Inventory inventory = mock(Inventory.class);
		PlayerDisconnectInventoryHardeningListener listener = new PlayerDisconnectInventoryHardeningListener((ref, store) -> inventory);
		PlayerRef playerRef = mock(PlayerRef.class);
		PlayerDisconnectEvent event = new PlayerDisconnectEvent(playerRef);
		@SuppressWarnings("unchecked")
		Ref<EntityStore> ref = (Ref<EntityStore>) mock(Ref.class);
		@SuppressWarnings("unchecked")
		Store<EntityStore> store = (Store<EntityStore>) mock(Store.class);
		EntityStore entityStore = mock(EntityStore.class);
		World world = mock(World.class);

		when(playerRef.getReference()).thenReturn(ref);
		when(ref.isValid()).thenReturn(true);
		when(ref.getStore()).thenReturn(store);
		when(store.getExternalData()).thenReturn(entityStore);
		when(entityStore.getWorld()).thenReturn(world);
		when(world.isInThread()).thenReturn(true);

		listener.handle(event);

		verify(inventory).unregister();
		verify(world, never()).execute(any(Runnable.class));
	}

	@Test
	void handleSchedulesHardeningWhenOffWorldThread() {
		Inventory inventory = mock(Inventory.class);
		PlayerDisconnectInventoryHardeningListener listener = new PlayerDisconnectInventoryHardeningListener((ref, store) -> inventory);
		PlayerRef playerRef = mock(PlayerRef.class);
		PlayerDisconnectEvent event = new PlayerDisconnectEvent(playerRef);
		@SuppressWarnings("unchecked")
		Ref<EntityStore> ref = (Ref<EntityStore>) mock(Ref.class);
		@SuppressWarnings("unchecked")
		Store<EntityStore> store = (Store<EntityStore>) mock(Store.class);
		EntityStore entityStore = mock(EntityStore.class);
		World world = mock(World.class);

		when(playerRef.getReference()).thenReturn(ref);
		when(ref.isValid()).thenReturn(true);
		when(ref.getStore()).thenReturn(store);
		when(store.getExternalData()).thenReturn(entityStore);
		when(entityStore.getWorld()).thenReturn(world);
		when(world.isInThread()).thenReturn(false);

		listener.handle(event);

		ArgumentCaptor<Runnable> hardenTask = ArgumentCaptor.forClass(Runnable.class);
		verify(world).execute(hardenTask.capture());
		verify(inventory, never()).unregister();

		hardenTask.getValue().run();
		verify(inventory).unregister();
	}
}
