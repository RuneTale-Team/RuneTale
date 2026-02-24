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
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.config.HeuristicsConfig;
import org.runetale.skills.config.ToolingConfig;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;
import org.runetale.skills.service.GatheringBypassService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.ToolRequirementEvaluator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles early gather gating on block-hit (damage) interactions.
 */
public class SkillNodeDamageBlockGateSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final long NOTIFICATION_COOLDOWN_MILLIS = 1500L;

	private final SkillsRuntimeApi runtimeApi;
	private final SkillNodeLookupService nodeLookupService;
	private final HeuristicsConfig heuristicsConfig;
	private final ToolingConfig toolingConfig;
	private final ToolRequirementEvaluator toolRequirementEvaluator;
	private final GatheringBypassService bypassService;
	private final String debugPluginKey;
	private final Query<EntityStore> query;
	private final Map<UUID, Long> lastNoticeByPlayer = new ConcurrentHashMap<>();

	public SkillNodeDamageBlockGateSystem(
			@Nonnull SkillsRuntimeApi runtimeApi,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull HeuristicsConfig heuristicsConfig,
			@Nonnull ToolingConfig toolingConfig,
			@Nonnull ToolRequirementEvaluator toolRequirementEvaluator,
			@Nonnull GatheringBypassService bypassService,
			@Nonnull String debugPluginKey) {
		super(DamageBlockEvent.class);
		this.runtimeApi = runtimeApi;
		this.nodeLookupService = nodeLookupService;
		this.heuristicsConfig = heuristicsConfig;
		this.toolingConfig = toolingConfig;
		this.toolRequirementEvaluator = toolRequirementEvaluator;
		this.bypassService = bypassService;
		this.debugPluginKey = debugPluginKey;
		this.query = Query.and(PlayerRef.getComponentType());
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DamageBlockEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
		if (playerRef == null) {
			playerRef = store.getComponent(ref, PlayerRef.getComponentType());
		}
		Player player = commandBuffer.getComponent(ref, Player.getComponentType());
		if (player == null) {
			player = store.getComponent(ref, Player.getComponentType());
		}

		boolean bypassActive = isBreakGateBypassed(player);
		String bypassMode = bypassMode(player);
		BlockType damagedBlockType = event.getBlockType();
		if (isSkillsDebugEnabled()) {
			LOGGER.atInfo().log("[Skills][Diag] Hit event received blockId=%s cancelled=%s player=%s bypass=%s",
					damagedBlockType.getId(),
					event.isCancelled(),
					playerRef == null ? "<missing>" : playerRef.getUuid(),
					bypassMode);
		}

		SkillNodeDefinition node = this.nodeLookupService.findByBlockId(damagedBlockType.getId());
		if (node == null) {
			if (isSkillsDebugEnabled()) {
				LOGGER.atInfo().log("[Skills][Diag] Node lookup miss on hit blockId=%s candidate=%s",
						damagedBlockType.getId(),
						looksLikeSkillNodeCandidate(damagedBlockType.getId()));
			}
			if (looksLikeSkillNodeCandidate(damagedBlockType.getId())) {
				if (bypassActive) {
					if (isSkillsDebugEnabled()) {
						LOGGER.atInfo().log("[Skills][Diag] Bypass allowed unconfigured node-like hit mode=%s block=%s player=%s",
								bypassMode,
								damagedBlockType.getId(),
								playerRef == null ? "<missing>" : playerRef.getUuid());
					}
					return;
				}
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
					"Player skill profile missing during hit event; skipping gather gate processing for safety.");
			if (isSkillsDebugEnabled()) {
				LOGGER.atWarning().log("[Skills][Diag] Hit event aborted due to missing profile blockId=%s player=%s",
						damagedBlockType.getId(),
						playerRef == null ? "<missing>" : playerRef.getUuid());
			}
			return;
		}

		SkillType skill = node.getSkillType();
		int levelBefore = this.runtimeApi.getSkillLevel(commandBuffer, ref, skill);
		if (levelBefore < node.getRequiredSkillLevel() && !bypassActive) {
			if (isSkillsDebugEnabled()) {
				LOGGER.atInfo().log("[Skills][Diag] Level gate blocked hit node=%s skill=%s level=%d required=%d block=%s",
						node.getId(),
						skill,
						levelBefore,
						node.getRequiredSkillLevel(),
						damagedBlockType.getId());
			}
			event.setCancelled(true);
			sendPlayerNotification(playerRef,
					String.format("[Skills] %s level %d/%d (current/required).", formatSkillName(skill),
							levelBefore, node.getRequiredSkillLevel()),
					NotificationStyle.Warning);
			return;
		}
		if (levelBefore < node.getRequiredSkillLevel() && isSkillsDebugEnabled()) {
			LOGGER.atInfo().log("[Skills][Diag] Bypass ignored level gate on hit mode=%s node=%s skill=%s level=%d required=%d block=%s",
					bypassMode,
					node.getId(),
					skill,
					levelBefore,
					node.getRequiredSkillLevel(),
					damagedBlockType.getId());
		}

		if (bypassActive) {
			return;
		}

		ItemStack heldItem = event.getItemInHand();
		double toolEfficiency = resolveToolEfficiencyMultiplier(heldItem, node);
		if (toolEfficiency <= 0.0D) {
			sendPlayerNotification(
					playerRef,
					"[Skills] That doesn't seem to be working well...",
					NotificationStyle.Warning);
		}
		double originalDamage = event.getDamage();
		double scaledDamage = originalDamage * toolEfficiency;
		event.setDamage((float) Math.max(0.0D, scaledDamage));
		if (isSkillsDebugEnabled()) {
			LOGGER.atFine().log(
					"[Skills][Diag] Tool efficiency scaled hit node=%s block=%s tool=%s multiplier=%.3f damage=%.3f->%.3f keyword=%s",
					node.getId(),
					damagedBlockType.getId(),
					heldItem == null || ItemStack.isEmpty(heldItem) ? "<empty>" : heldItem.getItemId(),
					toolEfficiency,
					originalDamage,
					scaledDamage,
					node.getRequiredToolKeyword());
		}
	}

	private double resolveToolEfficiencyMultiplier(@Nullable ItemStack heldItem, @Nonnull SkillNodeDefinition node) {
		if (heldItem == null || ItemStack.isEmpty(heldItem)) {
			return this.toolingConfig.noToolEfficiencyMultiplier();
		}

		RequirementCheckResult familyCheck = this.toolRequirementEvaluator
				.evaluate(heldItem, node.getRequiredToolKeyword(), ToolTier.NONE);
		if (!familyCheck.isSuccess()) {
			return this.toolingConfig.mismatchedFamilyEfficiencyMultiplier();
		}

		return this.toolingConfig.efficiencyMultiplierFor(familyCheck.getDetectedTier());
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	private void sendPlayerNotification(@Nullable PlayerRef playerRef, @Nonnull String text,
			@Nonnull NotificationStyle style) {
		if (playerRef == null || isNotificationCoolingDown(playerRef)) {
			return;
		}

		try {
			NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(text), style);
		} catch (Exception e) {
			LOGGER.atFine().withCause(e).log("Failed to send skills notification; falling back to chat message.");
			playerRef.sendMessage(Message.raw(text));
		}
	}

	private boolean isNotificationCoolingDown(@Nonnull PlayerRef playerRef) {
		long now = System.currentTimeMillis();
		UUID playerId = playerRef.getUuid();
		Long lastNotifiedAt = this.lastNoticeByPlayer.get(playerId);
		if (lastNotifiedAt != null && now - lastNotifiedAt < NOTIFICATION_COOLDOWN_MILLIS) {
			return true;
		}
		this.lastNoticeByPlayer.put(playerId, now);
		return false;
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
