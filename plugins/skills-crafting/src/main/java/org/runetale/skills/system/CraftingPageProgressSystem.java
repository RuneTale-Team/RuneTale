package org.runetale.skills.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.config.CraftingConfig;
import org.runetale.skills.page.SmeltingPage;
import org.runetale.skills.page.SmithingPage;
import org.runetale.skills.service.CraftingPageTrackerService;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ticks custom smithing/smelting page crafting progress animations.
 */
public class CraftingPageProgressSystem extends DelayedSystem<EntityStore> {
	private final CraftingPageTrackerService craftingPageTrackerService;

	public CraftingPageProgressSystem(
			@Nonnull CraftingConfig craftingConfig,
			@Nonnull CraftingPageTrackerService craftingPageTrackerService) {
		super(craftingConfig.pageProgressTickSeconds());
		this.craftingPageTrackerService = craftingPageTrackerService;
	}

	@Override
	public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
		Map<UUID, UUID> trackedPages = this.craftingPageTrackerService.snapshotTrackedPages();
		if (trackedPages.isEmpty()) {
			return;
		}

		World world = store.getExternalData().getWorld();
		UUID worldId = world.getWorldConfig().getUuid();
		Map<UUID, PlayerRef> onlinePlayersById = new HashMap<>();
		for (PlayerRef playerRef : world.getPlayerRefs()) {
			onlinePlayersById.put(playerRef.getUuid(), playerRef);
		}

		for (Map.Entry<UUID, UUID> trackedPage : trackedPages.entrySet()) {
			UUID playerId = trackedPage.getKey();
			UUID trackedWorldId = trackedPage.getValue();
			if (!worldId.equals(trackedWorldId)) {
				continue;
			}

			PlayerRef playerRef = onlinePlayersById.get(playerId);
			if (playerRef == null) {
				this.craftingPageTrackerService.untrackOpenPage(playerId);
				continue;
			}

			Ref<EntityStore> playerEntityRef = playerRef.getReference();
			if (playerEntityRef == null || !playerEntityRef.isValid()) {
				this.craftingPageTrackerService.untrackOpenPage(playerId);
				continue;
			}

			try {
				Player player = store.getComponent(playerEntityRef, Player.getComponentType());
				if (player == null) {
					this.craftingPageTrackerService.untrackOpenPage(playerId);
					continue;
				}

				CustomUIPage customPage = player.getPageManager().getCustomPage();
				if (customPage instanceof SmeltingPage smeltingPage) {
					smeltingPage.tickProgress(playerEntityRef, store, deltaTime);
				} else if (customPage instanceof SmithingPage smithingPage) {
					smithingPage.tickProgress(playerEntityRef, store, deltaTime);
				} else {
					this.craftingPageTrackerService.untrackOpenPage(playerId);
				}
			} catch (IllegalStateException ignored) {
				this.craftingPageTrackerService.untrackOpenPage(playerId);
			}
		}
	}
}
