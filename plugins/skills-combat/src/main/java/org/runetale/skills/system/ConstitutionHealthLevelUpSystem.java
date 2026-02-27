package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.event.SkillLevelUpEvent;
import org.runetale.skills.service.ConstitutionHealthService;

import javax.annotation.Nonnull;

public class ConstitutionHealthLevelUpSystem extends EntityEventSystem<EntityStore, SkillLevelUpEvent> {

	private final ConstitutionHealthService constitutionHealthService;
	private final Query<EntityStore> query;

	public ConstitutionHealthLevelUpSystem(@Nonnull ConstitutionHealthService constitutionHealthService) {
		super(SkillLevelUpEvent.class);
		this.constitutionHealthService = constitutionHealthService;
		this.query = Query.and(PlayerRef.getComponentType());
	}

	@Override
	public void handle(
			int index,
			@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer,
			@Nonnull SkillLevelUpEvent event) {
		if (event.getSkillType() != SkillType.CONSTITUTION) {
			return;
		}

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		this.constitutionHealthService.syncForLevel(commandBuffer, ref, event.getNewLevel(), true);
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}
}
