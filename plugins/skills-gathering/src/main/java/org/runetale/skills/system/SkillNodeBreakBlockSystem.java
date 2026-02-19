package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.config.HeuristicsConfig;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.GatheringBypassService;
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

	private final SkillsRuntimeApi runtimeApi;
	private final SkillNodeLookupService nodeLookupService;
	private final HeuristicsConfig heuristicsConfig;
	private final GatheringBypassService bypassService;
	private final String debugPluginKey;
	private final Query<EntityStore> query;

	public SkillNodeBreakBlockSystem(
			@Nonnull SkillsRuntimeApi runtimeApi,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull HeuristicsConfig heuristicsConfig,
			@Nonnull GatheringBypassService bypassService,
			@Nonnull String debugPluginKey) {
		super(BreakBlockEvent.class);
		this.runtimeApi = runtimeApi;
		this.nodeLookupService = nodeLookupService;
		this.heuristicsConfig = heuristicsConfig;
		this.bypassService = bypassService;
		this.debugPluginKey = debugPluginKey;
		this.query = Query.and(PlayerRef.getComponentType());
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
		Player player = commandBuffer.getComponent(ref, Player.getComponentType());
		if (player == null) {
			player = store.getComponent(ref, Player.getComponentType());
		}
		boolean bypassActive = isBreakGateBypassed(player);
		String bypassMode = bypassMode(player);
		BlockType brokenBlockType = event.getBlockType();
		if (isSkillsDebugEnabled()) {
			LOGGER.atInfo().log("[Skills][Diag] Break event received blockId=%s cancelled=%s player=%s bypass=%s",
					brokenBlockType.getId(),
					event.isCancelled(),
					playerRef == null ? "<missing>" : playerRef.getUuid(),
					bypassMode);
		}

		SkillNodeDefinition node = this.nodeLookupService.findByBlockId(brokenBlockType.getId());
		if (node == null) {
			if (isSkillsDebugEnabled()) {
				LOGGER.atInfo().log("[Skills][Diag] Node lookup miss blockId=%s candidate=%s",
						brokenBlockType.getId(),
						looksLikeSkillNodeCandidate(brokenBlockType.getId()));
			}
			if (looksLikeSkillNodeCandidate(brokenBlockType.getId())) {
				if (bypassActive) {
					if (isSkillsDebugEnabled()) {
						LOGGER.atInfo().log("[Skills][Diag] Bypass allowed unconfigured node-like block mode=%s block=%s player=%s",
								bypassMode,
								brokenBlockType.getId(),
								playerRef == null ? "<missing>" : playerRef.getUuid());
					}
					return;
				}
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

		if (!this.runtimeApi.hasSkillProfile(commandBuffer, ref)) {
			LOGGER.atWarning().log(
					"Player skill profile missing during break event; skipping skill processing for safety.");
			if (isSkillsDebugEnabled()) {
				LOGGER.atWarning().log("[Skills][Diag] Break event aborted due to missing profile blockId=%s player=%s",
						brokenBlockType.getId(),
						playerRef == null ? "<missing>" : playerRef.getUuid());
			}
			return;
		}

		SkillType skill = node.getSkillType();
		int levelBefore = this.runtimeApi.getSkillLevel(commandBuffer, ref, skill);

		if (levelBefore < node.getRequiredSkillLevel() && !bypassActive) {
			if (isSkillsDebugEnabled()) {
				LOGGER.atInfo().log("[Skills][Diag] Level gate blocked break node=%s skill=%s level=%d required=%d block=%s",
						node.getId(),
						skill,
						levelBefore,
						node.getRequiredSkillLevel(),
						brokenBlockType.getId());
			}
			event.setCancelled(true);
			sendPlayerNotification(playerRef,
					String.format("[Skills] %s level %d/%d (current/required).", formatSkillName(skill),
							levelBefore, node.getRequiredSkillLevel()),
					NotificationStyle.Warning);
			return;
		}
		if (levelBefore < node.getRequiredSkillLevel() && isSkillsDebugEnabled()) {
			LOGGER.atInfo().log("[Skills][Diag] Bypass ignored level gate mode=%s node=%s skill=%s level=%d required=%d block=%s",
					bypassMode,
					node.getId(),
					skill,
					levelBefore,
					node.getRequiredSkillLevel(),
					brokenBlockType.getId());
		}

		boolean queued = this.runtimeApi.grantSkillXp(
				commandBuffer,
				ref,
				skill,
				node.getExperienceReward(),
				"node:" + node.getId(),
				true);
		if (isSkillsDebugEnabled()) {
			LOGGER.atInfo().log("[Skills][Diag] Break XP dispatch node=%s skill=%s xp=%.4f queued=%s block=%s",
					node.getId(),
					skill,
					node.getExperienceReward(),
					queued,
					brokenBlockType.getId());
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

	private boolean isSkillsDebugEnabled() {
		return this.runtimeApi.isDebugEnabled(this.debugPluginKey);
	}

	private boolean isBreakGateBypassed(@Nullable Player player) {
		return isCreativeExempt(player) || isOpExempt(player);
	}

	@Nonnull
	private String bypassMode(@Nullable Player player) {
		if (isCreativeExempt(player)) {
			return "creative";
		}
		if (isOpExempt(player)) {
			return "op";
		}
		return "none";
	}

	private boolean isCreativeExempt(@Nullable Player player) {
		return player != null && player.getGameMode() == GameMode.Creative;
	}

	private boolean isOpExempt(@Nullable Player player) {
		return player != null
				&& this.bypassService.isOpExemptionEnabled()
				&& player.hasPermission(GatheringBypassService.OP_EXEMPT_PERMISSION);
	}

}
