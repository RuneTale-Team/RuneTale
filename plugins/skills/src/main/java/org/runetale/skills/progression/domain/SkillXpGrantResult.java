package org.runetale.skills.progression.domain;

import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

/**
 * Immutable progression snapshot produced by an XP grant mutation.
 */
public class SkillXpGrantResult {

	private final SkillType skillType;
	private final long previousExperience;
	private final long updatedExperience;
	private final long gainedExperience;
	private final int previousLevel;
	private final int updatedLevel;

	public SkillXpGrantResult(
			@Nonnull SkillType skillType,
			long previousExperience,
			long updatedExperience,
			long gainedExperience,
			int previousLevel,
			int updatedLevel) {
		this.skillType = skillType;
		this.previousExperience = Math.max(0L, previousExperience);
		this.updatedExperience = Math.max(0L, updatedExperience);
		this.gainedExperience = Math.max(0L, gainedExperience);
		this.previousLevel = Math.max(1, previousLevel);
		this.updatedLevel = Math.max(1, updatedLevel);
	}

	@Nonnull
	public SkillType getSkillType() {
		return this.skillType;
	}

	public long getPreviousExperience() {
		return this.previousExperience;
	}

	public long getUpdatedExperience() {
		return this.updatedExperience;
	}

	public long getGainedExperience() {
		return this.gainedExperience;
	}

	public int getPreviousLevel() {
		return this.previousLevel;
	}

	public int getUpdatedLevel() {
		return this.updatedLevel;
	}

	public boolean isLevelUp() {
		return this.updatedLevel > this.previousLevel;
	}
}
