package org.runetale.skills.service;

import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.skills.config.XpConfig;
import org.runetale.skills.config.XpRoundingMode;

import javax.annotation.Nonnull;

/**
 * OSRS-inspired nonlinear XP/level math service.
 */
public class XpService {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final XpConfig config;
	private final int maxLevel;
	private final int[] xpThresholds;

	public XpService(@Nonnull XpConfig config) {
		this.config = config;
		this.maxLevel = config.maxLevel();
		this.xpThresholds = buildThresholds(config);
	}

	/**
	 * Returns required XP to reach the given level.
	 */
	public long xpForLevel(int level) {
		int clamped = Math.max(1, Math.min(this.maxLevel, level));
		long threshold = this.xpThresholds[clamped];
		LOGGER.atFiner().log("xpForLevel(%d) -> %d", clamped, threshold);
		return threshold;
	}

	/**
	 * Resolves current level from cumulative XP.
	 */
	public int levelForXp(long xp) {
		long safeXp = Math.max(0L, xp);
		int level = 1;
		for (int i = 2; i <= this.maxLevel; i++) {
			if (safeXp >= this.xpThresholds[i]) {
				level = i;
			} else {
				break;
			}
		}
		LOGGER.atFiner().log("levelForXp(%d) -> %d", safeXp, level);
		return level;
	}

	/**
	 * Applies an XP gain and returns the updated total XP.
	 */
	public long addXp(long currentXp, double gainedXp) {
		long safeCurrent = Math.max(0L, currentXp);
		long gain = Math.max(0L, roundByMode(gainedXp, this.config.roundingMode()));
		long updated = safeCurrent + gain;
		LOGGER.atFine().log("XP mutation: current=%d gain=%d updated=%d", safeCurrent, gain, updated);
		return updated;
	}

	public int getMaxLevel() {
		return this.maxLevel;
	}

	private static int[] buildThresholds(@Nonnull XpConfig config) {
		int[] thresholds = new int[config.maxLevel() + 1];
		thresholds[1] = 0;
		int points = 0;
		for (int level = 2; level <= config.maxLevel(); level++) {
			int previousLevel = level - 1;
			points += (int) Math.floor(
					(config.levelTermMultiplier() * (double) previousLevel)
							+ config.growthScale() * Math.pow(config.growthBase(), (double) previousLevel / config.growthDivisor()));
			thresholds[level] = points / config.pointsDivisor();
		}
		return thresholds;
	}

	private static long roundByMode(double value, @Nonnull XpRoundingMode roundingMode) {
		if (roundingMode == XpRoundingMode.FLOOR) {
			return (long) Math.floor(value);
		}
		if (roundingMode == XpRoundingMode.CEIL) {
			return (long) Math.ceil(value);
		}

		return Math.round(value);
	}
}
