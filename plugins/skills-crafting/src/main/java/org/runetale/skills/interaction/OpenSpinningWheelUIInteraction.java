package org.runetale.skills.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.crafting.CraftingRuntimeRegistry;
import org.runetale.skills.crafting.CraftingRuntimeState;
import org.runetale.skills.page.FletchingPage;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Block interaction that opens the Spinning Wheel fletching UI page.
 *
 * <p>
 * Registered as {@code "runetale_open_spinning_wheel"}. Triggered by pressing F on the spinning wheel block.
 */
public final class OpenSpinningWheelUIInteraction extends SimpleInstantInteraction {

	public static final String TYPE_NAME = "runetale_open_spinning_wheel";

	public static final BuilderCodec<OpenSpinningWheelUIInteraction> CODEC = BuilderCodec
			.builder(OpenSpinningWheelUIInteraction.class, OpenSpinningWheelUIInteraction::new, SimpleInstantInteraction.CODEC)
			.build();

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	@Override
	protected void firstRun(
			@Nonnull InteractionType interactionType,
			@Nonnull InteractionContext ctx,
			@Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = ctx.getCommandBuffer();
		if (commandBuffer == null) {
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		Player player = (Player) commandBuffer.getComponent(ctx.getEntity(), Player.getComponentType());
		if (player == null) {
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		PlayerRef playerRef = commandBuffer.getComponent(ctx.getEntity(), PlayerRef.getComponentType());
		if (playerRef == null) {
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		BlockPosition targetBlock = ctx.getTargetBlock();
		if (targetBlock == null) {
			LOGGER.atWarning().log("No target block for spinning wheel interaction");
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
		CraftingRuntimeState craftingRuntime = CraftingRuntimeRegistry.get();
		if (runtimeApi == null || craftingRuntime == null) {
			LOGGER.atWarning().log("Skills runtime not available; cannot open spinning wheel UI");
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		Ref<EntityStore> ref = player.getReference();
		Store<EntityStore> store = ref.getStore();

		FletchingPage page = new FletchingPage(
				playerRef,
				targetBlock,
				runtimeApi,
				craftingRuntime.craftingRecipeTagService(),
				craftingRuntime.craftingPageTrackerService(),
				craftingRuntime.craftingConfig(),
				"Spinning Wheel",
				craftingRuntime.craftingConfig().spinningWheelBenchId(),
				List.of("RuneTale_Spinning_Wheel"));

		player.getPageManager().openCustomPage(ref, store, page);
		ctx.getState().state = InteractionState.Finished;
	}
}
