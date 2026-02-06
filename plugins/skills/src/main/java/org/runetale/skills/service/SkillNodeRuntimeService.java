package org.runetale.skills.service;

import com.hypixel.hytale.math.vector.Vector3i;
import org.runetale.skills.asset.SkillNodeDefinition;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runtime state holder for node depletion and respawn timing.
 *
 * <p>
 * This service is intentionally separate from event systems to keep mutable
 * runtime state
 * centralized and easy to swap with persistent/distributed backing in the
 * future.
 */
public class SkillNodeRuntimeService {

	private static final Logger LOGGER = Logger.getLogger(SkillNodeRuntimeService.class.getName());

	/**
	 * Map from node instance key to absolute epoch-millis respawn timestamp.
	 */
	private final Map<String, Long> depletedUntilByKey = new ConcurrentHashMap<>();

	/**
	 * Returns true when a node instance is still depleted and not yet respawned.
	 */
	public boolean isDepleted(@Nonnull String worldId, @Nonnull Vector3i blockPosition,
			@Nonnull SkillNodeDefinition definition) {
		String key = key(worldId, blockPosition, definition.getId());
		Long until = depletedUntilByKey.get(key);
		if (until == null) {
			return false;
		}

		long now = System.currentTimeMillis();
		if (now >= until) {
			// Respawn elapsed; clear stale runtime state.
			depletedUntilByKey.remove(key);
			LOGGER.log(Level.FINE, String.format("Node respawned: key=%s now=%d until=%d", key, now, until));
			return false;
		}

		LOGGER.log(Level.FINE, String.format("Node still depleted: key=%s now=%d until=%d", key, now, until));
		return true;
	}

	/**
	 * Marks a node instance as depleted and schedules respawn time in-memory.
	 */
	public void markDepleted(@Nonnull String worldId, @Nonnull Vector3i blockPosition,
			@Nonnull SkillNodeDefinition definition) {
		int respawnSeconds = Math.max(0, definition.getRespawnSeconds());
		if (respawnSeconds == 0) {
			LOGGER.log(Level.FINER,
					String.format("Depletion skipped (respawnSeconds=0): node=%s", definition.getId()));
			return;
		}

		String key = key(worldId, blockPosition, definition.getId());
		long until = System.currentTimeMillis() + (respawnSeconds * 1000L);
		depletedUntilByKey.put(key, until);
		LOGGER.log(Level.INFO, String.format("Node depleted: key=%s respawnAt=%d", key, until));
	}

	@Nonnull
	private static String key(@Nonnull String worldId, @Nonnull Vector3i pos, @Nonnull String nodeId) {
		return worldId + "|" + nodeId + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
