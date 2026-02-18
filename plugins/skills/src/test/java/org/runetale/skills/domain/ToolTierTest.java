package org.runetale.skills.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolTierTest {

	@Test
	void fromStringResolvesAliasesAndKnownNamesCaseInsensitively() {
		assertThat(ToolTier.fromString("BRONZE")).isEqualTo(ToolTier.WOOD);
		assertThat(ToolTier.fromString("steel")).isEqualTo(ToolTier.CRUDE);
		assertThat(ToolTier.fromString("Adamant")).isEqualTo(ToolTier.ADAMANTITE);
		assertThat(ToolTier.fromString("rune")).isEqualTo(ToolTier.ONYXIUM);
		assertThat(ToolTier.fromString("dragon")).isEqualTo(ToolTier.MITHRIL);
		assertThat(ToolTier.fromString("iron")).isEqualTo(ToolTier.IRON);
	}

	@Test
	void fromStringFallsBackToNoneForUnknownOrBlankValues() {
		assertThat(ToolTier.fromString(null)).isEqualTo(ToolTier.NONE);
		assertThat(ToolTier.fromString("  ")).isEqualTo(ToolTier.NONE);
		assertThat(ToolTier.fromString("mythic")).isEqualTo(ToolTier.NONE);
	}
}
