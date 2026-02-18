package org.runetale.skills.service;

import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks session-scoped skill telemetry for UI feedback.
 */
public class SkillSessionStatsService {

	private final Map<UUID, Long> mostRecentGainByPlayer = new ConcurrentHashMap<>();
	private final Map<UUID, SkillType> mostRecentSkillByPlayer = new ConcurrentHashMap<>();

	public void recordGain(@Nonnull UUID playerId, @Nonnull SkillType skillType, long gainedXp) {
		this.mostRecentGainByPlayer.put(playerId, Math.max(0L, gainedXp));
		this.mostRecentSkillByPlayer.put(playerId, skillType);
	}

	public long getMostRecentGain(@Nonnull UUID playerId) {
		Long gain = this.mostRecentGainByPlayer.get(playerId);
		return gain == null ? 0L : Math.max(0L, gain);
	}

	@Nullable
	public SkillType getMostRecentSkill(@Nonnull UUID playerId) {
		return this.mostRecentSkillByPlayer.get(playerId);
	}

	public void clear(@Nonnull UUID playerId) {
		this.mostRecentGainByPlayer.remove(playerId);
		this.mostRecentSkillByPlayer.remove(playerId);
	}
}
