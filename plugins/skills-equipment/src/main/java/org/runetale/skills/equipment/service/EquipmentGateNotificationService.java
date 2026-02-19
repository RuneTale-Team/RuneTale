package org.runetale.skills.equipment.service;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.equipment.domain.EquipmentLocation;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EquipmentGateNotificationService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EquipmentConfig equipmentConfig;
    private final Map<UUID, Long> lastNoticeByPlayer = new ConcurrentHashMap<>();

    public EquipmentGateNotificationService(@Nonnull EquipmentConfig equipmentConfig) {
        this.equipmentConfig = equipmentConfig;
    }

    public void sendBlockedEquipNotice(
            @Nonnull PlayerRef playerRef,
            @Nonnull SkillRequirement requirement,
            int currentLevel,
            @Nonnull String itemId,
            @Nonnull EquipmentLocation location) {
        long now = System.currentTimeMillis();
        Long last = this.lastNoticeByPlayer.get(playerRef.getUuid());
        if (last != null && now - last < this.equipmentConfig.notificationCooldownMillis()) {
            return;
        }
        this.lastNoticeByPlayer.put(playerRef.getUuid(), now);

        String message = String.format(
                this.equipmentConfig.notificationMessageTemplate(),
                formatSkillName(requirement.skillType().name()),
                currentLevel,
                requirement.requiredLevel(),
                itemId,
                location.displayName());

        try {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(message), NotificationStyle.Warning);
        } catch (Exception e) {
            LOGGER.atFine().withCause(e).log("Failed to send equipment gate notification; falling back to chat message.");
            playerRef.sendMessage(Message.raw(message));
        }
    }

    private static String formatSkillName(@Nonnull String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
