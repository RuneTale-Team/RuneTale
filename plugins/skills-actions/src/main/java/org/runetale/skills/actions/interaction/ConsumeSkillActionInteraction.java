package org.runetale.skills.actions.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.actions.ItemActionsRuntimeRegistry;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.ItemActionsConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public final class ConsumeSkillActionInteraction extends SimpleInstantInteraction {

    public static final String TYPE_NAME = "runetale_consume_skill_action";

    public static final BuilderCodec<ConsumeSkillActionInteraction> CODEC = BuilderCodec
            .builder(ConsumeSkillActionInteraction.class, ConsumeSkillActionInteraction::new, SimpleInstantInteraction.CODEC)
            .build();

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void firstRun(
            @Nonnull InteractionType interactionType,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> playerRef = context.getEntity();
        if (!playerRef.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Player player = (Player) commandBuffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        ItemActionsConfig itemActionsConfig = ItemActionsRuntimeRegistry.get();
        if (runtimeApi == null || itemActionsConfig == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        ItemContainer heldItemContainer = context.getHeldItemContainer();
        short heldItemSlot = context.getHeldItemSlot();
        ItemStack heldItem = context.getHeldItem();
        if (heldItemContainer == null || heldItemSlot < 0 || heldItem == null || ItemStack.isEmpty(heldItem)) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Store<EntityStore> store = commandBuffer.getStore();
        String targetBlockId = resolveTargetBlockId(context, store);
        ItemActionsConfig.ItemXpActionDefinition action = matchAction(itemActionsConfig, interactionType, player, heldItem, targetBlockId);
        if (action == null) {
            context.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("[Skills Actions] No action match interaction=%s item=%s targetBlock=%s", interactionType, heldItem.getItemId(), targetBlockId);
            debugLog(runtimeApi, itemActionsConfig, "No configured action matched item=%s interactionType=%s targetBlock=%s", heldItem.getItemId(), interactionType, targetBlockId);
            return;
        }

        if (!runtimeApi.hasSkillProfile(commandBuffer, playerRef)) {
            context.getState().state = InteractionState.Failed;
            debugLog(runtimeApi, itemActionsConfig, "Skipped action id=%s due to missing profile player=%s", action.id(), playerRef);
            return;
        }

        ItemStackSlotTransaction consumeTransaction = heldItemContainer.removeItemStackFromSlot(
                heldItemSlot,
                heldItem,
                action.consumeQuantity(),
                true,
                true);
        if (!consumeTransaction.succeeded()) {
            context.getState().state = InteractionState.Failed;
            debugLog(runtimeApi, itemActionsConfig, "Failed consume action id=%s slot=%d item=%s qty=%d", action.id(), heldItemSlot, heldItem.getItemId(), action.consumeQuantity());
            return;
        }

        ItemStack updated = heldItemContainer.getItemStack(heldItemSlot);
        context.setHeldItem(ItemStack.isEmpty(updated) ? null : updated);

        boolean granted;
        try {
            granted = runtimeApi.grantSkillXp(
                    commandBuffer,
                    playerRef,
                    action.skillType(),
                    action.experience(),
                    action.source(),
                    action.notifyPlayer());
        } catch (RuntimeException exception) {
            context.getState().state = InteractionState.Failed;
            LOGGER.atWarning().withCause(exception).log(
                    "[Skills Actions] XP dispatch threw for action=%s player=%s skill=%s",
                    action.id(),
                    playerRef,
                    action.skillType());
            return;
        }
        if (!granted) {
            context.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("[Skills Actions] XP dispatch failed after consume action=%s player=%s skill=%s", action.id(), playerRef, action.skillType());
            return;
        }

        context.getState().state = InteractionState.Finished;
        LOGGER.atInfo().log("[Skills Actions] Applied action=%s interaction=%s item=%s qty=%d", action.id(), interactionType, heldItem.getItemId(), action.consumeQuantity());
        debugLog(runtimeApi, itemActionsConfig, "Applied action id=%s item=%s qty=%d skill=%s xp=%.4f interaction=%s", action.id(), heldItem.getItemId(), action.consumeQuantity(), action.skillType(), action.experience(), interactionType);
    }

    @Nullable
    private static ItemActionsConfig.ItemXpActionDefinition matchAction(
            @Nonnull ItemActionsConfig config,
            @Nonnull InteractionType interactionType,
            @Nonnull Player player,
            @Nonnull ItemStack heldItem,
            @Nullable String targetBlockId) {
        for (ItemActionsConfig.ItemXpActionDefinition action : config.actions()) {
            if (!action.enabled()) {
                continue;
            }
            if (!action.matchesInteractionType(interactionType)) {
                continue;
            }
            if (!action.matchesItemId(heldItem.getItemId())) {
                continue;
            }
            if (!action.matchesTargetBlockId(targetBlockId)) {
                continue;
            }
            if (heldItem.getQuantity() < action.consumeQuantity()) {
                continue;
            }
            if (!action.allowCreative() && player.getGameMode() == GameMode.Creative) {
                continue;
            }
            return action;
        }
        return null;
    }

    @Nullable
    private static String resolveTargetBlockId(
            @Nonnull InteractionContext context,
            @Nonnull Store<EntityStore> store) {
        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock == null) {
            return null;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return null;
        }

        BlockType blockType = world.getBlockType(targetBlock.x, targetBlock.y, targetBlock.z);
        if (blockType == null) {
            return null;
        }

        return blockType.getId();
    }

    private static void debugLog(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull ItemActionsConfig config,
            @Nonnull String message,
            Object... args) {
        if (runtimeApi.isDebugEnabled(config.debugPluginKey())) {
            LOGGER.atInfo().log("[Skills][Diag] %s", String.format(Locale.ROOT, message, args));
        }
    }
}
