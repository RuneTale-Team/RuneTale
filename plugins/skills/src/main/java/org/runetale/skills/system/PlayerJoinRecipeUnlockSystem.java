package org.runetale.skills.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Syncs crafting recipe unlocks when a player joins the server.
 *
 * <p>
 * Reads the player's current skill levels and unlocks any recipes whose
 * requirements are already met. This catches up players who leveled up
 * while the unlock system wasn't running or who had their levels changed
 * externally.
 */
public class PlayerJoinRecipeUnlockSystem extends RefSystem<EntityStore> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;
	private final Query<EntityStore> query;

	public PlayerJoinRecipeUnlockSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		this.profileComponentType = profileComponentType;
		this.craftingRecipeTagService = craftingRecipeTagService;
		this.query = Query.and(PlayerRef.getComponentType(), profileComponentType);
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	@Override
	public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
			@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

		PlayerSkillProfileComponent profile = commandBuffer.getComponent(ref, this.profileComponentType);
		if (profile == null) {
			LOGGER.atWarning().log("Player skill profile missing during join recipe sync; skipping.");
			return;
		}

		int unlocked = 0;
		for (CraftingRecipe recipe : CraftingRecipe.getAssetMap().getAssetMap().values()) {
			Optional<SkillType> skillOpt = this.craftingRecipeTagService.getSkillRequired(recipe);
			if (skillOpt.isEmpty()) {
				continue;
			}

			SkillType skill = skillOpt.get();
			int requiredLevel = this.craftingRecipeTagService.getCraftingLevelRequired(recipe);
			int playerLevel = profile.getLevel(skill);
			if (playerLevel < requiredLevel) {
				continue;
			}

			if (recipe.getPrimaryOutput() == null) {
				continue;
			}

			String itemId = recipe.getPrimaryOutput().getItemId();
			if (CraftingPlugin.learnRecipe(ref, itemId, commandBuffer)) {
				unlocked++;
			}
		}

		if (unlocked > 0) {
			LOGGER.atFine().log("Synced %d recipe unlocks on player join", unlocked);
		}
	}

	@Override
	public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
			@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		// No cleanup required.
	}
}
