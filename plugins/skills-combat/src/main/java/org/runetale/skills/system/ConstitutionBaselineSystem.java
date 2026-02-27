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
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.CombatConfig;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

public class ConstitutionBaselineSystem extends RefSystem<EntityStore> {

	private final SkillsRuntimeApi runtimeApi;
	private final CombatConfig combatConfig;
	private final Query<EntityStore> query;

	public ConstitutionBaselineSystem(
			@Nonnull SkillsRuntimeApi runtimeApi,
			@Nonnull CombatConfig combatConfig) {
		this.runtimeApi = runtimeApi;
		this.combatConfig = combatConfig;
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
		long targetXp = this.runtimeApi.xpForLevel(this.combatConfig.constitutionBaseLevel());
		if (targetXp <= 0L) {
			return;
		}

		long currentXp = this.runtimeApi.getSkillExperience(commandBuffer, ref, SkillType.CONSTITUTION);
		long missingXp = targetXp - currentXp;
		if (missingXp <= 0L) {
			return;
		}

		this.runtimeApi.grantSkillXp(
				commandBuffer,
				ref,
				SkillType.CONSTITUTION,
				missingXp,
				this.combatConfig.sourceConstitutionBaseline(),
				false);
	}

	@Override
	public void onEntityRemove(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull RemoveReason reason,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer) {
	}
}
