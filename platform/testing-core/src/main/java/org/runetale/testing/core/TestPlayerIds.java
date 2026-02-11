package org.runetale.testing.core;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Shared deterministic test player-id helpers.
 */
public final class TestPlayerIds {

	private static final String PREFIX = "runetale:test-player:";

	private TestPlayerIds() {
	}

	/**
	 * Returns a deterministic UUID for a stable player key.
	 */
	public static UUID fromKey(String key) {
		String normalizedKey = key.trim();
		return UUID.nameUUIDFromBytes((PREFIX + normalizedKey).getBytes(StandardCharsets.UTF_8));
	}
}
