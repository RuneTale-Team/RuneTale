package org.runetale.skills.progression.event;

import com.hypixel.hytale.component.system.EcsEvent;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

/**
 * Entity-scoped event requesting XP gain for a specific skill.
 */
public class SkillXpGrantEvent extends EcsEvent {

	private final SkillType skillType;
	private final double experience;
	private final String source;
	private final boolean notifyPlayer;

	public SkillXpGrantEvent(
			@Nonnull SkillType skillType,
			double experience,
			@Nonnull String source,
			boolean notifyPlayer) {
		this.skillType = skillType;
		this.experience = experience;
		this.source = source;
		this.notifyPlayer = notifyPlayer;
	}

	@Nonnull
	public SkillType getSkillType() {
		return this.skillType;
	}

	public double getExperience() {
		return this.experience;
	}

	@Nonnull
	public String getSource() {
		return this.source;
	}

	public boolean shouldNotifyPlayer() {
		return this.notifyPlayer;
	}
}
