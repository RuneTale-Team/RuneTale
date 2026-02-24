package org.runetale.skills.actions.interaction;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.runetale.skills.actions.ItemActionsRuntimeRegistry;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.ItemActionsConfig;
import org.runetale.skills.domain.SkillType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled("Requires EntityModule bootstrap for Player component type")
class ConsumeSkillActionInteractionTest {

    private SkillsRuntimeApi runtimeApi;
    private ItemActionsConfig itemActionsConfig;

    @AfterEach
    void tearDown() {
        if (this.runtimeApi != null) {
            SkillsRuntimeRegistry.clear(this.runtimeApi);
            this.runtimeApi = null;
        }
        if (this.itemActionsConfig != null) {
            ItemActionsRuntimeRegistry.clear(this.itemActionsConfig);
            this.itemActionsConfig = null;
        }
    }

    @Test
    void firstRunConsumesAndGrantsXpForPrimaryAction() {
        this.runtimeApi = mock(SkillsRuntimeApi.class);
        this.itemActionsConfig = new ItemActionsConfig(List.of(prayerAction()), "skills-actions");
        SkillsRuntimeRegistry.register(this.runtimeApi);
        ItemActionsRuntimeRegistry.register(this.itemActionsConfig);

        ConsumeSkillActionInteraction interaction = new ConsumeSkillActionInteraction();
        InteractionContext context = mock(InteractionContext.class);
        CooldownHandler cooldownHandler = mock(CooldownHandler.class);
        CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
        @SuppressWarnings("unchecked")
        Ref<EntityStore> playerRef = (Ref<EntityStore>) mock(Ref.class);
        Player player = mock(Player.class);
        ItemContainer container = mock(ItemContainer.class);
        ItemStack heldItem = mock(ItemStack.class);
        ItemStackSlotTransaction transaction = mock(ItemStackSlotTransaction.class);
        @SuppressWarnings("unchecked")
        Store<EntityStore> store = (Store<EntityStore>) mock(Store.class);

        InteractionSyncData state = new InteractionSyncData();
        when(context.getCommandBuffer()).thenReturn(commandBuffer);
        when(context.getEntity()).thenReturn(playerRef);
        when(context.getHeldItemContainer()).thenReturn(container);
        when(context.getHeldItemSlot()).thenReturn((byte) 5);
        when(context.getHeldItem()).thenReturn(heldItem);
        when(context.getState()).thenReturn(state);

        when(playerRef.isValid()).thenReturn(true);
        when(playerRef.getStore()).thenReturn(store);
        when(commandBuffer.getComponent(playerRef, Player.getComponentType())).thenReturn(player);

        when(player.getGameMode()).thenReturn(GameMode.Adventure);
        when(heldItem.getItemId()).thenReturn("RuneTale_Bones");
        when(heldItem.getQuantity()).thenReturn(1);

        when(this.runtimeApi.hasSkillProfile(store, playerRef)).thenReturn(true);
        when(container.removeItemStackFromSlot((short) 5, heldItem, 1, true, true)).thenReturn(transaction);
        when(transaction.succeeded()).thenReturn(true);
        when(container.getItemStack((short) 5)).thenReturn(null);
        when(this.runtimeApi.grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true)).thenReturn(true);

        interaction.firstRun(InteractionType.Primary, context, cooldownHandler);

        verify(container).removeItemStackFromSlot((short) 5, heldItem, 1, true, true);
        verify(this.runtimeApi).grantSkillXp(store, playerRef, SkillType.PRAYER, 4.5D, "prayer:bury", true);
        verify(context).setHeldItem(null);
        assertThat(state.state).isEqualTo(InteractionState.Finished);
    }

    @Test
    void firstRunFailsForMismatchedInteractionType() {
        this.runtimeApi = mock(SkillsRuntimeApi.class);
        this.itemActionsConfig = new ItemActionsConfig(List.of(prayerAction()), "skills-actions");
        SkillsRuntimeRegistry.register(this.runtimeApi);
        ItemActionsRuntimeRegistry.register(this.itemActionsConfig);

        ConsumeSkillActionInteraction interaction = new ConsumeSkillActionInteraction();
        InteractionContext context = mock(InteractionContext.class);
        CooldownHandler cooldownHandler = mock(CooldownHandler.class);
        CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
        @SuppressWarnings("unchecked")
        Ref<EntityStore> playerRef = (Ref<EntityStore>) mock(Ref.class);
        Player player = mock(Player.class);
        ItemContainer container = mock(ItemContainer.class);
        ItemStack heldItem = mock(ItemStack.class);

        InteractionSyncData state = new InteractionSyncData();
        when(context.getCommandBuffer()).thenReturn(commandBuffer);
        when(context.getEntity()).thenReturn(playerRef);
        when(context.getHeldItemContainer()).thenReturn(container);
        when(context.getHeldItemSlot()).thenReturn((byte) 5);
        when(context.getHeldItem()).thenReturn(heldItem);
        when(context.getState()).thenReturn(state);

        when(playerRef.isValid()).thenReturn(true);
        when(commandBuffer.getComponent(playerRef, Player.getComponentType())).thenReturn(player);
        when(player.getGameMode()).thenReturn(GameMode.Adventure);
        when(heldItem.getItemId()).thenReturn("RuneTale_Bones");
        when(heldItem.getQuantity()).thenReturn(1);

        interaction.firstRun(InteractionType.Secondary, context, cooldownHandler);

        verify(container, never()).removeItemStackFromSlot((short) 5, heldItem, 1, true, true);
        assertThat(state.state).isEqualTo(InteractionState.Failed);
    }

    private static ItemActionsConfig.ItemXpActionDefinition prayerAction() {
        return new ItemActionsConfig.ItemXpActionDefinition(
                "prayer_bury_bones",
                true,
                "RuneTale_Bones",
                SkillType.PRAYER,
                4.5D,
                1,
                "prayer:bury",
                true,
                true,
                false,
                MouseButtonType.Left,
                MouseButtonState.Pressed);
    }
}
