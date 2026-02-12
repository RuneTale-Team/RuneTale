package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.config.HeuristicsConfig;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.SkillNodeBreakResolutionResult;
import org.runetale.skills.service.SkillNodeBreakResolutionService;
import org.runetale.skills.service.SkillNodeLookupService;

import javax.annotation.Nonnull;

/**
 * Handles block-break gathering flow:
 * lookup -> requirements -> XP dispatch.
 */
public class SkillNodeBreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final SkillXpDispatchService skillXpDispatchService;
	private final SkillNodeLookupService nodeLookupService;
	private final HeuristicsConfig heuristicsConfig;
	private final Query<EntityStore> query;

	public SkillNodeBreakBlockSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull SkillXpDispatchService skillXpDispatchService,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull HeuristicsConfig heuristicsConfig) {
		super(BreakBlockEvent.class);
		this.profileComponentType = profileComponentType;
		this.skillXpDispatchService = skillXpDispatchService;
		this.nodeLookupService = nodeLookupService;
		this.heuristicsConfig = heuristicsConfig;
		this.query = Query.and(PlayerRef.getComponentType(), profileComponentType);
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		PlayerRef playerRef = this.playerRefResolver.resolve(commandBuffer, ref);
		BlockType brokenBlockType = event.getBlockType();
		String blockId = brokenBlockType.getId();
		SkillNodeDefinition node = this.nodeLookupService.findByBlockId(blockId);
		if (node == null) {
			SkillNodeBreakResolutionResult missingNodeResult = this.skillNodeBreakResolutionService.resolveMissingNode(blockId);
			applyResolution(event, playerRef, missingNodeResult);
			return;
		}

		PlayerSkillProfileComponent profile = commandBuffer.getComponent(ref, this.profileComponentType);
		if (profile == null) {
			LOGGER.atWarning().log(
					"Player skill profile missing during break event; skipping skill processing for safety.");
			return;
		}

		SkillType skill = node.getSkillType();
		int levelBefore = profile.getLevel(skill);

		if (levelBefore < node.getRequiredSkillLevel()) {
			event.setCancelled(true);
			sendPlayerNotification(playerRef,
					String.format("[Skills] %s level %d/%d (current/required).", formatSkillName(skill),
							levelBefore, node.getRequiredSkillLevel()),
					NotificationStyle.Warning);
			return;
		}

		this.skillXpDispatchService.grantSkillXp(
				commandBuffer,
				ref,
				resolution.getSkillType(),
				resolution.getExperience(),
				resolution.getSourceTag(),
				true);
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	private void applyResolution(@Nonnull BreakBlockEvent event, PlayerRef playerRef,
			@Nonnull SkillNodeBreakResolutionResult resolution) {
		if (resolution.shouldCancelBreak()) {
			event.setCancelled(true);
		}

		if (resolution.shouldNotifyPlayer()) {
			this.skillNodePlayerNotifier.notify(playerRef, resolution.getPlayerMessage(), resolution.getNotificationStyle());
		}
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skill) {
		String name = skill.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private boolean looksLikeSkillNodeCandidate(@Nonnull String blockId) {
		String lowered = blockId.toLowerCase(Locale.ROOT);
		for (String token : this.heuristicsConfig.nodeCandidateTokens()) {
			if (lowered.contains(token)) {
				return true;
			}
		}
		return false;
	}

}
