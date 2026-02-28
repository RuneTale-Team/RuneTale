package org.runetale.lootprotection.service;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class LootProtectionNotificationService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String CONTESTED_BLOCK_TEXT =
            "[LootProtection] Another player already has this node claimed.";
    private static final String INVENTORY_FULL_TEXT =
            "[LootProtection] Inventory full. Your loot is protected temporarily.";

    private final LongSupplier blockOwnershipNotifyCooldownSupplier;
    private final LongSupplier inventoryFullNotifyCooldownSupplier;
    private final Map<UUID, Long> lastBlockNoticeAtByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInventoryFullNoticeAtByPlayer = new ConcurrentHashMap<>();

    public LootProtectionNotificationService(
            @Nonnull LongSupplier blockOwnershipNotifyCooldownSupplier,
            @Nonnull LongSupplier inventoryFullNotifyCooldownSupplier) {
        this.blockOwnershipNotifyCooldownSupplier = blockOwnershipNotifyCooldownSupplier;
        this.inventoryFullNotifyCooldownSupplier = inventoryFullNotifyCooldownSupplier;
    }

    public void sendContestedBlockNotice(@Nullable PlayerRef playerRef) {
        sendNotice(
                playerRef,
                this.lastBlockNoticeAtByPlayer,
                this.blockOwnershipNotifyCooldownSupplier,
                CONTESTED_BLOCK_TEXT,
                NotificationStyle.Warning);
    }

    public void sendInventoryFullNotice(@Nullable PlayerRef playerRef) {
        sendNotice(
                playerRef,
                this.lastInventoryFullNoticeAtByPlayer,
                this.inventoryFullNotifyCooldownSupplier,
                INVENTORY_FULL_TEXT,
                NotificationStyle.Default);
    }

    private void sendNotice(
            @Nullable PlayerRef playerRef,
            @Nonnull Map<UUID, Long> lastNoticeAtByPlayer,
            @Nonnull LongSupplier cooldownMillisSupplier,
            @Nonnull String text,
            @Nonnull NotificationStyle style) {
        if (playerRef == null || isCoolingDown(playerRef, lastNoticeAtByPlayer, cooldownMillisSupplier)) {
            return;
        }

        try {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(text), style);
        } catch (Exception e) {
            LOGGER.atFine().withCause(e).log("Failed to send loot protection notification; falling back to chat message.");
            playerRef.sendMessage(Message.raw(text));
        }
    }

    private static boolean isCoolingDown(
            @Nonnull PlayerRef playerRef,
            @Nonnull Map<UUID, Long> lastNoticeAtByPlayer,
            @Nonnull LongSupplier cooldownMillisSupplier) {
        long now = System.currentTimeMillis();
        UUID playerId = playerRef.getUuid();
        Long lastNotifiedAt = lastNoticeAtByPlayer.get(playerId);
        long cooldownMillis = Math.max(100L, cooldownMillisSupplier.getAsLong());
        if (lastNotifiedAt != null && now - lastNotifiedAt < cooldownMillis) {
            return true;
        }
        lastNoticeAtByPlayer.put(playerId, now);
        return false;
    }

    public void clear() {
        this.lastBlockNoticeAtByPlayer.clear();
        this.lastInventoryFullNoticeAtByPlayer.clear();
    }
}
