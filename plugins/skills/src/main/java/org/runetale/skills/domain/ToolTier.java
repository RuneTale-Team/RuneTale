package org.runetale.skills.domain;

import java.util.Locale;

/**
 * Tool quality tiers used by gathering requirement checks.
 *
 * <p>
 * The numeric rank enables straightforward >= comparisons for strict
 * minimum-tier checks.
 */
public enum ToolTier {
	NONE(0),
	BRONZE(1),
	IRON(2),
	STEEL(3),
	MITHRIL(4),
	ADAMANT(5),
	RUNE(6),
	DRAGON(7),
	CRYSTAL(8);

	private final int rank;

	ToolTier(int rank) {
		this.rank = rank;
	}

	/**
	 * Returns a sortable rank for strict minimum-tier evaluation.
	 */
	public int rank() {
		return rank;
	}

	/**
	 * Parses a tier from asset/config text in a forgiving, case-insensitive way.
	 */
	public static ToolTier fromString(String raw) {
		if (raw == null || raw.isBlank()) {
			return NONE;
		}

		try {
			return ToolTier.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			// Fail-safe: unknown configuration should not crash runtime.
			return NONE;
		}
	}
}
