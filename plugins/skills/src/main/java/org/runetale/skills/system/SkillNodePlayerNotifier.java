package org.runetale.skills.system;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Sends player-facing node-break notifications.
 */
public interface SkillNodePlayerNotifier {

	void notify(@Nullable PlayerRef playerRef, @Nonnull String text, @Nonnull NotificationStyle style);
}
