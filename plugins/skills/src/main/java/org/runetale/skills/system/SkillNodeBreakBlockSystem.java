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
import org.runetale.skills.config.ToolingConfig;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.SkillNodeBreakResolutionResult;
import org.runetale.skills.service.SkillNodeBreakResolutionService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.ToolRequirementEvaluator;

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
	private final ToolRequirementEvaluator toolRequirementEvaluator;
	private final SkillNodeBreakResolutionService skillNodeBreakResolutionService;
	private final SkillNodePlayerRefResolver playerRefResolver;
	private final SkillNodePlayerNotifier skillNodePlayerNotifier;
	private final Query<EntityStore> query;

	public SkillNodeBreakBlockSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull SkillXpDispatchService skillXpDispatchService,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull HeuristicsConfig heuristicsConfig,
			@Nonnull ToolingConfig toolingConfig) {
		this(
				profileComponentType,
				skillXpDispatchService,
				nodeLookupService,
				new ToolRequirementEvaluator(toolingConfig),
				new SkillNodeBreakResolutionService(heuristicsConfig.nodeCandidateTokens()),
				new CommandBufferSkillNodePlayerRefResolver(),
				new NotificationUtilSkillNodePlayerNotifier(),
				Query.and(PlayerRef.getComponentType(), profileComponentType));
	}

	public SkillNodeBreakBlockSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull SkillXpDispatchService skillXpDispatchService,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull ToolRequirementEvaluator toolRequirementEvaluator,
			@Nonnull SkillNodeBreakResolutionService skillNodeBreakResolutionService,
			@Nonnull SkillNodePlayerRefResolver playerRefResolver,
			@Nonnull SkillNodePlayerNotifier skillNodePlayerNotifier,
			@Nonnull Query<EntityStore> query) {
		super(BreakBlockEvent.class);
		this.profileComponentType = profileComponentType;
		this.skillXpDispatchService = skillXpDispatchService;
		this.nodeLookupService = nodeLookupService;
		this.toolRequirementEvaluator = toolRequirementEvaluator;
		this.skillNodeBreakResolutionService = skillNodeBreakResolutionService;
		this.playerRefResolver = playerRefResolver;
		this.skillNodePlayerNotifier = skillNodePlayerNotifier;
		this.query = query;
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

		int levelBefore = profile.getLevel(node.getSkillType());
		RequirementCheckResult toolCheck = this.toolRequirementEvaluator.evaluate(event.getItemInHand(),
				node.getRequiredToolKeyword(), node.getRequiredToolTier());
		SkillNodeBreakResolutionResult resolution = this.skillNodeBreakResolutionService
				.resolveConfiguredNode(node, levelBefore, toolCheck);
		applyResolution(event, playerRef, resolution);
		if (!resolution.shouldDispatchXp()) {
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
}
