package org.runetale.skills.progression.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.progression.service.SkillProgressionService;
import org.runetale.skills.service.SkillXpToastHudService;
import org.runetale.skills.service.SkillSessionStatsService;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies dispatched XP grant events to player skill profiles.
 */
public class SkillXpGrantSystem extends EntityEventSystem<EntityStore, SkillXpGrantEvent> {

	private static final Logger LOGGER = Logger.getLogger(SkillXpGrantSystem.class.getName());

	private final SkillProgressionService progressionService;
	private final SkillSessionStatsService sessionStatsService;
	private final SkillXpToastHudService skillXpToastHudService;
	private final Query<EntityStore> query;

	public SkillXpGrantSystem(
			@Nonnull SkillProgressionService progressionService,
			@Nonnull SkillSessionStatsService sessionStatsService,
			@Nonnull SkillXpToastHudService skillXpToastHudService) {
		super(SkillXpGrantEvent.class);
		this.progressionService = progressionService;
		this.sessionStatsService = sessionStatsService;
		this.skillXpToastHudService = skillXpToastHudService;
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

		this.skillXpToastHudService.showXpToast(playerRef, result.getSkillType(), result.getGainedExperience());

		if (result.isLevelUp()) {
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

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skill) {
		String lowered = skill.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}
}
