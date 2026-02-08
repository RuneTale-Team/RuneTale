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
	DEFENSE,
	RANGED,
	WOODCUTTING,
	MINING;

	/**
	 * Parses a skill identity from resource/config text in a forgiving,
	 * case-insensitive way.
	 *
	 * <p>
	 * Unknown values intentionally fall back to WOODCUTTING so malformed resource
	 * data does not crash server startup.
	 */
	public static SkillType fromString(String raw) {
		SkillType parsed = tryParseStrict(raw);
		return parsed == null ? WOODCUTTING : parsed;
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

		try {
			return SkillType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
