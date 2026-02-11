package org.runetale.skills.service;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MaterialTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.CraftResult;
import org.runetale.skills.domain.SkillRequirement;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Stateless service that executes manual crafting through the custom
 * smelting/smithing UI, bypassing the native bench crafting window.
 *
 * <p>
 * Validates skill requirements, atomically removes input materials,
 * and delivers output items to the player's inventory (or drops them).
 */
public class CustomCraftingService {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	/**
	 * Attempts to craft a single unit of the given recipe.
	 *
	 * @param store   entity store for component access
	 * @param ref     player entity reference
	 * @param recipe  the crafting recipe to execute
	 * @param profile the player's skill profile (for level checks)
	 * @param requirements skill requirements extracted from recipe tags
	 * @return result indicating success or the specific failure reason
	 */
	@Nonnull
	public CraftResult tryCraft(
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull CraftingRecipe recipe,
			@Nonnull PlayerSkillProfileComponent profile,
			@Nonnull List<SkillRequirement> requirements) {

		// 1. Check skill level requirements
		for (SkillRequirement req : requirements) {
			int playerLevel = profile.getLevel(req.skillType());
			if (playerLevel < req.requiredLevel()) {
				LOGGER.atFine().log("Craft rejected: %s requires %s level %d, player has %d",
						recipe.getId(), req.skillType(), req.requiredLevel(), playerLevel);
				return CraftResult.LEVEL_TOO_LOW;
			}
		}

		// 2. Get player inventory
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			LOGGER.atWarning().log("Cannot craft: Player component not found on entity");
			return CraftResult.MISSING_MATERIALS;
		}

		Inventory inventory = player.getInventory();

		// 3. Atomically remove input materials (all-or-nothing)
		List<MaterialQuantity> inputs = CraftingManager.getInputMaterials(recipe);
		if (!inputs.isEmpty()) {
			ListTransaction<MaterialTransaction> transaction =
					inventory.getCombinedBackpackStorageHotbar().removeMaterials(inputs, true, true, true);
			if (!transaction.succeeded()) {
				LOGGER.atFine().log("Craft rejected: missing materials for %s", recipe.getId());
				return CraftResult.MISSING_MATERIALS;
			}
		}

		// 4. Deliver output items (add to inventory or drop on ground)
		List<ItemStack> outputs = CraftingManager.getOutputItemStacks(recipe);
		if (!outputs.isEmpty()) {
			SimpleItemContainer.addOrDropItemStacks(store, ref, inventory.getCombinedArmorHotbarStorage(), outputs);
		}

		LOGGER.atFine().log("Craft succeeded: %s", recipe.getId());
		return CraftResult.SUCCESS;
	}
}
