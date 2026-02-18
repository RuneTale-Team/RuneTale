package org.runetale.skills.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.config.CraftingConfig;
import org.runetale.skills.page.SmeltingPage;
import org.runetale.skills.page.SmithingPage;
import org.runetale.skills.service.CraftingPageTrackerService;

import javax.annotation.Nonnull;
import java.util.Collection;

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
		Collection<Ref<EntityStore>> trackedRefs = this.craftingPageTrackerService.snapshotTrackedRefs();
		for (Ref<EntityStore> playerEntityRef : trackedRefs) {
			if (playerEntityRef == null || !playerEntityRef.isValid()) {
				continue;
			}

			PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
			if (playerRef == null) {
				continue;
			}

			Player player = store.getComponent(playerEntityRef, Player.getComponentType());
			if (player == null) {
				this.craftingPageTrackerService.untrackOpenPage(playerRef.getUuid());
				continue;
			}

			CustomUIPage customPage = player.getPageManager().getCustomPage();
			if (customPage instanceof SmeltingPage smeltingPage) {
				smeltingPage.tickProgress(playerEntityRef, store, deltaTime);
			} else if (customPage instanceof SmithingPage smithingPage) {
				smithingPage.tickProgress(playerEntityRef, store, deltaTime);
			} else {
				this.craftingPageTrackerService.untrackOpenPage(playerRef.getUuid());
			}
		}
	}
}
