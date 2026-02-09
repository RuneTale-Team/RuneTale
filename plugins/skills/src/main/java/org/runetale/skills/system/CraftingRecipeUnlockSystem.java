package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.event.SkillLevelUpEvent;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Unlocks crafting recipes when a player levels up in a skill.
 *
 * <p>
 * Iterates all loaded recipes and teaches the player any recipe whose
 * {@code SkillRequired} matches the leveled skill and whose
 * {@code CraftingLevelRequired} is at or below the new level.
 */
public class CraftingRecipeUnlockSystem extends EntityEventSystem<EntityStore, SkillLevelUpEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final CraftingRecipeTagService craftingRecipeTagService;
	private final Query<EntityStore> query;

	public CraftingRecipeUnlockSystem(
			@Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		super(SkillLevelUpEvent.class);
		this.craftingRecipeTagService = craftingRecipeTagService;
		this.query = Query.and(PlayerRef.getComponentType());
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull SkillLevelUpEvent event) {

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

		for (CraftingRecipe recipe : CraftingRecipe.getAssetMap().getAssetMap().values()) {
			Optional<SkillType> skillOpt = this.craftingRecipeTagService.getSkillRequired(recipe);
			if (skillOpt.isEmpty() || skillOpt.get() != event.getSkillType()) {
				continue;
			}

			int requiredLevel = this.craftingRecipeTagService.getCraftingLevelRequired(recipe);
			if (requiredLevel > event.getNewLevel()) {
				continue;
			}

			if (recipe.getPrimaryOutput() == null) {
				continue;
			}

			String itemId = recipe.getPrimaryOutput().getItemId();
			boolean learned = CraftingPlugin.learnRecipe(ref, itemId, commandBuffer);
			if (learned) {
				LOGGER.atFine().log("Unlocked recipe %s (item=%s) for %s level %d",
						recipe.getId(), itemId, event.getSkillType(), event.getNewLevel());
			}
		}
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}
}
