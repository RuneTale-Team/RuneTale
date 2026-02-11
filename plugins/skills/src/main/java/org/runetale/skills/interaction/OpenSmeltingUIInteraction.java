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
import org.runetale.skills.SkillsPlugin;
import org.runetale.skills.page.SmeltingPage;

import javax.annotation.Nonnull;

/**
 * Block interaction that opens the custom smelting UI page.
 *
 * <p>
 * Registered with {@link com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction#CODEC}
 * as {@code "runetale_open_smelting"}. Follows the {@link SimpleInstantInteraction} pattern
 * to avoid the codec loading-order issue with {@code OpenCustomUIInteraction.PAGE_CODEC}.
 */
public final class OpenSmeltingUIInteraction extends SimpleInstantInteraction {

	public static final String TYPE_NAME = "runetale_open_smelting";

	public static final BuilderCodec<OpenSmeltingUIInteraction> CODEC = BuilderCodec
			.builder(OpenSmeltingUIInteraction.class, OpenSmeltingUIInteraction::new, SimpleInstantInteraction.CODEC)
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
			LOGGER.atWarning().log("No target block for smelting interaction");
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		SkillsPlugin plugin = SkillsPlugin.getInstance();
		if (plugin == null) {
			LOGGER.atWarning().log("SkillsPlugin not available; cannot open smelting UI");
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		Ref<EntityStore> ref = player.getReference();
		Store<EntityStore> store = ref.getStore();

		SmeltingPage page = new SmeltingPage(
				playerRef,
				targetBlock,
				plugin.getPlayerSkillProfileComponentType(),
				plugin.getCraftingRecipeTagService());

		player.getPageManager().openCustomPage(ref, store, page);
		ctx.getState().state = InteractionState.Finished;
	}
}
