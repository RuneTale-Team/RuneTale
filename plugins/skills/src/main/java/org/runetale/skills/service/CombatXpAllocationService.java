package org.runetale.skills.service;

import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Computes combat XP routing grants based on damage source and combat style.
 */
public class CombatXpAllocationService {

	private static final double XP_PER_DAMAGE = 4.0D;
	private static final String SOURCE_RANGED = "combat:ranged";
	private static final String SOURCE_MELEE_PREFIX = "combat:melee:";

	@Nonnull
	public List<CombatXpGrant> allocateAttackerXp(boolean rangedDamage, @Nonnull CombatStyleType style, double finalDamage) {
		long totalXp = Math.max(0L, Math.round(Math.max(0.0D, finalDamage) * XP_PER_DAMAGE));
		if (totalXp <= 0L) {
			return List.of();
		}

		if (rangedDamage) {
			return List.of(new CombatXpGrant(SkillType.RANGED, totalXp, SOURCE_RANGED));
		}

		if (style.isControlledSplit()) {
			long attackXp = totalXp / 3L;
			long strengthXp = totalXp / 3L;
			long defenseXp = totalXp / 3L;

			long remainder = totalXp - (attackXp + strengthXp + defenseXp);
			if (remainder >= 1L) {
				attackXp++;
			}
			if (remainder >= 2L) {
				strengthXp++;
			}

			List<CombatXpGrant> grants = new ArrayList<>(3);
			grants.add(new CombatXpGrant(SkillType.ATTACK, attackXp, SOURCE_MELEE_PREFIX + "controlled:attack"));
			grants.add(new CombatXpGrant(SkillType.STRENGTH, strengthXp, SOURCE_MELEE_PREFIX + "controlled:strength"));
			grants.add(new CombatXpGrant(SkillType.DEFENSE, defenseXp, SOURCE_MELEE_PREFIX + "controlled:defense"));
			return grants;
		}

		if (style == CombatStyleType.ACCURATE) {
			return List.of(new CombatXpGrant(SkillType.ATTACK, totalXp, SOURCE_MELEE_PREFIX + "accurate"));
		}

		if (style == CombatStyleType.AGGRESSIVE) {
			return List.of(new CombatXpGrant(SkillType.STRENGTH, totalXp, SOURCE_MELEE_PREFIX + "aggressive"));
		}

		return List.of(new CombatXpGrant(SkillType.DEFENSE, totalXp, SOURCE_MELEE_PREFIX + "defensive"));
	}

	public record CombatXpGrant(@Nonnull SkillType skillType, long amount, @Nonnull String sourceTag) {

		@Nonnull
		public String normalizedSourceTag() {
			return this.sourceTag.toLowerCase(Locale.ROOT);
		}
	}
}
