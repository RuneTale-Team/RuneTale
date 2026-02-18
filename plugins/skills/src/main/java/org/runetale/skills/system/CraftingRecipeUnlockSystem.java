package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.progression.event.SkillLevelUpEvent;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Unlocks crafting recipes when a player levels up in a skill.
 *
 * <p>
 * Iterates all loaded recipes and teaches the player any recipe whose
 * {@code SkillsRequired} includes the leveled skill and whose full set of
 * skill-level requirements are met by the player's current profile.
 */
public class CraftingRecipeUnlockSystem extends EntityEventSystem<EntityStore, SkillLevelUpEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;
	private final Query<EntityStore> query;

	public CraftingRecipeUnlockSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		super(SkillLevelUpEvent.class);
		this.profileComponentType = profileComponentType;
		this.craftingRecipeTagService = craftingRecipeTagService;
		this.query = Query.and(PlayerRef.getComponentType(), profileComponentType);
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull SkillLevelUpEvent event) {

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

		PlayerSkillProfileComponent profile = commandBuffer.getComponent(ref, this.profileComponentType);
		if (profile == null) {
			LOGGER.atWarning().log("Player skill profile missing during recipe unlock; skipping.");
			return;
		}

		for (CraftingRecipe recipe : CraftingRecipe.getAssetMap().getAssetMap().values()) {
			List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(recipe);
			if (requirements.isEmpty()) {
				continue;
			}

			boolean relevantToThisLevelUp = false;
			boolean allMet = true;
			for (SkillRequirement req : requirements) {
				if (req.skillType() == event.getSkillType()) {
					relevantToThisLevelUp = true;
					if (event.getNewLevel() < req.requiredLevel()) {
						allMet = false;
						break;
					}
				} else {
					if (profile.getLevel(req.skillType()) < req.requiredLevel()) {
						allMet = false;
						break;
					}
				}
			}

			if (!relevantToThisLevelUp || !allMet) {
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
