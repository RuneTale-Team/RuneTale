package org.runetale.skills.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillTypeTest {

	@Test
	void tryParseStrictParsesValidIdsCaseInsensitively() {
		assertEquals(SkillType.MINING, SkillType.tryParseStrict("MINING"));
		assertEquals(SkillType.WOODCUTTING, SkillType.tryParseStrict("woodcutting"));
		assertEquals(SkillType.ATTACK, SkillType.tryParseStrict(" attack "));
		assertEquals(SkillType.SMITHING, SkillType.tryParseStrict("smithing"));
		assertEquals(SkillType.DEFENCE, SkillType.tryParseStrict("defence"));
		assertEquals(SkillType.PRAYER, SkillType.tryParseStrict("Prayer"));
		assertEquals(SkillType.FIREMAKING, SkillType.tryParseStrict("firemaking"));
	}

	@Test
	void tryParseStrictReturnsNullForMissingOrUnknownValues() {
		assertNull(SkillType.tryParseStrict(null));
		assertNull(SkillType.tryParseStrict(""));
		assertNull(SkillType.tryParseStrict("   "));
		assertNull(SkillType.tryParseStrict("FISHING"));
	}

	@Test
	void fromStringThrowsForMissingOrUnknownValues() {
		assertThrows(IllegalArgumentException.class, () -> SkillType.fromString(null));
		assertThrows(IllegalArgumentException.class, () -> SkillType.fromString(""));
		assertThrows(IllegalArgumentException.class, () -> SkillType.fromString("FISHING"));
	}
}
