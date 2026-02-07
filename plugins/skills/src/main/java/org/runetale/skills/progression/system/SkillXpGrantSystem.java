package org.runetale.skills.progression.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.progression.service.SkillProgressionService;
import org.runetale.skills.service.OsrsXpService;
import org.runetale.skills.service.SkillSessionStatsService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies dispatched XP grant events to player skill profiles.
 */
public class SkillXpGrantSystem extends EntityEventSystem<EntityStore, SkillXpGrantEvent> {

	private static final Logger LOGGER = Logger.getLogger(SkillXpGrantSystem.class.getName());
	private static final int MAX_LEVEL = 99;

	private final SkillProgressionService progressionService;
	private final OsrsXpService xpService;
	private final SkillSessionStatsService sessionStatsService;
	private final Query<EntityStore> query;

	public SkillXpGrantSystem(
			@Nonnull SkillProgressionService progressionService,
			@Nonnull OsrsXpService xpService,
			@Nonnull SkillSessionStatsService sessionStatsService) {
		super(SkillXpGrantEvent.class);
		this.progressionService = progressionService;
		this.xpService = xpService;
		this.sessionStatsService = sessionStatsService;
		this.query = Query.and(PlayerRef.getComponentType());
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull SkillXpGrantEvent event) {

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		SkillXpGrantResult result = this.progressionService.grantExperience(
				commandBuffer,
				ref,
				event.getSkillType(),
				event.getExperience());

		if (result.getGainedExperience() <= 0L) {
			return;
		}

		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
		if (playerRef == null) {
			return;
		}

		this.sessionStatsService.recordGain(playerRef.getUuid(), result.getSkillType(), result.getGainedExperience());
		LOGGER.log(
				Level.FINE,
				String.format(
						"Applied XP grant source=%s skill=%s gain=%d totalXp=%d level=%d",
						event.getSource(),
						result.getSkillType(),
						result.getGainedExperience(),
						result.getUpdatedExperience(),
						result.getUpdatedLevel()));

		if (!event.shouldNotifyPlayer()) {
			return;
		}

		sendPlayerNotification(playerRef,
				String.format(
						"[Skills] +%d %s XP (%d/%d current/required, %d total).",
						result.getGainedExperience(),
						formatSkillName(result.getSkillType()),
						xpProgressCurrent(result.getUpdatedLevel(), result.getUpdatedExperience()),
						xpProgressRequired(result.getUpdatedLevel()),
						result.getUpdatedExperience()));

		if (result.isLevelUp()) {
			sendPlayerNotification(
					playerRef,
					String.format(
							"[Skills] %s level up: %d -> %d.",
							formatSkillName(result.getSkillType()),
							result.getPreviousLevel(),
							result.getUpdatedLevel()),
					NotificationStyle.Success);
			EventTitleUtil.showEventTitleToPlayer(
					playerRef,
					Message.raw(formatSkillName(result.getSkillType()) + " Level Up!"),
					Message.raw("Now level " + result.getUpdatedLevel()),
					true);
		}
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	private void sendPlayerNotification(@Nullable PlayerRef playerRef, @Nonnull String text) {
		sendPlayerNotification(playerRef, text, NotificationStyle.Default);
	}

	private void sendPlayerNotification(@Nullable PlayerRef playerRef, @Nonnull String text,
			@Nonnull NotificationStyle style) {
		if (playerRef == null) {
			return;
		}

		try {
			NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(text), style);
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Failed to send skills notification; falling back to chat message.", e);
			playerRef.sendMessage(Message.raw(text));
		}
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skill) {
		String lowered = skill.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}

	private long xpProgressCurrent(int level, long totalXp) {
		int safeLevel = Math.max(1, Math.min(MAX_LEVEL, level));
		if (safeLevel >= MAX_LEVEL) {
			return 0L;
		}

		long levelStartXp = this.xpService.xpForLevel(safeLevel);
		long nextLevelXp = this.xpService.xpForLevel(safeLevel + 1);
		long required = Math.max(1L, nextLevelXp - levelStartXp);
		long current = Math.max(0L, totalXp - levelStartXp);
		return Math.min(current, required);
	}

	private long xpProgressRequired(int level) {
		int safeLevel = Math.max(1, Math.min(MAX_LEVEL, level));
		if (safeLevel >= MAX_LEVEL) {
			return 0L;
		}

		long levelStartXp = this.xpService.xpForLevel(safeLevel);
		long nextLevelXp = this.xpService.xpForLevel(safeLevel + 1);
		return Math.max(1L, nextLevelXp - levelStartXp);
	}
}
