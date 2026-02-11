package org.runetale.skills.domain;

/**
 * Result of a manual crafting attempt through the custom smelting/smithing UI.
 */
public enum CraftResult {

	/** Craft succeeded — inputs consumed, outputs delivered. */
	SUCCESS,

	/** Player does not meet a skill level requirement. */
	LEVEL_TOO_LOW,

	/** Player inventory lacks one or more input materials. */
	MISSING_MATERIALS
}
