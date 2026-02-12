package org.runetale.skills.service;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Decision result for node break handling.
 */
public class SkillNodeBreakResolutionResult {

	private final boolean cancelBreak;
	private final String playerMessage;
	private final NotificationStyle notificationStyle;
	private final SkillType skillType;
	private final double experience;
	private final String sourceTag;

	private SkillNodeBreakResolutionResult(
			boolean cancelBreak,
			@Nullable String playerMessage,
			@Nonnull NotificationStyle notificationStyle,
			@Nullable SkillType skillType,
			double experience,
			@Nullable String sourceTag) {
		this.cancelBreak = cancelBreak;
		this.playerMessage = playerMessage;
		this.notificationStyle = notificationStyle;
		this.skillType = skillType;
		this.experience = Math.max(0.0D, experience);
		this.sourceTag = sourceTag;
	}

	@Nonnull
	public static SkillNodeBreakResolutionResult noAction() {
		return new SkillNodeBreakResolutionResult(false, null, NotificationStyle.Default, null, 0.0D, null);
	}

	@Nonnull
	public static SkillNodeBreakResolutionResult cancelWithWarning(@Nonnull String message) {
		return new SkillNodeBreakResolutionResult(true, message, NotificationStyle.Warning, null, 0.0D, null);
	}

	@Nonnull
	public static SkillNodeBreakResolutionResult dispatchXp(
			@Nonnull SkillType skillType,
			double experience,
			@Nonnull String sourceTag) {
		return new SkillNodeBreakResolutionResult(false, null, NotificationStyle.Default, skillType, experience, sourceTag);
	}

	public boolean shouldCancelBreak() {
		return this.cancelBreak;
	}

	public boolean shouldNotifyPlayer() {
		return this.playerMessage != null;
	}

	@Nullable
	public String getPlayerMessage() {
		return this.playerMessage;
	}

	@Nonnull
	public NotificationStyle getNotificationStyle() {
		return this.notificationStyle;
	}

	public boolean shouldDispatchXp() {
		return this.skillType != null && this.sourceTag != null && this.experience > 0.0D;
	}

	@Nullable
	public SkillType getSkillType() {
		return this.skillType;
	}

	public double getExperience() {
		return this.experience;
	}

	@Nullable
	public String getSourceTag() {
		return this.sourceTag;
	}
}
