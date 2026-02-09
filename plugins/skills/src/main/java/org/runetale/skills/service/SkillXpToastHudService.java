package org.runetale.skills.service;

import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders a top-center, icon-based XP toast via CustomHud commands.
 */
public class SkillXpToastHudService {

	private static final Logger LOGGER = Logger.getLogger(SkillXpToastHudService.class.getName());

	private static final String HUD_DOCUMENT_PATH = "SkillsPlugin/SkillXpToastHud.ui";
	private static final String HUD_DOCUMENT_PATH_LEVEL_UP = "SkillsPlugin/SkillXpToastHudLevelUp.ui";
	private static final String ROOT_SELECTOR = "#SkillsXpToastRoot";
	private static final String INNER_SELECTOR = ROOT_SELECTOR + " #ToastInner";
	private static final String ICON_SELECTOR = ROOT_SELECTOR + " #ToastIcon";
	private static final String PRIMARY_TEXT_SELECTOR = ROOT_SELECTOR + " #ToastPrimary";
	private static final String SECONDARY_TEXT_SELECTOR = ROOT_SELECTOR + " #ToastSecondary";
	private static final long XP_TOAST_DURATION_MILLIS = 1400L;
	private static final long XP_TOAST_FADE_DURATION_MILLIS = 180L;
	private static final String ROOT_BACKGROUND_FADED = "#1b314b";
	private static final String INNER_BACKGROUND_FADED = "#0a1421";

	private final Map<UUID, Long> hideAtByPlayer = new ConcurrentHashMap<>();
	private final Set<UUID> visibleByPlayer = ConcurrentHashMap.newKeySet();
	private final Set<UUID> fadedByPlayer = ConcurrentHashMap.newKeySet();

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
		this.hideAtByPlayer.put(playerUuid, System.currentTimeMillis() + XP_TOAST_DURATION_MILLIS);
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

		long fadeAt = hideAt - XP_TOAST_FADE_DURATION_MILLIS;
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

	private void applyFade(@Nonnull PlayerRef playerRef) {
		if (!this.visibleByPlayer.contains(playerRef.getUuid())) {
			return;
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		commandBuilder.set(ROOT_SELECTOR + ".Background", ROOT_BACKGROUND_FADED);
		commandBuilder.set(INNER_SELECTOR + ".Background", INNER_BACKGROUND_FADED);
		send(playerRef, commandBuilder);
	}

	private void send(@Nonnull PlayerRef playerRef, @Nonnull UICommandBuilder commandBuilder) {
		try {
			playerRef.getPacketHandler().writeNoCache(new CustomHud(false, commandBuilder.getCommands()));
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Failed to update custom XP toast HUD.", e);
		}
	}

	@Nonnull
	private String skillIconTexturePath(@Nonnull SkillType skillType) {
		String id = skillType.name().toLowerCase(Locale.ROOT);
		return "SkillsPlugin/Assets/Icons/icon_" + id + ".png";
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skillType) {
		String lowered = skillType.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}
}
