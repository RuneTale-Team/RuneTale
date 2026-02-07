package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.OsrsXpService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.SkillNodeRuntimeService;
import org.runetale.skills.service.SkillSessionStatsService;
import org.runetale.skills.service.ToolRequirementEvaluator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles block-break gathering flow:
 * lookup -> requirements -> XP mutation -> optional depletion state.
 */
public class SkillNodeBreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

	private static final Logger LOGGER = Logger.getLogger(SkillNodeBreakBlockSystem.class.getName());

	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final OsrsXpService xpService;
	private final SkillNodeLookupService nodeLookupService;
	private final SkillNodeRuntimeService nodeRuntimeService;
	private final ToolRequirementEvaluator toolRequirementEvaluator;
	private final SkillSessionStatsService sessionStatsService;
	private final Query<EntityStore> query;

	public SkillNodeBreakBlockSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull OsrsXpService xpService,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull SkillNodeRuntimeService nodeRuntimeService,
			@Nonnull ToolRequirementEvaluator toolRequirementEvaluator,
			@Nonnull SkillSessionStatsService sessionStatsService) {
		super(BreakBlockEvent.class);
		this.profileComponentType = profileComponentType;
		this.xpService = xpService;
		this.nodeLookupService = nodeLookupService;
		this.nodeRuntimeService = nodeRuntimeService;
		this.toolRequirementEvaluator = toolRequirementEvaluator;
		this.sessionStatsService = sessionStatsService;
		this.query = Query.and(PlayerRef.getComponentType(), profileComponentType);
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
		BlockType brokenBlockType = event.getBlockType();
		SkillNodeDefinition node = this.nodeLookupService.findByBlockId(brokenBlockType.getId());
		if (node == null) {
			if (looksLikeSkillNodeCandidate(brokenBlockType.getId())) {
				event.setCancelled(true);
				sendPlayerNotification(
						playerRef,
						"[Skills] This resource is not configured yet. Try a supported node.",
						NotificationStyle.Warning);
			}
			return;
		}

		PlayerSkillProfileComponent profile = commandBuffer.getComponent(ref, this.profileComponentType);
		if (profile == null) {
			LOGGER.log(Level.WARNING,
					"Player skill profile missing during break event; skipping skill processing for safety.");
			return;
		}

		SkillType skill = node.getSkillType();
		int levelBefore = profile.getLevel(skill);
		long xpBefore = profile.getExperience(skill);

		if (levelBefore < node.getRequiredSkillLevel()) {
			event.setCancelled(true);
			sendPlayerNotification(playerRef,
					String.format("[Skills] %s level %d/%d (current/required).", formatSkillName(skill),
							levelBefore, node.getRequiredSkillLevel()),
					NotificationStyle.Warning);
			return;
		}

		RequirementCheckResult toolCheck = this.toolRequirementEvaluator.evaluate(event.getItemInHand(),
				node.getRequiredToolKeyword(), node.getRequiredToolTier());
		if (!toolCheck.isSuccess()) {
			event.setCancelled(true);
			sendPlayerNotification(playerRef,
					String.format("[Skills] Tool tier %s/%s (current/required) for %s.",
							toolCheck.getDetectedTier().name().toLowerCase(Locale.ROOT),
							node.getRequiredToolTier().name().toLowerCase(Locale.ROOT),
							node.getRequiredToolKeyword()),
					NotificationStyle.Warning);
			return;
		}

		String worldId = store.getExternalData().getWorld().getName();
		if (this.nodeRuntimeService.isDepleted(worldId, event.getTargetBlock(), node)) {
			event.setCancelled(true);
			sendPlayerNotification(playerRef, "[Skills] This node is depleted. Try again in a bit.", NotificationStyle.Danger);
			return;
		}

		long updatedXp = this.xpService.addXp(xpBefore, node.getExperienceReward());
		int updatedLevel = this.xpService.levelForXp(updatedXp);
		long gainedXp = Math.max(0L, updatedXp - xpBefore);
		long progressCurrent = xpProgressCurrent(updatedLevel, updatedXp);
		long progressRequired = xpProgressRequired(updatedLevel);
		profile.set(skill, updatedXp, updatedLevel);
		commandBuffer.putComponent(ref, this.profileComponentType, profile);
		if (playerRef != null) {
			this.sessionStatsService.recordGain(playerRef.getUuid(), skill, gainedXp);
		}
		sendPlayerNotification(playerRef,
				String.format("[Skills] +%d %s XP (%d/%d current/required, %d total).",
						gainedXp,
						formatSkillName(skill),
						progressCurrent,
						progressRequired,
						updatedXp));
		if (updatedLevel > levelBefore) {
			sendPlayerNotification(playerRef,
					String.format("[Skills] %s level up: %d -> %d.", formatSkillName(skill), levelBefore, updatedLevel),
					NotificationStyle.Success);
			if (playerRef != null) {
				EventTitleUtil.showEventTitleToPlayer(
						playerRef,
						Message.raw(formatSkillName(skill) + " Level Up!"),
						Message.raw("Now level " + updatedLevel),
						true);
			}
		}

		if (node.isDepletes()) {
			// Depletion remains deterministic from data: each successful gather rolls
			// against per-node chance.
			double roll = ThreadLocalRandom.current().nextDouble();
			double chance = node.getDepletionChance();
			boolean willDeplete = roll <= chance;
			if (willDeplete) {
				this.nodeRuntimeService.markDepleted(worldId, event.getTargetBlock(), node);
			}
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
		if (playerRef != null) {
			try {
				NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(text), style);
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "Failed to send skills notification; falling back to chat message.", e);
				playerRef.sendMessage(Message.raw(text));
			}
		}
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skill) {
		String name = skill.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private boolean looksLikeSkillNodeCandidate(@Nonnull String blockId) {
		String lowered = blockId.toLowerCase(Locale.ROOT);
		return lowered.contains("log") || lowered.contains("tree") || lowered.contains("ore") || lowered.contains("rock");
	}

	private long xpProgressCurrent(int level, long totalXp) {
		int safeLevel = Math.max(1, Math.min(99, level));
		if (safeLevel >= 99) {
			return 0L;
		}
		long levelStartXp = this.xpService.xpForLevel(safeLevel);
		long nextLevelXp = this.xpService.xpForLevel(safeLevel + 1);
		long required = Math.max(1L, nextLevelXp - levelStartXp);
		long current = Math.max(0L, totalXp - levelStartXp);
		return Math.min(current, required);
	}

	private long xpProgressRequired(int level) {
		int safeLevel = Math.max(1, Math.min(99, level));
		if (safeLevel >= 99) {
			return 0L;
		}
		long levelStartXp = this.xpService.xpForLevel(safeLevel);
		long nextLevelXp = this.xpService.xpForLevel(safeLevel + 1);
		return Math.max(1L, nextLevelXp - levelStartXp);
	}
}
