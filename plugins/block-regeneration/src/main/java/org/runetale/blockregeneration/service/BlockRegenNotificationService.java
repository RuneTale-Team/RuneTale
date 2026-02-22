package org.runetale.blockregeneration.service;

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

public class BlockRegenNotificationService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final LongSupplier cooldownMillisSupplier;
    private final Map<UUID, Long> lastNoticeByPlayer = new ConcurrentHashMap<>();

    public BlockRegenNotificationService(@Nonnull LongSupplier cooldownMillisSupplier) {
        this.cooldownMillisSupplier = cooldownMillisSupplier;
    }

    public void sendDepletedNotice(@Nullable PlayerRef playerRef) {
        sendNotice(playerRef, "[BlockRegen] This node is depleted. It will regenerate soon.", NotificationStyle.Warning);
    }

    private void sendNotice(@Nullable PlayerRef playerRef, @Nonnull String text, @Nonnull NotificationStyle style) {
        if (playerRef == null || isNotificationCoolingDown(playerRef)) {
            return;
        }

        try {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(text), style);
        } catch (Exception e) {
            LOGGER.atFine().withCause(e).log("Failed to send block regen notification; falling back to chat message.");
            playerRef.sendMessage(Message.raw(text));
        }
    }

    private boolean isNotificationCoolingDown(@Nonnull PlayerRef playerRef) {
        long now = System.currentTimeMillis();
        UUID playerId = playerRef.getUuid();
        Long lastNotifiedAt = this.lastNoticeByPlayer.get(playerId);
        long cooldownMillis = Math.max(100L, this.cooldownMillisSupplier.getAsLong());
        if (lastNotifiedAt != null && now - lastNotifiedAt < cooldownMillis) {
            return true;
        }
        this.lastNoticeByPlayer.put(playerId, now);
        return false;
    }

    public void clear() {
        this.lastNoticeByPlayer.clear();
    }
}
