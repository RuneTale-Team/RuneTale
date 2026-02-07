package org.runetale.skills.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.service.SkillXpToastHudService;

import javax.annotation.Nonnull;

/**
 * Removes expired custom XP toasts from player HUDs.
 */
public class SkillXpToastHudExpirySystem extends DelayedSystem<EntityStore> {

	private final SkillXpToastHudService skillXpToastHudService;

	public SkillXpToastHudExpirySystem(@Nonnull SkillXpToastHudService skillXpToastHudService) {
		super(0.1F);
		this.skillXpToastHudService = skillXpToastHudService;
	}

	@Override
	public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
		World world = store.getExternalData().getWorld();
		long nowMillis = System.currentTimeMillis();
		for (PlayerRef playerRef : world.getPlayerRefs()) {
			this.skillXpToastHudService.expireIfDue(playerRef, nowMillis);
		}
	}
}
