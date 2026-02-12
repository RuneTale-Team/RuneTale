package org.runetale.skills.system;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Sends node-break notifications using Hytale notification UI with chat fallback.
 */
public class NotificationUtilSkillNodePlayerNotifier implements SkillNodePlayerNotifier {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	@Override
	public void notify(@Nullable PlayerRef playerRef, @Nonnull String text, @Nonnull NotificationStyle style) {
		if (playerRef == null) {
			return;
		}

		try {
			NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(text), style);
		} catch (Exception e) {
			LOGGER.atFine().withCause(e).log("Failed to send skills notification; falling back to chat message.");
			playerRef.sendMessage(Message.raw(text));
		}
	}
}
