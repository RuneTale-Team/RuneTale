package org.runetale.skills.service;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OSRS-inspired nonlinear XP/level math service.
 */
public class XpService {

	private static final Logger LOGGER = Logger.getLogger(XpService.class.getName());
	private static final int MAX_LEVEL = 99;
	private static final int[] XP_THRESHOLDS = buildThresholds();

	/**
	 * Returns required XP to reach the given level (1..99).
	 */
	public long xpForLevel(int level) {
		int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
		long threshold = XP_THRESHOLDS[clamped];
		LOGGER.log(Level.FINER, String.format("xpForLevel(%d) -> %d", clamped, threshold));
		return threshold;
	}

	/**
	 * Resolves current level from cumulative XP.
	 */
	public int levelForXp(long xp) {
		long safeXp = Math.max(0L, xp);
		int level = 1;
		for (int i = 2; i <= MAX_LEVEL; i++) {
			if (safeXp >= XP_THRESHOLDS[i]) {
				level = i;
			} else {
				break;
			}
		}
		LOGGER.log(Level.FINER, String.format("levelForXp(%d) -> %d", safeXp, level));
		return level;
	}

	/**
	 * Applies an XP gain and returns the updated total XP.
	 */
	public long addXp(long currentXp, double gainedXp) {
		long safeCurrent = Math.max(0L, currentXp);
		long gain = Math.max(0L, Math.round(gainedXp));
		long updated = safeCurrent + gain;
		LOGGER.log(Level.FINE, String.format("XP mutation: current=%d gain=%d updated=%d", safeCurrent, gain, updated));
		return updated;
	}

	private static int[] buildThresholds() {
		int[] thresholds = new int[MAX_LEVEL + 1];
		int points = 0;
		for (int level = 1; level <= MAX_LEVEL; level++) {
			points += Math.floor((double) level + 300.0 * Math.pow(2.0, (double) level / 7.0));
			if (level == 1) {
				thresholds[level] = 0;
			} else {
				thresholds[level] = points / 4;
			}
		}
		return thresholds;
	}
}
