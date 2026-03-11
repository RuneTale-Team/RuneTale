package org.runetale.skills.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
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
 * Item interaction that opens the Arrow Shafts fletching UI page.
 *
 * <p>
 * Registered as {@code "runetale_open_arrow_shaft"}. Triggered by right-clicking while holding an arrow shaft.
 */
public final class OpenArrowShaftUIInteraction extends SimpleInstantInteraction {

	public static final String TYPE_NAME = "runetale_open_arrow_shaft";

	public static final BuilderCodec<OpenArrowShaftUIInteraction> CODEC = BuilderCodec
			.builder(OpenArrowShaftUIInteraction.class, OpenArrowShaftUIInteraction::new, SimpleInstantInteraction.CODEC)
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

		SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
		CraftingRuntimeState craftingRuntime = CraftingRuntimeRegistry.get();
		if (runtimeApi == null || craftingRuntime == null) {
			LOGGER.atWarning().log("Skills runtime not available; cannot open arrow shaft UI");
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		Ref<EntityStore> ref = player.getReference();
		Store<EntityStore> store = ref.getStore();

		FletchingPage page = new FletchingPage(
				playerRef,
				null,
				runtimeApi,
				craftingRuntime.craftingRecipeTagService(),
				craftingRuntime.craftingPageTrackerService(),
				craftingRuntime.craftingConfig(),
				"Arrow Shafts",
				craftingRuntime.craftingConfig().fletchingBenchId(),
				List.of("RuneTale_Fletching_Feathering"));

		player.getPageManager().openCustomPage(ref, store, page);
		ctx.getState().state = InteractionState.Finished;
	}
}
