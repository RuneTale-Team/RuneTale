package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.SkillNodeBreakResolutionResult;
import org.runetale.skills.service.SkillNodeBreakResolutionService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.ToolRequirementEvaluator;
import org.runetale.testing.junit.ContractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContractTest
class SkillNodeBreakBlockSystemContractTest {

	@Test
	void handleCancelsAndNotifiesWhenResolutionRequestsWarning() {
		ComponentType<EntityStore, PlayerSkillProfileComponent> profileType = new ComponentType<>();
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		SkillNodeLookupService lookupService = mock(SkillNodeLookupService.class);
		ToolRequirementEvaluator toolEvaluator = mock(ToolRequirementEvaluator.class);
		SkillNodeBreakResolutionService resolutionService = mock(SkillNodeBreakResolutionService.class);
		SkillNodePlayerRefResolver playerRefResolver = mock(SkillNodePlayerRefResolver.class);
		SkillNodePlayerNotifier notifier = mock(SkillNodePlayerNotifier.class);
		Query<EntityStore> query = mock(Query.class);
		SkillNodeBreakBlockSystem system = new SkillNodeBreakBlockSystem(profileType, dispatchService, lookupService,
				toolEvaluator, resolutionService, playerRefResolver, notifier, query);

		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		BreakBlockEvent event = mock(BreakBlockEvent.class);
		Ref<EntityStore> ref = mock(Ref.class);
		PlayerRef playerRef = mock(PlayerRef.class);
		BlockType blockType = mock(BlockType.class);

		when(chunk.getReferenceTo(0)).thenReturn(ref);
		when(playerRefResolver.resolve(commandBuffer, ref)).thenReturn(playerRef);
		when(event.getBlockType()).thenReturn(blockType);
		when(blockType.getId()).thenReturn("Ore_Unconfigured");
		when(lookupService.findByBlockId("Ore_Unconfigured")).thenReturn(null);
		when(resolutionService.resolveMissingNode("Ore_Unconfigured"))
				.thenReturn(SkillNodeBreakResolutionResult.cancelWithWarning("[Skills] Missing node"));

		system.handle(0, chunk, store, commandBuffer, event);

		verify(event).setCancelled(true);
		verify(notifier).notify(playerRef, "[Skills] Missing node", NotificationStyle.Warning);
		verify(dispatchService, never()).grantSkillXp(any(CommandBuffer.class), any(Ref.class), any(SkillType.class), any(Double.class), any(String.class), any(Boolean.class));
	}

	@Test
	void handleDispatchesXpWhenResolutionRequestsGrant() {
		ComponentType<EntityStore, PlayerSkillProfileComponent> profileType = new ComponentType<>();
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		SkillNodeLookupService lookupService = mock(SkillNodeLookupService.class);
		ToolRequirementEvaluator toolEvaluator = mock(ToolRequirementEvaluator.class);
		SkillNodeBreakResolutionService resolutionService = mock(SkillNodeBreakResolutionService.class);
		SkillNodePlayerRefResolver playerRefResolver = mock(SkillNodePlayerRefResolver.class);
		SkillNodePlayerNotifier notifier = mock(SkillNodePlayerNotifier.class);
		Query<EntityStore> query = mock(Query.class);
		SkillNodeBreakBlockSystem system = new SkillNodeBreakBlockSystem(profileType, dispatchService, lookupService,
				toolEvaluator, resolutionService, playerRefResolver, notifier, query);

		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		BreakBlockEvent event = mock(BreakBlockEvent.class);
		Ref<EntityStore> ref = mock(Ref.class);
		PlayerRef playerRef = mock(PlayerRef.class);
		BlockType blockType = mock(BlockType.class);
		PlayerSkillProfileComponent profile = mock(PlayerSkillProfileComponent.class);
		SkillNodeDefinition node = new SkillNodeDefinition("oak_test", SkillType.WOODCUTTING, "Wood_Oak", 1,
				ToolTier.NONE, "Tool_Hatchet", 12.0D);
		RequirementCheckResult toolCheck = RequirementCheckResult.success(ToolTier.COPPER, "Tool_Hatchet_Copper");

		when(chunk.getReferenceTo(0)).thenReturn(ref);
		when(playerRefResolver.resolve(commandBuffer, ref)).thenReturn(playerRef);
		when(event.getBlockType()).thenReturn(blockType);
		when(blockType.getId()).thenReturn("Wood_Oak");
		when(lookupService.findByBlockId("Wood_Oak")).thenReturn(node);
		when(commandBuffer.getComponent(ref, profileType)).thenReturn(profile);
		when(profile.getLevel(SkillType.WOODCUTTING)).thenReturn(10);
		when(toolEvaluator.evaluate(any(), eq("Tool_Hatchet"), eq(ToolTier.NONE))).thenReturn(toolCheck);
		when(resolutionService.resolveConfiguredNode(node, 10, toolCheck))
				.thenReturn(SkillNodeBreakResolutionResult.dispatchXp(SkillType.WOODCUTTING, 12.0D, "node:oak_test"));

		system.handle(0, chunk, store, commandBuffer, event);

		verify(dispatchService).grantSkillXp(commandBuffer, ref, SkillType.WOODCUTTING, 12.0D, "node:oak_test", true);
		verify(event, never()).setCancelled(true);
		verify(notifier, never()).notify(any(), any(), any());
	}

	@Test
	void handleReturnsWhenProfileMissingForConfiguredNode() {
		ComponentType<EntityStore, PlayerSkillProfileComponent> profileType = new ComponentType<>();
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		SkillNodeLookupService lookupService = mock(SkillNodeLookupService.class);
		ToolRequirementEvaluator toolEvaluator = mock(ToolRequirementEvaluator.class);
		SkillNodeBreakResolutionService resolutionService = mock(SkillNodeBreakResolutionService.class);
		SkillNodePlayerRefResolver playerRefResolver = mock(SkillNodePlayerRefResolver.class);
		SkillNodePlayerNotifier notifier = mock(SkillNodePlayerNotifier.class);
		Query<EntityStore> query = mock(Query.class);
		SkillNodeBreakBlockSystem system = new SkillNodeBreakBlockSystem(profileType, dispatchService, lookupService,
				toolEvaluator, resolutionService, playerRefResolver, notifier, query);

		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		BreakBlockEvent event = mock(BreakBlockEvent.class);
		Ref<EntityStore> ref = mock(Ref.class);
		PlayerRef playerRef = mock(PlayerRef.class);
		BlockType blockType = mock(BlockType.class);
		SkillNodeDefinition node = new SkillNodeDefinition("oak_test", SkillType.WOODCUTTING, "Wood_Oak", 1,
				ToolTier.NONE, "Tool_Hatchet", 12.0D);

		when(chunk.getReferenceTo(0)).thenReturn(ref);
		when(playerRefResolver.resolve(commandBuffer, ref)).thenReturn(playerRef);
		when(event.getBlockType()).thenReturn(blockType);
		when(blockType.getId()).thenReturn("Wood_Oak");
		when(lookupService.findByBlockId("Wood_Oak")).thenReturn(node);
		when(commandBuffer.getComponent(ref, profileType)).thenReturn(null);

		system.handle(0, chunk, store, commandBuffer, event);

		verify(resolutionService, never()).resolveConfiguredNode(any(), any(Integer.class), any());
		verify(dispatchService, never()).grantSkillXp(any(CommandBuffer.class), any(Ref.class), any(SkillType.class), any(Double.class), any(String.class), any(Boolean.class));
	}
}
