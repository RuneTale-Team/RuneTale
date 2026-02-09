package org.runetale.skills.service;

import org.runetale.skills.domain.CombatStyleType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks session-scoped player melee combat style selections.
 */
public class CombatStyleService {

	private final Map<UUID, CombatStyleType> styleByPlayer = new ConcurrentHashMap<>();

	@Nonnull
	public CombatStyleType getCombatStyle(@Nonnull UUID playerId) {
		CombatStyleType style = this.styleByPlayer.get(playerId);
		return style == null ? CombatStyleType.ACCURATE : style;
	}

	public void setCombatStyle(@Nonnull UUID playerId, @Nonnull CombatStyleType style) {
		this.styleByPlayer.put(playerId, style);
	}

	public void clear(@Nonnull UUID playerId) {
		this.styleByPlayer.remove(playerId);
	}
}
