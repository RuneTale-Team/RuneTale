package org.runetale.skills.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

/**
 * Player-only command that prints the caller's current skill progression.
 */
public class SkillCommand extends AbstractPlayerCommand {

	public SkillCommand() {
		super("skill", "Displays your skill levels and XP.");
	}

	@Override
	protected void execute(
			@Nonnull CommandContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {
		if (PlayerSkillProfileComponent.getComponentType() == null) {
			playerRef.sendMessage(Message.raw("Skills profile is currently unavailable."));
			return;
		}

		PlayerSkillProfileComponent profile = store.getComponent(ref, PlayerSkillProfileComponent.getComponentType());
		if (profile == null) {
			playerRef.sendMessage(Message.raw("Skill profile missing; showing default levels."));
			for (SkillType skillType : SkillType.values()) {
				playerRef.sendMessage(Message.raw(String.format("%s: level=1 xp=0", skillType.name())));
			}
			return;
		}

		playerRef.sendMessage(Message.raw("Your skills:"));
		for (SkillType skillType : SkillType.values()) {
			int level = profile.getLevel(skillType);
			long experience = profile.getExperience(skillType);
			playerRef
					.sendMessage(Message.raw(String.format("%s: level=%d xp=%d", skillType.name(), level, experience)));
		}
	}
}
