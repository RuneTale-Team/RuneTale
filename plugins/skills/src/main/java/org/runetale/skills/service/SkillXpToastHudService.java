package org.runetale.skills.service;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.runetale.skills.config.HudConfig;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a top-center, icon-based XP toast via CustomHud commands.
 */
public class SkillXpToastHudService {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String HUD_DOCUMENT_PATH = "SkillsPlugin/SkillXpToastHud.ui";
	private static final String HUD_DOCUMENT_PATH_LEVEL_UP = "SkillsPlugin/SkillXpToastHudLevelUp.ui";
	private static final String ROOT_SELECTOR = "#SkillsXpToastRoot";
	private static final String INNER_SELECTOR = ROOT_SELECTOR + " #ToastInner";
	private static final String ICON_SELECTOR = ROOT_SELECTOR + " #ToastIcon";
	private static final String PRIMARY_TEXT_SELECTOR = ROOT_SELECTOR + " #ToastPrimary";
	private static final String SECONDARY_TEXT_SELECTOR = ROOT_SELECTOR + " #ToastSecondary";

	private final HudConfig hudConfig;

	private final Map<UUID, Long> hideAtByPlayer = new ConcurrentHashMap<>();
	private final Set<UUID> visibleByPlayer = ConcurrentHashMap.newKeySet();
	private final Set<UUID> fadedByPlayer = ConcurrentHashMap.newKeySet();

	public SkillXpToastHudService(@Nonnull HudConfig hudConfig) {
		this.hudConfig = hudConfig;
	}

	public void showXpToast(@Nullable PlayerRef playerRef, @Nonnull SkillType skillType, long gainedXp) {
		showXpToast(playerRef, skillType, gainedXp, false);
	}

	public void showXpToast(@Nullable PlayerRef playerRef, @Nonnull SkillType skillType, long gainedXp, boolean levelUpActive) {
		if (playerRef == null || gainedXp <= 0L) {
			return;
		}

		UUID playerUuid = playerRef.getUuid();
		UICommandBuilder commandBuilder = new UICommandBuilder();
		if (this.visibleByPlayer.contains(playerUuid)) {
			commandBuilder.remove(ROOT_SELECTOR);
		}
		commandBuilder.append(levelUpActive ? HUD_DOCUMENT_PATH_LEVEL_UP : HUD_DOCUMENT_PATH);
		commandBuilder.set(ICON_SELECTOR + ".Background", skillIconTexturePath(skillType));
		commandBuilder.set(PRIMARY_TEXT_SELECTOR + ".Text", String.format(Locale.ROOT, "+%,d XP", gainedXp));
		commandBuilder.set(SECONDARY_TEXT_SELECTOR + ".Text", formatSkillName(skillType));

		send(playerRef, commandBuilder);
		this.visibleByPlayer.add(playerUuid);
		this.fadedByPlayer.remove(playerUuid);
		this.hideAtByPlayer.put(playerUuid, System.currentTimeMillis() + this.hudConfig.toastDurationMillis());
	}

	public void tickLifecycle(@Nullable PlayerRef playerRef, long nowMillis) {
		if (playerRef == null) {
			return;
		}

		UUID playerUuid = playerRef.getUuid();
		Long hideAt = this.hideAtByPlayer.get(playerUuid);
		if (hideAt == null) {
			return;
		}

		if (nowMillis >= hideAt) {
			hideToast(playerRef);
			return;
		}

		long fadeAt = hideAt - this.hudConfig.toastFadeDurationMillis();
		if (nowMillis < fadeAt || !this.fadedByPlayer.add(playerUuid)) {
			return;
		}

		applyFade(playerRef);
	}

	public void hideToast(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}

		UUID playerUuid = playerRef.getUuid();
		this.hideAtByPlayer.remove(playerUuid);
		this.fadedByPlayer.remove(playerUuid);
		if (!this.visibleByPlayer.remove(playerUuid)) {
			return;
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		commandBuilder.remove(ROOT_SELECTOR);
		send(playerRef, commandBuilder);
	}

	public void clear(@Nonnull UUID playerId) {
		this.hideAtByPlayer.remove(playerId);
		this.visibleByPlayer.remove(playerId);
		this.fadedByPlayer.remove(playerId);
	}

	private void applyFade(@Nonnull PlayerRef playerRef) {
		if (!this.visibleByPlayer.contains(playerRef.getUuid())) {
			return;
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		commandBuilder.set(ROOT_SELECTOR + ".Background", this.hudConfig.rootBackgroundFaded());
		commandBuilder.set(INNER_SELECTOR + ".Background", this.hudConfig.innerBackgroundFaded());
		send(playerRef, commandBuilder);
	}

	private void send(@Nonnull PlayerRef playerRef, @Nonnull UICommandBuilder commandBuilder) {
		try {
			// NOTE: Hytale packet transport signatures may change across releases (Packet vs ToClientPacket).
			playerRef.getPacketHandler().writeNoCache((ToClientPacket) new CustomHud(false, commandBuilder.getCommands()));
		} catch (Exception e) {
			LOGGER.atFine().withCause(e).log("Failed to update custom XP toast HUD.");
		}
	}

	@Nonnull
	private String skillIconTexturePath(@Nonnull SkillType skillType) {
		String id = skillIconId(skillType);
		return "SkillsPlugin/Assets/Icons/icon_" + id + ".png";
	}

	@Nonnull
	private String skillIconId(@Nonnull SkillType skillType) {
		if (skillType == SkillType.DEFENCE) {
			return "defense";
		}
		return skillType.name().toLowerCase(Locale.ROOT);
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skillType) {
		String lowered = skillType.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}
}
