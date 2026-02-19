package org.runetale.skills.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.service.CombatStyleService;
import org.runetale.skills.service.SkillSessionStatsService;
import org.runetale.skills.service.SkillXpToastHudService;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Cleans up session-scoped player state when a player entity is removed.
 */
public class PlayerSessionCleanupSystem extends RefSystem<EntityStore> {

	private final CombatStyleService combatStyleService;
	private final SkillSessionStatsService skillSessionStatsService;
	private final SkillXpToastHudService skillXpToastHudService;
	private final Query<EntityStore> query;

	public PlayerSessionCleanupSystem(
			@Nonnull CombatStyleService combatStyleService,
			@Nonnull SkillSessionStatsService skillSessionStatsService,
			@Nonnull SkillXpToastHudService skillXpToastHudService) {
		this.combatStyleService = combatStyleService;
		this.skillSessionStatsService = skillSessionStatsService;
		this.skillXpToastHudService = skillXpToastHudService;
		this.query = Query.and(PlayerRef.getComponentType());
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	@Override
	public void onEntityAdded(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull AddReason reason,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer) {
		// No setup required.
	}

	@Override
	public void onEntityRemove(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull RemoveReason reason,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer) {
		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
		if (playerRef == null) {
			playerRef = store.getComponent(ref, PlayerRef.getComponentType());
		}
		if (playerRef == null) {
			return;
		}

		UUID playerId = playerRef.getUuid();
		this.combatStyleService.clear(playerId);
		this.skillSessionStatsService.clear(playerId);
		this.skillXpToastHudService.clear(playerId);
	}
}
