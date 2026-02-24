package org.runetale.skills.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolTierTest {

	@Test
	void fromStringResolvesKnownNamesCaseInsensitively() {
		assertThat(ToolTier.fromString("BRONZE")).isEqualTo(ToolTier.BRONZE);
		assertThat(ToolTier.fromString("steel")).isEqualTo(ToolTier.STEEL);
		assertThat(ToolTier.fromString("Adamant")).isEqualTo(ToolTier.ADAMANT);
		assertThat(ToolTier.fromString("rune")).isEqualTo(ToolTier.RUNE);
		assertThat(ToolTier.fromString("dragon")).isEqualTo(ToolTier.DRAGON);
		assertThat(ToolTier.fromString("iron")).isEqualTo(ToolTier.IRON);
	}

	@Test
	void fromStringFallsBackToNoneForUnknownOrBlankValues() {
		assertThat(ToolTier.fromString(null)).isEqualTo(ToolTier.NONE);
		assertThat(ToolTier.fromString("  ")).isEqualTo(ToolTier.NONE);
		assertThat(ToolTier.fromString("wood")).isEqualTo(ToolTier.NONE);
		assertThat(ToolTier.fromString("mythic")).isEqualTo(ToolTier.NONE);
	}
}
