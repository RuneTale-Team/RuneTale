package org.runetale.skills.progression.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.progression.service.PlayerRefResolver;
import org.runetale.skills.progression.service.SkillProgressionService;
import org.runetale.skills.progression.service.SkillXpGrantFeedbackService;
import org.runetale.skills.service.SkillSessionStatsService;
import org.runetale.testing.junit.ContractTest;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContractTest
class SkillXpGrantSystemContractTest {

	@Test
	void handleReturnsEarlyWhenProgressionGainsNoXp() {
		SkillProgressionService progressionService = mock(SkillProgressionService.class);
		SkillSessionStatsService sessionStatsService = mock(SkillSessionStatsService.class);
		SkillXpGrantFeedbackService feedbackService = mock(SkillXpGrantFeedbackService.class);
		PlayerRefResolver playerRefResolver = mock(PlayerRefResolver.class);
		Query<EntityStore> query = mock(Query.class);
		SkillXpGrantSystem system = new SkillXpGrantSystem(
				progressionService,
				sessionStatsService,
				feedbackService,
				playerRefResolver,
				query);
		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> ref = mock(Ref.class);
		SkillXpGrantEvent event = new SkillXpGrantEvent(SkillType.MINING, 12.0D, "test", true);
		SkillXpGrantResult noGainResult = new SkillXpGrantResult(SkillType.MINING, 100L, 100L, 0L, 2, 2);

		when(chunk.getReferenceTo(0)).thenReturn(ref);
		when(progressionService.grantExperience(eq(commandBuffer), eq(ref), eq(SkillType.MINING), anyDouble()))
				.thenReturn(noGainResult);

		system.handle(0, chunk, store, commandBuffer, event);

		verify(playerRefResolver, never()).resolve(commandBuffer, ref);
		verify(sessionStatsService, never()).recordGain(any(UUID.class), any(SkillType.class), anyLong());
		verify(feedbackService, never()).apply(any(CommandBuffer.class), any(Ref.class), any(PlayerRef.class), any(SkillXpGrantEvent.class), any(SkillXpGrantResult.class));
	}

	@Test
	void handleReturnsEarlyWhenPlayerRefMissing() {
		SkillProgressionService progressionService = mock(SkillProgressionService.class);
		SkillSessionStatsService sessionStatsService = mock(SkillSessionStatsService.class);
		SkillXpGrantFeedbackService feedbackService = mock(SkillXpGrantFeedbackService.class);
		PlayerRefResolver playerRefResolver = mock(PlayerRefResolver.class);
		Query<EntityStore> query = mock(Query.class);
		SkillXpGrantSystem system = new SkillXpGrantSystem(
				progressionService,
				sessionStatsService,
				feedbackService,
				playerRefResolver,
				query);
		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> ref = mock(Ref.class);
		SkillXpGrantEvent event = new SkillXpGrantEvent(SkillType.MINING, 12.0D, "test", true);
		SkillXpGrantResult gainResult = new SkillXpGrantResult(SkillType.MINING, 100L, 112L, 12L, 2, 2);

		when(chunk.getReferenceTo(0)).thenReturn(ref);
		when(progressionService.grantExperience(eq(commandBuffer), eq(ref), eq(SkillType.MINING), anyDouble()))
				.thenReturn(gainResult);
		when(playerRefResolver.resolve(commandBuffer, ref)).thenReturn(null);

		system.handle(0, chunk, store, commandBuffer, event);

		verify(sessionStatsService, never()).recordGain(any(UUID.class), any(SkillType.class), anyLong());
		verify(feedbackService, never()).apply(any(CommandBuffer.class), any(Ref.class), any(PlayerRef.class), any(SkillXpGrantEvent.class), any(SkillXpGrantResult.class));
	}

	@Test
	void handleRecordsSessionAndDelegatesFeedbackWhenPlayerPresent() {
		SkillProgressionService progressionService = mock(SkillProgressionService.class);
		SkillSessionStatsService sessionStatsService = mock(SkillSessionStatsService.class);
		SkillXpGrantFeedbackService feedbackService = mock(SkillXpGrantFeedbackService.class);
		PlayerRefResolver playerRefResolver = mock(PlayerRefResolver.class);
		Query<EntityStore> query = mock(Query.class);
		SkillXpGrantSystem system = new SkillXpGrantSystem(
				progressionService,
				sessionStatsService,
				feedbackService,
				playerRefResolver,
				query);
		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> ref = mock(Ref.class);
		PlayerRef playerRef = mock(PlayerRef.class);
		UUID playerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		SkillXpGrantEvent event = new SkillXpGrantEvent(SkillType.SMITHING, 25.0D, "test", true);
		SkillXpGrantResult gainResult = new SkillXpGrantResult(SkillType.SMITHING, 100L, 125L, 25L, 2, 2);

		when(chunk.getReferenceTo(0)).thenReturn(ref);
		when(progressionService.grantExperience(eq(commandBuffer), eq(ref), eq(SkillType.SMITHING), anyDouble()))
				.thenReturn(gainResult);
		when(playerRefResolver.resolve(commandBuffer, ref)).thenReturn(playerRef);
		when(playerRef.getUuid()).thenReturn(playerId);

		system.handle(0, chunk, store, commandBuffer, event);

		verify(sessionStatsService).recordGain(playerId, SkillType.SMITHING, 25L);
		verify(feedbackService).apply(commandBuffer, ref, playerRef, event, gainResult);
	}
}
