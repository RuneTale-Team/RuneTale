package org.runetale.skills.actions.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.ItemActionsConfig;
import org.runetale.skills.domain.SkillType;

import java.util.List;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemXpActionMouseButtonListenerTest {

    @Test
    void handleConsumesConfiguredQuantityFromActiveHotbarSlot() {
        SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
        ItemActionsConfig config = new ItemActionsConfig(
                List.of(prayerAction(2)),
                "skills-actions");
        ItemXpActionMouseButtonListener listener = new ItemXpActionMouseButtonListener(runtimeApi, config);

        PlayerMouseButtonEvent event = mock(PlayerMouseButtonEvent.class);
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);
        ItemContainer hotbar = mock(ItemContainer.class);
        ItemStack heldStack = mock(ItemStack.class);
        ItemStackSlotTransaction consumeTransaction = mock(ItemStackSlotTransaction.class);
        @SuppressWarnings("unchecked")
        Ref<EntityStore> playerRef = (Ref<EntityStore>) mock(Ref.class);
        @SuppressWarnings("unchecked")
        Store<EntityStore> store = (Store<EntityStore>) mock(Store.class);

        when(event.isCancelled()).thenReturn(false);
        when(event.getMouseButton()).thenReturn(new MouseButtonEvent(MouseButtonType.Right, MouseButtonState.Pressed, (byte) 1));
        when(event.getPlayer()).thenReturn(player);
        when(event.getPlayerRef()).thenReturn(playerRef);

        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.Adventure);
        when(inventory.usingToolsItem()).thenReturn(false);
        when(inventory.getHotbar()).thenReturn(hotbar);
        when(inventory.getActiveHotbarSlot()).thenReturn((byte) 3);
        when(hotbar.getItemStack((short) 3)).thenReturn(heldStack);
        when(heldStack.getItemId()).thenReturn("RuneTale_Bones");
        when(heldStack.getQuantity()).thenReturn(5);
        when(playerRef.isValid()).thenReturn(true);
        when(playerRef.getStore()).thenReturn(store);
        when(runtimeApi.hasSkillProfile(store, playerRef)).thenReturn(true);
        when(hotbar.removeItemStackFromSlot((short) 3, heldStack, 2, true, true)).thenReturn(consumeTransaction);
        when(consumeTransaction.succeeded()).thenReturn(true);
        when(runtimeApi.grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true)).thenReturn(true);

        listener.handle(event);

        verify(hotbar).removeItemStackFromSlot((short) 3, heldStack, 2, true, true);
        verify(runtimeApi).grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true);
        verify(event).setCancelled(true);
    }

    @Test
    void handleConsumesFromActiveToolsSlotWhenHoldingToolHand() {
        SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
        ItemActionsConfig config = new ItemActionsConfig(
                List.of(prayerAction(1)),
                "skills-actions");
        ItemXpActionMouseButtonListener listener = new ItemXpActionMouseButtonListener(runtimeApi, config);

        PlayerMouseButtonEvent event = mock(PlayerMouseButtonEvent.class);
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);
        ItemContainer tools = mock(ItemContainer.class);
        ItemStack heldStack = mock(ItemStack.class);
        ItemStackSlotTransaction consumeTransaction = mock(ItemStackSlotTransaction.class);
        @SuppressWarnings("unchecked")
        Ref<EntityStore> playerRef = (Ref<EntityStore>) mock(Ref.class);
        @SuppressWarnings("unchecked")
        Store<EntityStore> store = (Store<EntityStore>) mock(Store.class);

        when(event.isCancelled()).thenReturn(false);
        when(event.getMouseButton()).thenReturn(new MouseButtonEvent(MouseButtonType.Right, MouseButtonState.Pressed, (byte) 1));
        when(event.getPlayer()).thenReturn(player);
        when(event.getPlayerRef()).thenReturn(playerRef);

        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.Adventure);
        when(inventory.usingToolsItem()).thenReturn(true);
        when(inventory.getTools()).thenReturn(tools);
        when(inventory.getActiveToolsSlot()).thenReturn((byte) 2);
        when(tools.getItemStack((short) 2)).thenReturn(heldStack);
        when(heldStack.getItemId()).thenReturn("RuneTale_Bones");
        when(heldStack.getQuantity()).thenReturn(1);
        when(playerRef.isValid()).thenReturn(true);
        when(playerRef.getStore()).thenReturn(store);
        when(runtimeApi.hasSkillProfile(store, playerRef)).thenReturn(true);
        when(tools.removeItemStackFromSlot((short) 2, heldStack, 1, true, true)).thenReturn(consumeTransaction);
        when(consumeTransaction.succeeded()).thenReturn(true);
        when(runtimeApi.grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true)).thenReturn(true);

        listener.handle(event);

        verify(tools).removeItemStackFromSlot((short) 2, heldStack, 1, true, true);
        verify(runtimeApi).grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true);
    }

    @Test
    void handleSkipsWhenActiveHandHasInsufficientQuantity() {
        SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
        ItemActionsConfig config = new ItemActionsConfig(
                List.of(prayerAction(2)),
                "skills-actions");
        ItemXpActionMouseButtonListener listener = new ItemXpActionMouseButtonListener(runtimeApi, config);

        PlayerMouseButtonEvent event = mock(PlayerMouseButtonEvent.class);
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);
        ItemContainer hotbar = mock(ItemContainer.class);
        ItemStack heldStack = mock(ItemStack.class);

        when(event.isCancelled()).thenReturn(false);
        when(event.getMouseButton()).thenReturn(new MouseButtonEvent(MouseButtonType.Right, MouseButtonState.Pressed, (byte) 1));
        when(event.getPlayer()).thenReturn(player);

        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.Adventure);
        when(inventory.usingToolsItem()).thenReturn(false);
        when(inventory.getHotbar()).thenReturn(hotbar);
        when(inventory.getActiveHotbarSlot()).thenReturn((byte) 1);
        when(hotbar.getItemStack((short) 1)).thenReturn(heldStack);
        when(heldStack.getItemId()).thenReturn("RuneTale_Bones");
        when(heldStack.getQuantity()).thenReturn(1);

        listener.handle(event);

        verify(hotbar, never()).removeItemStackFromSlot(eq((short) 1), eq(heldStack), eq(2), eq(true), eq(true));
        verifyNoInteractions(runtimeApi);
    }

    @SuppressWarnings("deprecation")
    @Test
    void handleProcessesSecondaryInteractAsRightClickFallback() {
        SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
        ItemActionsConfig config = new ItemActionsConfig(
                List.of(prayerAction(1)),
                "skills-actions");
        ItemXpActionMouseButtonListener listener = new ItemXpActionMouseButtonListener(runtimeApi, config);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);
        ItemContainer hotbar = mock(ItemContainer.class);
        ItemStack heldStack = mock(ItemStack.class);
        ItemStackSlotTransaction consumeTransaction = mock(ItemStackSlotTransaction.class);
        @SuppressWarnings("unchecked")
        Ref<EntityStore> playerRef = (Ref<EntityStore>) mock(Ref.class);
        @SuppressWarnings("unchecked")
        Store<EntityStore> store = (Store<EntityStore>) mock(Store.class);

        when(event.getActionType()).thenReturn(InteractionType.Secondary);
        when(event.getPlayer()).thenReturn(player);
        when(event.getPlayerRef()).thenReturn(playerRef);
        when(event.getClientUseTime()).thenReturn(42L);

        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.Adventure);
        when(player.getUuid()).thenReturn(java.util.UUID.randomUUID());
        when(inventory.usingToolsItem()).thenReturn(false);
        when(inventory.getHotbar()).thenReturn(hotbar);
        when(inventory.getActiveHotbarSlot()).thenReturn((byte) 0);
        when(hotbar.getItemStack((short) 0)).thenReturn(heldStack);
        when(heldStack.getItemId()).thenReturn("runetale:RuneTale_Bones");
        when(heldStack.getQuantity()).thenReturn(2);
        when(playerRef.isValid()).thenReturn(true);
        when(playerRef.getStore()).thenReturn(store);
        when(runtimeApi.hasSkillProfile(store, playerRef)).thenReturn(true);
        when(hotbar.removeItemStackFromSlot((short) 0, heldStack, 1, true, true)).thenReturn(consumeTransaction);
        when(consumeTransaction.succeeded()).thenReturn(true);
        when(runtimeApi.grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true)).thenReturn(true);

        listener.handle(event);

        verify(hotbar).removeItemStackFromSlot((short) 0, heldStack, 1, true, true);
        verify(runtimeApi).grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true);
        verify(event).setCancelled(true);
    }

    private static ItemActionsConfig.ItemXpActionDefinition prayerAction(int consumeQuantity) {
        return new ItemActionsConfig.ItemXpActionDefinition(
                "prayer_bury_bones",
                true,
                "RuneTale_Bones",
                SkillType.PRAYER,
                4.5D,
                consumeQuantity,
                "prayer:bury",
                true,
                true,
                false,
                MouseButtonType.Right,
                MouseButtonState.Pressed);
    }
}
