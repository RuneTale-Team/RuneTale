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
import org.runetale.skills.service.ConstitutionHealthService;

import javax.annotation.Nonnull;

public class ConstitutionHealthSyncOnJoinSystem extends RefSystem<EntityStore> {

	private final ConstitutionHealthService constitutionHealthService;
	private final Query<EntityStore> query;

	public ConstitutionHealthSyncOnJoinSystem(@Nonnull ConstitutionHealthService constitutionHealthService) {
		this.constitutionHealthService = constitutionHealthService;
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
		this.constitutionHealthService.syncForPlayer(commandBuffer, ref, false);
	}

	@Override
	public void onEntityRemove(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull RemoveReason reason,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer) {
	}
}
