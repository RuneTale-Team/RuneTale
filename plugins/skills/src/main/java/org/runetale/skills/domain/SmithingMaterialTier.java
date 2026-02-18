package org.runetale.skills.domain;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Material tiers shared by smelting (furnace) and smithing (anvil) UIs.
 *
 * <p>
 * Each tier maps to an anvil bench category ({@code "RuneTale_Anvil_<DisplayName>"})
 * and furnace recipes whose output item ID contains {@code "Bar_<DisplayName>"}.
 * The {@code smithingLevelHint} is visual-only context for the UI; actual
 * requirements come from recipe tags.
 */
public enum SmithingMaterialTier {
	BRONZE("Bronze", 1),
	IRON("Iron", 15),
	STEEL("Steel", 30),
	MITHRIL("Mithril", 50),
	ADAMANTITE("Adamantite", 70),
	RUNITE("Runite", 85);

	private final String displayName;
	private final int smithingLevelHint;

	SmithingMaterialTier(@Nonnull String displayName, int smithingLevelHint) {
		this.displayName = displayName;
		this.smithingLevelHint = Math.max(1, smithingLevelHint);
	}

	@Nonnull
	public String getDisplayName() {
		return displayName;
	}

	public int getSmithingLevelHint() {
		return smithingLevelHint;
	}

	/**
	 * Returns the anvil bench category ID for this tier
	 * (e.g. {@code "RuneTale_Anvil_Bronze"}).
	 */
	@Nonnull
	public String getAnvilCategory() {
		return "RuneTale_Anvil_" + displayName;
	}

	/**
	 * Returns the substring matched against furnace recipe output item IDs
	 * (e.g. {@code "Bar_Bronze"}).
	 */
	@Nonnull
	public String getBarSubstring() {
		return "Bar_" + displayName;
	}

	/**
	 * Returns the icon texture path for this tier's tab button.
	 */
	@Nonnull
	public String getIconPath() {
		return "SkillsPlugin/Assets/Icons/icon_smithing.png";
	}

	/**
	 * Returns the tier name formatted for display in section titles
	 * (e.g. {@code "Bronze Bars"} for smelting, {@code "Bronze Equipment"} for smithing).
	 */
	@Nonnull
	public String getSectionTitle(@Nonnull String suffix) {
		return displayName + " " + suffix;
	}
}
