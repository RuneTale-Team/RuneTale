package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.config.HeuristicsConfig;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.SkillNodeLookupService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

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
		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
		BlockType brokenBlockType = event.getBlockType();
		SkillNodeDefinition node = this.nodeLookupService.findByBlockId(brokenBlockType.getId());
		if (node == null) {
			if (looksLikeSkillNodeCandidate(brokenBlockType.getId())) {
				LOGGER.atWarning().log(
						"[Skills] Unconfigured node-like block encountered id=%s. Add matching blockId/blockIds in Skills/Nodes.",
						brokenBlockType.getId());
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
				skill,
				node.getExperienceReward(),
				"node:" + node.getId(),
				true);
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
				LOGGER.atFine().withCause(e).log("Failed to send skills notification; falling back to chat message.");
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
		for (String token : this.heuristicsConfig.nodeCandidateTokens()) {
			if (lowered.contains(token)) {
				return true;
			}
		}
		return false;
	}

}
