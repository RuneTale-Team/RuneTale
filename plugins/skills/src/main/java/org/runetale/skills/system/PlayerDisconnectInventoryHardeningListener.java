package org.runetale.skills.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerDisconnectInventoryHardeningListener {
	private final InventoryLookup inventoryLookup;

	public PlayerDisconnectInventoryHardeningListener() {
		this(PlayerDisconnectInventoryHardeningListener::lookupInventory);
	}

	PlayerDisconnectInventoryHardeningListener(@Nonnull InventoryLookup inventoryLookup) {
		this.inventoryLookup = inventoryLookup;
	}

	public void handle(@Nonnull PlayerDisconnectEvent event) {
		PlayerRef playerRef = event.getPlayerRef();
		Ref<EntityStore> ref = playerRef.getReference();
		if (ref == null || !ref.isValid()) {
			return;
		}

		Store<EntityStore> store = ref.getStore();
		EntityStore entityStore = store.getExternalData();
		if (entityStore == null) {
			return;
		}

		World world = entityStore.getWorld();
		if (world == null) {
			return;
		}

		Runnable hardenTask = () -> detachInventoryListeners(ref, store);
		if (world.isInThread()) {
			hardenTask.run();
			return;
		}

		world.execute(hardenTask);
	}

	private void detachInventoryListeners(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		if (!ref.isValid()) {
			return;
		}

		Inventory inventory = this.inventoryLookup.resolve(ref, store);
		if (inventory == null) {
			return;
		}

		inventory.unregister();
	}

	@Nullable
	private static Inventory lookupInventory(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return null;
		}

		return player.getInventory();
	}

	@FunctionalInterface
	interface InventoryLookup {
		@Nullable
		Inventory resolve(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store);
	}
}
