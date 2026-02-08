package org.runetale.skills.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Player-selected melee combat style used to route combat XP.
 */
public enum CombatStyleType {
	ATTACK(SkillType.ATTACK),
	STRENGTH(SkillType.STRENGTH),
	DEFENSE(SkillType.DEFENSE);

	private final SkillType grantedSkill;

	CombatStyleType(@Nonnull SkillType grantedSkill) {
		this.grantedSkill = grantedSkill;
	}

	@Nonnull
	public SkillType getGrantedSkill() {
		return this.grantedSkill;
	}

	@Nullable
	public static CombatStyleType tryParse(@Nullable String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}

		try {
			return CombatStyleType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
