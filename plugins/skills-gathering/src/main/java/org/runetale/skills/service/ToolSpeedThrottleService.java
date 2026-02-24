package org.runetale.skills.service;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministically throttles damage hits based on configurable tool efficiency.
 */
public final class ToolSpeedThrottleService {

	private static final long DEFAULT_ENTRY_TTL_MILLIS = 30_000L;
	private static final int DEFAULT_MAX_TRACKED_TARGETS = 8_192;

	private final long entryTtlMillis;
	private final int maxTrackedTargets;
	private final Map<ThrottleKey, ThrottleState> stateByKey = new ConcurrentHashMap<>();

	public ToolSpeedThrottleService() {
		this(DEFAULT_ENTRY_TTL_MILLIS, DEFAULT_MAX_TRACKED_TARGETS);
	}

	ToolSpeedThrottleService(long entryTtlMillis, int maxTrackedTargets) {
		this.entryTtlMillis = Math.max(1L, entryTtlMillis);
		this.maxTrackedTargets = Math.max(128, maxTrackedTargets);
	}

	public boolean allowHit(
			@Nonnull UUID playerId,
			@Nonnull String blockId,
			int x,
			int y,
			int z,
			double efficiencyMultiplier,
			long nowMillis) {
		double multiplier = sanitizeMultiplier(efficiencyMultiplier);
		if (multiplier <= 0.0D) {
			return false;
		}

		ThrottleKey key = new ThrottleKey(playerId, normalizeBlockId(blockId), x, y, z);
		if (multiplier >= 1.0D) {
			this.stateByKey.remove(key);
			return true;
		}

		pruneIfNeeded(nowMillis);

		ThrottleState state = this.stateByKey.computeIfAbsent(key, ignored -> new ThrottleState());
		if (nowMillis - state.lastTouchedAt > this.entryTtlMillis) {
			state.accumulatedEfficiency = 0.0D;
		}

		state.lastTouchedAt = nowMillis;
		state.accumulatedEfficiency += multiplier;
		if (state.accumulatedEfficiency >= 1.0D) {
			state.accumulatedEfficiency -= 1.0D;
			return true;
		}
		return false;
	}

	private void pruneIfNeeded(long nowMillis) {
		if (this.stateByKey.size() <= this.maxTrackedTargets) {
			return;
		}

		Iterator<Map.Entry<ThrottleKey, ThrottleState>> iterator = this.stateByKey.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<ThrottleKey, ThrottleState> entry = iterator.next();
			if (nowMillis - entry.getValue().lastTouchedAt > this.entryTtlMillis) {
				iterator.remove();
			}
		}

		if (this.stateByKey.size() <= this.maxTrackedTargets) {
			return;
		}

		iterator = this.stateByKey.entrySet().iterator();
		while (this.stateByKey.size() > this.maxTrackedTargets && iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	private static String normalizeBlockId(@Nonnull String blockId) {
		return blockId.trim().toLowerCase(Locale.ROOT);
	}

	private static double sanitizeMultiplier(double multiplier) {
		if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
			return 0.0D;
		}
		return multiplier;
	}

	private record ThrottleKey(@Nonnull UUID playerId, @Nonnull String blockId, int x, int y, int z) {
	}

	private static final class ThrottleState {
		double accumulatedEfficiency;
		long lastTouchedAt;
	}
}
