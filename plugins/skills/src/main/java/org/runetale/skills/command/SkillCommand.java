package org.runetale.skills.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.XpService;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Player-only command that prints the caller's current skill progression.
 */
public class SkillCommand extends AbstractPlayerCommand {

	private static final int MAX_LEVEL = 99;
	private final XpService xpService;

	public SkillCommand(@Nonnull XpService xpService) {
		super("skill", "Displays your skill levels and XP.");
		this.setPermissionGroup(GameMode.Adventure);
		this.xpService = xpService;
	}

	@Override
	protected void execute(
			@Nonnull CommandContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {
		if (PlayerSkillProfileComponent.getComponentType() == null) {
			playerRef.sendMessage(Message.raw("Skills profile is currently unavailable."));
			return;
		}

		PlayerSkillProfileComponent profile = store.getComponent(ref, PlayerSkillProfileComponent.getComponentType());
		if (profile == null) {
			playerRef.sendMessage(Message.raw("[Skills] Profile missing; showing defaults."));
			for (SkillType skillType : SkillType.values()) {
				playerRef.sendMessage(Message.raw(String.format("- %s | Lv 1 | XP 0 | Progress 0/%d", formatSkillName(skillType),
						xpRequiredForNextLevel(1))));
			}
			return;
		}

		long totalXp = 0L;
		int totalLevel = 0;
		SkillType highestSkill = SkillType.WOODCUTTING;
		int highestLevel = 1;

		for (SkillType skillType : SkillType.values()) {
			int level = profile.getLevel(skillType);
			totalLevel += level;
			if (level > highestLevel) {
				highestLevel = level;
				highestSkill = skillType;
			}
			totalXp += profile.getExperience(skillType);
		}

		playerRef.sendMessage(Message.raw("[Skills] Overview"));
		playerRef.sendMessage(Message.raw(String.format("Total Level: %d | Total XP: %,d | Highest: %s Lv %d",
				totalLevel,
				totalXp,
				formatSkillName(highestSkill),
				highestLevel)));
		playerRef.sendMessage(Message.raw("------------------------------"));

		for (SkillType skillType : SkillType.values()) {
			int level = profile.getLevel(skillType);
			long experience = profile.getExperience(skillType);
			String progress = formatProgress(level, experience);
			playerRef
					.sendMessage(Message.raw(
							String.format("- %s | Lv %d | XP %,d | %s", formatSkillName(skillType), level, experience, progress)));
		}
	}

	private String formatProgress(int level, long experience) {
		int safeLevel = Math.max(1, Math.min(MAX_LEVEL, level));
		if (safeLevel >= MAX_LEVEL) {
			return "MAX";
		}

		long levelStartXp = this.xpService.xpForLevel(safeLevel);
		long nextLevelXp = this.xpService.xpForLevel(safeLevel + 1);
		long xpNeeded = Math.max(1L, nextLevelXp - levelStartXp);
		long xpIntoLevel = Math.max(0L, experience - levelStartXp);
		long clampedXpIntoLevel = Math.min(xpIntoLevel, xpNeeded);

		double percent = (double) clampedXpIntoLevel * 100.0D / (double) xpNeeded;
		return String.format(Locale.ROOT, "Progress %d/%d (%.1f%%)", clampedXpIntoLevel, xpNeeded, percent);
	}

	private long xpRequiredForNextLevel(int level) {
		int safeLevel = Math.max(1, Math.min(MAX_LEVEL - 1, level));
		long currentLevelXp = this.xpService.xpForLevel(safeLevel);
		long nextLevelXp = this.xpService.xpForLevel(safeLevel + 1);
		return Math.max(1L, nextLevelXp - currentLevelXp);
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skillType) {
		String lowered = skillType.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}
}
