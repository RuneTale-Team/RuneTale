package org.runetale.skills.domain;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Enumerates all supported skill identities.
 *
 * <p>
 * This enum is intentionally centralized so new skills (e.g. Mining/Fishing)
 * can be added without changing persistence shape or system architecture.
 */
public enum SkillType {
	ATTACK,
	STRENGTH,
	DEFENCE,
	RANGED,
	WOODCUTTING,
	MINING,
	SMITHING;

	/**
	 * Parses a skill identity from resource/config text in a strict,
	 * case-insensitive way.
	 *
	 * <p>
	 * Missing or unknown values throw to prevent silent fallback behavior.
	 */
	public static SkillType fromString(String raw) {
		SkillType parsed = tryParseStrict(raw);
		if (parsed == null) {
			throw new IllegalArgumentException("Unknown skill id: " + raw);
		}
		return parsed;
	}

	/**
	 * Parses a skill identity without fallback behavior.
	 *
	 * <p>
	 * Returns null for missing/unknown values so callers can reject invalid input
	 * explicitly.
	 */
	@Nullable
	public static SkillType tryParseStrict(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}

		String normalized = raw.trim().toUpperCase(Locale.ROOT);
		if ("DEFENSE".equals(normalized)) {
			return DEFENCE;
		}

		try {
			return SkillType.valueOf(normalized);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
