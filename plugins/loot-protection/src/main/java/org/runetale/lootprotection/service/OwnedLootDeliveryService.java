package org.runetale.lootprotection.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class OwnedLootDeliveryService {

    private final OnlinePlayerLookupService onlinePlayerLookupService;
    private final LootProtectionNotificationService notificationService;

    public OwnedLootDeliveryService(
            @Nonnull OnlinePlayerLookupService onlinePlayerLookupService,
            @Nonnull LootProtectionNotificationService notificationService) {
        this.onlinePlayerLookupService = onlinePlayerLookupService;
        this.notificationService = notificationService;
    }

    @Nonnull
    public DeliveryResult attemptDeliver(
            @Nonnull Ref<EntityStore> itemRef,
            @Nonnull ItemComponent itemComponent,
            @Nonnull UUID ownerPlayerId,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            boolean notifyIfInventoryFull) {
        Ref<EntityStore> ownerRef = this.onlinePlayerLookupService.getIfOnline(ownerPlayerId);
        if (ownerRef == null) {
            return DeliveryResult.OWNER_OFFLINE;
        }

        Player ownerPlayer = commandBuffer.getComponent(ownerRef, Player.getComponentType());
        if (ownerPlayer == null) {
            ownerPlayer = store.getComponent(ownerRef, Player.getComponentType());
        }
        if (ownerPlayer == null) {
            return DeliveryResult.OWNER_OFFLINE;
        }

        ItemStack sourceStack = itemComponent.getItemStack();
        if (ItemStack.isEmpty(sourceStack)) {
            commandBuffer.removeEntity(itemRef, RemoveReason.REMOVE);
            return DeliveryResult.DELIVERED_ALL;
        }

        int sourceQuantity = sourceStack.getQuantity();
        ItemStackTransaction transaction = ownerPlayer.giveItem(sourceStack, ownerRef, commandBuffer);
        ItemStack remainder = transaction.getRemainder();

        if (ItemStack.isEmpty(remainder)) {
            itemComponent.setRemovedByPlayerPickup(true);
            commandBuffer.removeEntity(itemRef, RemoveReason.REMOVE);
            return DeliveryResult.DELIVERED_ALL;
        }

        itemComponent.setItemStack(remainder);
        if (remainder.getQuantity() < sourceQuantity) {
            return DeliveryResult.DELIVERED_PARTIAL;
        }

        if (notifyIfInventoryFull) {
            PlayerRef ownerPlayerRef = getPlayerRef(ownerRef, store, commandBuffer);
            this.notificationService.sendInventoryFullNotice(ownerPlayerRef);
        }
        return DeliveryResult.INVENTORY_FULL;
    }

    @Nullable
    private static PlayerRef getPlayerRef(
            @Nonnull Ref<EntityStore> ownerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = commandBuffer.getComponent(ownerRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
        }
        return playerRef;
    }

    public enum DeliveryResult {
        DELIVERED_ALL,
        DELIVERED_PARTIAL,
        INVENTORY_FULL,
        OWNER_OFFLINE
    }
}
