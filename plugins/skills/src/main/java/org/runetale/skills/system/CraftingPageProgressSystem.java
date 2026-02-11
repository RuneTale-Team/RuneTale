package org.runetale.skills.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.page.SmeltingPage;
import org.runetale.skills.page.SmithingPage;

import javax.annotation.Nonnull;

/**
 * Ticks custom smithing/smelting page crafting progress animations.
 */
public class CraftingPageProgressSystem extends DelayedSystem<EntityStore> {

	public CraftingPageProgressSystem() {
		super(0.05F);
	}

	@Override
	public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
		World world = store.getExternalData().getWorld();
		for (PlayerRef playerRef : world.getPlayerRefs()) {
			if (playerRef == null) {
				continue;
			}

			if (playerRef.getReference() == null || !playerRef.getReference().isValid()) {
				continue;
			}

			Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
			if (player == null) {
				continue;
			}

			CustomUIPage customPage = player.getPageManager().getCustomPage();
			if (customPage instanceof SmeltingPage smeltingPage) {
				smeltingPage.tickProgress(playerRef.getReference(), store, deltaTime);
			} else if (customPage instanceof SmithingPage smithingPage) {
				smithingPage.tickProgress(playerRef.getReference(), store, deltaTime);
			}
		}
	}
}
