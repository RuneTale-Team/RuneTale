package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.OsrsXpService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.SkillNodeRuntimeService;
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
	private final Query<EntityStore> query;

	public SkillNodeBreakBlockSystem(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull OsrsXpService xpService,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull SkillNodeRuntimeService nodeRuntimeService,
			@Nonnull ToolRequirementEvaluator toolRequirementEvaluator) {
		super(BreakBlockEvent.class);
		this.profileComponentType = profileComponentType;
		this.xpService = xpService;
		this.nodeLookupService = nodeLookupService;
		this.nodeRuntimeService = nodeRuntimeService;
		this.toolRequirementEvaluator = toolRequirementEvaluator;
		this.query = Query.and(PlayerRef.getComponentType(), profileComponentType);
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {

		BlockType brokenBlockType = event.getBlockType();
		SkillNodeDefinition node = this.nodeLookupService.findByBlockId(brokenBlockType.getId());
		if (node == null) {
			// Non-skill block: no-op by design.
			return;
		}

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
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
			sendPlayerMessage(playerRef,
					String.format("[Skills] %s level %d required (you are %d).", formatSkillName(skill),
							node.getRequiredSkillLevel(), levelBefore));
			return;
		}

		RequirementCheckResult toolCheck = this.toolRequirementEvaluator.evaluate(event.getItemInHand(),
				node.getRequiredToolKeyword(), node.getRequiredToolTier());
		if (!toolCheck.isSuccess()) {
			event.setCancelled(true);
			sendPlayerMessage(playerRef,
					String.format("[Skills] You need a %s %s tool.", node.getRequiredToolTier().name().toLowerCase(Locale.ROOT),
							node.getRequiredToolKeyword()));
			return;
		}

		String worldId = store.getExternalData().getWorld().getName();
		if (this.nodeRuntimeService.isDepleted(worldId, event.getTargetBlock(), node)) {
			event.setCancelled(true);
			sendPlayerMessage(playerRef, "[Skills] This node is depleted. Try again in a bit.");
			return;
		}

		long updatedXp = this.xpService.addXp(xpBefore, node.getExperienceReward());
		int updatedLevel = this.xpService.levelForXp(updatedXp);
		long gainedXp = Math.max(0L, updatedXp - xpBefore);
		profile.set(skill, updatedXp, updatedLevel);
		commandBuffer.putComponent(ref, this.profileComponentType, profile);
		sendPlayerMessage(playerRef,
				String.format("[Skills] +%d %s XP (%d total).", gainedXp, formatSkillName(skill), updatedXp));
		if (updatedLevel > levelBefore) {
			sendPlayerMessage(playerRef,
					String.format("[Skills] %s level up: %d -> %d.", formatSkillName(skill), levelBefore, updatedLevel));
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

	private void sendPlayerMessage(@Nullable PlayerRef playerRef, @Nonnull String text) {
		if (playerRef != null) {
			playerRef.sendMessage(Message.raw(text));
		}
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skill) {
		String name = skill.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
