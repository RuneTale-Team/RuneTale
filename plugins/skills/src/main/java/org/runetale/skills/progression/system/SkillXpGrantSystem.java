package org.runetale.skills.progression.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.service.CommandBufferPlayerRefResolver;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.progression.service.EventTitleSkillLevelUpAnnouncer;
import org.runetale.skills.progression.service.PlayerRefResolver;
import org.runetale.skills.progression.service.SkillXpGrantFeedbackService;
import org.runetale.skills.progression.service.SkillProgressionService;
import org.runetale.skills.service.SkillXpToastHudService;
import org.runetale.skills.service.SkillSessionStatsService;

import javax.annotation.Nonnull;

/**
 * Applies dispatched XP grant events to player skill profiles.
 */
public class SkillXpGrantSystem extends EntityEventSystem<EntityStore, SkillXpGrantEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final SkillProgressionService progressionService;
	private final SkillSessionStatsService sessionStatsService;
	private final SkillXpGrantFeedbackService skillXpGrantFeedbackService;
	private final PlayerRefResolver playerRefResolver;
	private final Query<EntityStore> query;

	public SkillXpGrantSystem(
			@Nonnull SkillProgressionService progressionService,
			@Nonnull SkillSessionStatsService sessionStatsService,
			@Nonnull SkillXpToastHudService skillXpToastHudService) {
		this(
				progressionService,
				sessionStatsService,
				new SkillXpGrantFeedbackService(skillXpToastHudService, new EventTitleSkillLevelUpAnnouncer()));
	}

	public SkillXpGrantSystem(
			@Nonnull SkillProgressionService progressionService,
			@Nonnull SkillSessionStatsService sessionStatsService,
			@Nonnull SkillXpGrantFeedbackService skillXpGrantFeedbackService) {
		this(
				progressionService,
				sessionStatsService,
				skillXpGrantFeedbackService,
				new CommandBufferPlayerRefResolver(),
				Query.and(PlayerRef.getComponentType()));
	}

	public SkillXpGrantSystem(
			@Nonnull SkillProgressionService progressionService,
			@Nonnull SkillSessionStatsService sessionStatsService,
			@Nonnull SkillXpGrantFeedbackService skillXpGrantFeedbackService,
			@Nonnull PlayerRefResolver playerRefResolver,
			@Nonnull Query<EntityStore> query) {
		super(SkillXpGrantEvent.class);
		this.progressionService = progressionService;
		this.sessionStatsService = sessionStatsService;
		this.skillXpGrantFeedbackService = skillXpGrantFeedbackService;
		this.playerRefResolver = playerRefResolver;
		this.query = query;
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

		PlayerRef playerRef = this.playerRefResolver.resolve(commandBuffer, ref);
		if (playerRef == null) {
			return;
		}

		this.sessionStatsService.recordGain(playerRef.getUuid(), result.getSkillType(), result.getGainedExperience());
		LOGGER.atFine().log("Applied XP grant source=%s skill=%s gain=%d totalXp=%d level=%d",
				event.getSource(),
				result.getSkillType(),
				result.getGainedExperience(),
				result.getUpdatedExperience(),
				result.getUpdatedLevel());

		this.skillXpGrantFeedbackService.apply(commandBuffer, ref, playerRef, event, result);
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}
}
