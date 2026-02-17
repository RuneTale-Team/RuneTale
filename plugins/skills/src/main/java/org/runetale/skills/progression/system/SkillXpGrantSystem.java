package org.runetale.skills.progression.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.event.SkillLevelUpEvent;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.progression.service.SkillProgressionService;
import org.runetale.skills.service.DebugModeService;
import org.runetale.skills.service.SkillXpToastHudService;
import org.runetale.skills.service.SkillSessionStatsService;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Applies dispatched XP grant events to player skill profiles.
 */
public class SkillXpGrantSystem extends EntityEventSystem<EntityStore, SkillXpGrantEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final SkillProgressionService progressionService;
	private final SkillSessionStatsService sessionStatsService;
	private final SkillXpToastHudService skillXpToastHudService;
	private final DebugModeService debugModeService;
	private final Query<EntityStore> query;

	public SkillXpGrantSystem(
			@Nonnull SkillProgressionService progressionService,
			@Nonnull SkillSessionStatsService sessionStatsService,
			@Nonnull SkillXpToastHudService skillXpToastHudService,
			@Nonnull DebugModeService debugModeService) {
		super(SkillXpGrantEvent.class);
		this.progressionService = progressionService;
		this.sessionStatsService = sessionStatsService;
		this.skillXpToastHudService = skillXpToastHudService;
		this.debugModeService = debugModeService;
		this.query = Query.and(PlayerRef.getComponentType());
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull SkillXpGrantEvent event) {

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		if (isSkillsDebugEnabled()) {
			LOGGER.atInfo().log("[Skills][Diag] Processing XP grant event skill=%s xp=%.4f source=%s notify=%s",
					event.getSkillType(),
					event.getExperience(),
					event.getSource(),
					event.shouldNotifyPlayer());
		}

		SkillXpGrantResult result = this.progressionService.grantExperience(
				commandBuffer,
				ref,
				event.getSkillType(),
				event.getExperience());

		if (result.getGainedExperience() <= 0L) {
			if (isSkillsDebugEnabled()) {
				LOGGER.atInfo().log("[Skills][Diag] XP grant produced no gain skill=%s source=%s requestedXp=%.4f",
						event.getSkillType(),
						event.getSource(),
						event.getExperience());
			}
			return;
		}

		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
		if (playerRef == null) {
			if (isSkillsDebugEnabled()) {
				LOGGER.atWarning().log("[Skills][Diag] XP grant applied but PlayerRef missing skill=%s source=%s gain=%d",
						result.getSkillType(),
						event.getSource(),
						result.getGainedExperience());
			}
			return;
		}

		this.sessionStatsService.recordGain(playerRef.getUuid(), result.getSkillType(), result.getGainedExperience());
		LOGGER.atFine().log("Applied XP grant source=%s skill=%s gain=%d totalXp=%d level=%d",
				event.getSource(),
				result.getSkillType(),
				result.getGainedExperience(),
				result.getUpdatedExperience(),
				result.getUpdatedLevel());

		if (!event.shouldNotifyPlayer()) {
			return;
		}

		this.skillXpToastHudService.showXpToast(
				playerRef,
				result.getSkillType(),
				result.getGainedExperience(),
				result.isLevelUp());

		if (result.isLevelUp()) {
			EventTitleUtil.showEventTitleToPlayer(
					playerRef,
					Message.raw(formatSkillName(result.getSkillType()) + " Level Up!"),
					Message.raw("Now level " + result.getUpdatedLevel()),
					true);

			commandBuffer.invoke(ref, new SkillLevelUpEvent(
					result.getSkillType(), result.getPreviousLevel(), result.getUpdatedLevel()));
		}

		if (isSkillsDebugEnabled()) {
			LOGGER.atInfo().log("[Skills][Diag] XP grant applied source=%s skill=%s gain=%d totalXp=%d prevLevel=%d newLevel=%d",
					event.getSource(),
					result.getSkillType(),
					result.getGainedExperience(),
					result.getUpdatedExperience(),
					result.getPreviousLevel(),
					result.getUpdatedLevel());
		}
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skill) {
		String lowered = skill.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}

	private boolean isSkillsDebugEnabled() {
		return this.debugModeService.isEnabled("skills");
	}
}
