package org.runetale.skills.progression.event;

import com.hypixel.hytale.component.system.EcsEvent;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

/**
 * Entity-scoped event fired when a player's skill level increases.
 */
public class SkillLevelUpEvent extends EcsEvent {

	private final SkillType skillType;
	private final int previousLevel;
	private final int newLevel;

	public SkillLevelUpEvent(
			@Nonnull SkillType skillType,
			int previousLevel,
			int newLevel) {
		this.skillType = skillType;
		this.previousLevel = previousLevel;
		this.newLevel = newLevel;
	}

	@Nonnull
	public SkillType getSkillType() {
		return this.skillType;
	}

	public int getPreviousLevel() {
		return this.previousLevel;
	}

	public int getNewLevel() {
		return this.newLevel;
	}
}
