package org.runetale.skills.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Player-selected melee combat mode used to route combat XP.
 */
public enum CombatStyleType {
	ACCURATE,
	AGGRESSIVE,
	DEFENSIVE,
	CONTROLLED;

	@Nonnull
	public String getId() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	@Nonnull
	public String getDisplayName() {
		String lowered = this.getId();
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}

	@Nonnull
	public String describeMeleeXpRouting() {
		return switch (this) {
			case ACCURATE -> "Melee XP -> Attack";
			case AGGRESSIVE -> "Melee XP -> Strength";
			case DEFENSIVE -> "Melee XP -> Defense";
			case CONTROLLED -> "Melee XP -> Attack/Strength/Defense split";
		};
	}

	public boolean isControlledSplit() {
		return this == CONTROLLED;
	}

	@Nullable
	public static CombatStyleType tryParse(@Nullable String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}

		String normalized = raw.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
			case "ATTACK" -> ACCURATE;
			case "STRENGTH" -> AGGRESSIVE;
			case "DEFENSE", "DEFENCE" -> DEFENSIVE;
			default -> tryParseExact(normalized);
		};
	}

	@Nonnull
	public static String validModeHint() {
		return "accurate, aggressive, defensive, controlled";
	}

	@Nullable
	private static CombatStyleType tryParseExact(@Nonnull String normalized) {
		try {
			return CombatStyleType.valueOf(normalized);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
