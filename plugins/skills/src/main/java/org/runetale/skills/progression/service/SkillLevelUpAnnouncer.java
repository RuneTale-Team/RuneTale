package org.runetale.skills.progression.service;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

/**
 * Abstraction for notifying players about skill level-up milestones.
 */
public interface SkillLevelUpAnnouncer {

	void announceLevelUp(@Nonnull PlayerRef playerRef, @Nonnull SkillType skillType, int newLevel);
}
