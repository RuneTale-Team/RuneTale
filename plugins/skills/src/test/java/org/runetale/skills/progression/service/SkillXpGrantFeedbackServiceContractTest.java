package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.event.SkillLevelUpEvent;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.service.SkillXpToastHudService;
import org.runetale.testing.junit.ContractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ContractTest
class SkillXpGrantFeedbackServiceContractTest {

	@Test
	void applySkipsAllFeedbackWhenNotificationsDisabled() {
		SkillXpToastHudService hudService = mock(SkillXpToastHudService.class);
		SkillLevelUpAnnouncer announcer = mock(SkillLevelUpAnnouncer.class);
		SkillXpGrantFeedbackService service = new SkillXpGrantFeedbackService(hudService, announcer);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> ref = mock(Ref.class);
		PlayerRef playerRef = mock(PlayerRef.class);
		SkillXpGrantEvent event = new SkillXpGrantEvent(SkillType.MINING, 10.0D, "test", false);
		SkillXpGrantResult result = new SkillXpGrantResult(SkillType.MINING, 0L, 10L, 10L, 1, 1);

		service.apply(commandBuffer, ref, playerRef, event, result);

		verify(hudService, never()).showXpToast(any(PlayerRef.class), any(SkillType.class), anyLong(), anyBoolean());
		verify(announcer, never()).announceLevelUp(any(PlayerRef.class), any(SkillType.class), anyInt());
		verify(commandBuffer, never()).invoke(eq(ref), any(SkillLevelUpEvent.class));
	}

	@Test
	void applyShowsToastButSkipsLevelUpWhenNoLevelIncrease() {
		SkillXpToastHudService hudService = mock(SkillXpToastHudService.class);
		SkillLevelUpAnnouncer announcer = mock(SkillLevelUpAnnouncer.class);
		SkillXpGrantFeedbackService service = new SkillXpGrantFeedbackService(hudService, announcer);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> ref = mock(Ref.class);
		PlayerRef playerRef = mock(PlayerRef.class);
		SkillXpGrantEvent event = new SkillXpGrantEvent(SkillType.SMITHING, 15.0D, "test", true);
		SkillXpGrantResult result = new SkillXpGrantResult(SkillType.SMITHING, 20L, 35L, 15L, 1, 1);

		service.apply(commandBuffer, ref, playerRef, event, result);

		verify(hudService).showXpToast(playerRef, SkillType.SMITHING, 15L, false);
		verify(announcer, never()).announceLevelUp(any(PlayerRef.class), any(SkillType.class), anyInt());
		verify(commandBuffer, never()).invoke(eq(ref), any(SkillLevelUpEvent.class));
	}

	@Test
	void applyShowsToastAndEmitsLevelUpWhenLevelIncreases() {
		SkillXpToastHudService hudService = mock(SkillXpToastHudService.class);
		SkillLevelUpAnnouncer announcer = mock(SkillLevelUpAnnouncer.class);
		SkillXpGrantFeedbackService service = new SkillXpGrantFeedbackService(hudService, announcer);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> ref = mock(Ref.class);
		PlayerRef playerRef = mock(PlayerRef.class);
		SkillXpGrantEvent event = new SkillXpGrantEvent(SkillType.WOODCUTTING, 200.0D, "test", true);
		SkillXpGrantResult result = new SkillXpGrantResult(SkillType.WOODCUTTING, 100L, 300L, 200L, 1, 2);

		service.apply(commandBuffer, ref, playerRef, event, result);

		verify(hudService).showXpToast(playerRef, SkillType.WOODCUTTING, 200L, true);
		verify(announcer).announceLevelUp(playerRef, SkillType.WOODCUTTING, 2);
		verify(commandBuffer).invoke(eq(ref), any(SkillLevelUpEvent.class));
	}
}
