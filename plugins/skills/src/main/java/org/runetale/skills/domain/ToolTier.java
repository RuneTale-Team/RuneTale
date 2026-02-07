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
	WOOD(1),
	CRUDE(2),
	COPPER(3),
	IRON(4),
	THORIUM(5),
	COBALT(6),
	ADAMANTITE(7),
	ONYXIUM(8),
	MITHRIL(9);

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

		String normalized = raw.trim().toUpperCase(Locale.ROOT);
		switch (normalized) {
			case "BRONZE":
				return WOOD;
			case "STEEL":
				return CRUDE;
			case "ADAMANT":
				return ADAMANTITE;
			case "RUNE":
				return ONYXIUM;
			case "DRAGON":
			case "CRYSTAL":
				return MITHRIL;
			default:
				break;
		}

		try {
			return ToolTier.valueOf(normalized);
		} catch (IllegalArgumentException ignored) {
			// Fail-safe: unknown configuration should not crash runtime.
			return NONE;
		}
	}
}
