package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.event.SkillLevelUpEvent;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.service.SkillXpToastHudService;

import javax.annotation.Nonnull;

/**
 * Applies post-grant feedback side effects (toast, level-up title, level-up event).
 */
public class SkillXpGrantFeedbackService {

	private final SkillXpToastHudService skillXpToastHudService;
	private final SkillLevelUpAnnouncer levelUpAnnouncer;

	public SkillXpGrantFeedbackService(
			@Nonnull SkillXpToastHudService skillXpToastHudService,
			@Nonnull SkillLevelUpAnnouncer levelUpAnnouncer) {
		this.skillXpToastHudService = skillXpToastHudService;
		this.levelUpAnnouncer = levelUpAnnouncer;
	}

	public void apply(
			@Nonnull CommandBuffer<EntityStore> commandBuffer,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull SkillXpGrantEvent event,
			@Nonnull SkillXpGrantResult result) {
		if (!event.shouldNotifyPlayer()) {
			return;
		}

		this.skillXpToastHudService.showXpToast(
				playerRef,
				result.getSkillType(),
				result.getGainedExperience(),
				result.isLevelUp());

		if (!result.isLevelUp()) {
			return;
		}

		this.levelUpAnnouncer.announceLevelUp(playerRef, result.getSkillType(), result.getUpdatedLevel());
		commandBuffer.invoke(ref, new SkillLevelUpEvent(
				result.getSkillType(),
				result.getPreviousLevel(),
				result.getUpdatedLevel()));
	}
}
