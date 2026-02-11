package org.runetale.skills.progression.service;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Announces level-up milestones using EventTitle UI messages.
 */
public class EventTitleSkillLevelUpAnnouncer implements SkillLevelUpAnnouncer {

	@Override
	public void announceLevelUp(@Nonnull PlayerRef playerRef, @Nonnull SkillType skillType, int newLevel) {
		EventTitleUtil.showEventTitleToPlayer(
				playerRef,
				Message.raw(formatSkillName(skillType) + " Level Up!"),
				Message.raw("Now level " + newLevel),
				true);
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skillType) {
		String lowered = skillType.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}
}
