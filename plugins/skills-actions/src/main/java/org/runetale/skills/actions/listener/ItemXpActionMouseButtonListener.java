package org.runetale.skills.actions.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.ItemActionsConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ItemXpActionMouseButtonListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final SkillsRuntimeApi runtimeApi;
    private final ItemActionsConfig itemActionsConfig;

    public ItemXpActionMouseButtonListener(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull ItemActionsConfig itemActionsConfig) {
        this.runtimeApi = runtimeApi;
        this.itemActionsConfig = itemActionsConfig;
    }

    public void handle(@Nonnull PlayerMouseButtonEvent event) {
        if (event.isCancelled()) {
            return;
        }

        MouseButtonEvent mouseButton = event.getMouseButton();
        if (mouseButton == null) {
            return;
        }

        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemContainer activeContainer = inventory.usingToolsItem() ? inventory.getTools() : inventory.getHotbar();
        short activeSlot = (short) (inventory.usingToolsItem() ? inventory.getActiveToolsSlot() : inventory.getActiveHotbarSlot());
        if (activeContainer == null || activeSlot < 0) {
            return;
        }

        ItemStack slotStack = activeContainer.getItemStack(activeSlot);
        if (slotStack == null || ItemStack.isEmpty(slotStack)) {
            return;
        }

        ItemActionsConfig.ItemXpActionDefinition action = matchAction(mouseButton, player, slotStack);
        if (action == null) {
            return;
        }

        Ref<EntityStore> playerRef = event.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        if (!this.runtimeApi.hasSkillProfile(store, playerRef)) {
            debugLog("Skipped action id=%s due to missing profile player=%s", action.id(), playerRef);
            return;
        }

        ItemStackSlotTransaction consumeTransaction = activeContainer.removeItemStackFromSlot(
                activeSlot,
                slotStack,
                action.consumeQuantity(),
                true,
                true);
        if (!consumeTransaction.succeeded()) {
            debugLog("Failed consume action id=%s slot=%d item=%s qty=%d", action.id(), activeSlot, slotStack.getItemId(), action.consumeQuantity());
            return;
        }

        boolean granted = this.runtimeApi.grantSkillXp(
                store,
                playerRef,
                action.skillType(),
                action.experience(),
                action.source(),
                action.notifyPlayer());

        if (!granted) {
            LOGGER.atWarning().log("[Skills Actions] XP dispatch failed after consume action=%s player=%s skill=%s", action.id(), playerRef, action.skillType());
            return;
        }

        if (action.cancelInputEvent()) {
            event.setCancelled(true);
        }

        debugLog("Applied action id=%s item=%s qty=%d skill=%s xp=%.4f", action.id(), slotStack.getItemId(), action.consumeQuantity(), action.skillType(), action.experience());
    }

    @Nullable
    private ItemActionsConfig.ItemXpActionDefinition matchAction(
            @Nonnull MouseButtonEvent mouseButton,
            @Nonnull Player player,
            @Nonnull ItemStack heldStack) {
        for (ItemActionsConfig.ItemXpActionDefinition action : this.itemActionsConfig.actions()) {
            if (!action.enabled()) {
                continue;
            }
            if (mouseButton.mouseButtonType != action.mouseButtonType()) {
                continue;
            }
            if (mouseButton.state != action.mouseButtonState()) {
                continue;
            }
            if (!action.matchesItemId(heldStack.getItemId())) {
                continue;
            }
            if (heldStack.getQuantity() < action.consumeQuantity()) {
                continue;
            }
            if (!action.allowCreative() && player.getGameMode() == GameMode.Creative) {
                continue;
            }
            return action;
        }
        return null;
    }

    private void debugLog(@Nonnull String message, Object... args) {
        if (this.runtimeApi.isDebugEnabled(this.itemActionsConfig.debugPluginKey())) {
            LOGGER.atInfo().log("[Skills][Diag] " + message, args);
        }
    }
}
