package org.runetale.skills.domain;

import java.util.Locale;

/**
 * Enumerates all supported skill identities.
 *
 * <p>
 * This enum is intentionally centralized so new skills (e.g. Mining/Fishing)
 * can be added without changing persistence shape or system architecture.
 */
public enum SkillType {
	WOODCUTTING,
	MINING,
	FISHING;

	/**
	 * Parses a skill identity from resource/config text in a forgiving,
	 * case-insensitive way.
	 *
	 * <p>
	 * Unknown values intentionally fall back to WOODCUTTING so malformed resource
	 * data does not crash server startup.
	 */
	public static SkillType fromString(String raw) {
		if (raw == null || raw.isBlank()) {
			return WOODCUTTING;
		}

		try {
			return SkillType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return WOODCUTTING;
		}
	}
}
