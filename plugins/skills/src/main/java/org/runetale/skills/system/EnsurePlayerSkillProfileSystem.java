package org.runetale.skills.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;

import javax.annotation.Nonnull;

/**
 * Ensures each player entity always has a persistent skill profile component.
 *
 * <p>
 * This system runs before break-block processing systems, so profile reads in
 * gameplay systems can assume the component exists.
 */
public class EnsurePlayerSkillProfileSystem extends HolderSystem<EntityStore> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final Query<EntityStore> query;

	public EnsurePlayerSkillProfileSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType) {
		this.profileComponentType = profileComponentType;
		this.query = Query.and(PlayerRef.getComponentType(), Query.not(profileComponentType));
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	@Override
	public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason,
			@Nonnull Store<EntityStore> store) {
		holder.ensureComponent(this.profileComponentType);
		LOGGER.atFiner().log("Added missing player skill profile (reason=%s)", reason);
	}

	@Override
	public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason,
			@Nonnull Store<EntityStore> store) {
		// No cleanup required. Component lifecycle is handled by ECS persistence.
	}
}
