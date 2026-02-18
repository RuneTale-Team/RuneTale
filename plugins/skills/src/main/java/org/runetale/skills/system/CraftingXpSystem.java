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
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Grants skill XP when a player successfully crafts a recipe tagged with
 * skill XP rewards. XP is granted to all skills listed in the recipe's
 * {@code SkillsRequired} tag.
 */
public class CraftingXpSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final SkillXpDispatchService skillXpDispatchService;
	private final CraftingRecipeTagService craftingRecipeTagService;
	private final Query<EntityStore> query;

	public CraftingXpSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull SkillXpDispatchService skillXpDispatchService,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		super(CraftRecipeEvent.Post.class);
		this.skillXpDispatchService = skillXpDispatchService;
		this.craftingRecipeTagService = craftingRecipeTagService;
		this.query = Query.and(PlayerRef.getComponentType(), profileComponentType);
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull CraftRecipeEvent.Post event) {

		CraftingRecipe recipe = event.getCraftedRecipe();
		if (recipe == null) {
			return;
		}

		List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(recipe);
		if (requirements.isEmpty()) {
			return;
		}

		Optional<Double> xpOpt = this.craftingRecipeTagService.getXpOnSuccessfulCraft(recipe);
		if (xpOpt.isEmpty()) {
			return;
		}

		double totalXp = xpOpt.get() * event.getQuantity();
		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

		for (SkillRequirement req : requirements) {
			String source = "craft:" + req.skillType().name().toLowerCase(Locale.ROOT) + ":" + recipe.getId();
			this.skillXpDispatchService.grantSkillXp(commandBuffer, ref, req.skillType(), totalXp, source, true);
			LOGGER.atFine().log("Granted %.1f %s XP for crafting %s (qty=%d)",
					totalXp, req.skillType(), recipe.getId(), event.getQuantity());
		}
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}
}
