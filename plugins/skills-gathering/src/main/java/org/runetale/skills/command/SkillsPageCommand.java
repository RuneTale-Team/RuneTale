package org.runetale.skills.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.page.SkillsOverviewPage;
import org.runetale.skills.service.SkillNodeLookupService;

import javax.annotation.Nonnull;
import java.util.Locale;

public class SkillsPageCommand extends AbstractPlayerCommand {

	private final SkillsRuntimeApi runtimeApi;
	private final SkillNodeLookupService nodeLookupService;
	private final OptionalArg<String> actionArg;

	public SkillsPageCommand(
			@Nonnull SkillsRuntimeApi runtimeApi,
			@Nonnull SkillNodeLookupService nodeLookupService) {
		super("skills", "Opens your skills overview page.");
		this.setPermissionGroup(GameMode.Adventure);
		this.runtimeApi = runtimeApi;
		this.nodeLookupService = nodeLookupService;
		this.actionArg = this.withOptionalArg("action", "Use 'help' to show command usage.", ArgTypes.STRING);
	}

	@Override
	protected void execute(
			@Nonnull CommandContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {
		if (this.actionArg.provided(context)) {
			String action = this.actionArg.get(context);
			if (isHelpToken(action)) {
				sendHelp(context);
				return;
			}

			context.sendMessage(Message.raw("[Skills] Unknown argument: " + action + "."));
			sendHelp(context);
			return;
		}

		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		player.getPageManager().openCustomPage(
				ref,
				store,
				new SkillsOverviewPage(
						playerRef,
						this.runtimeApi,
						this.nodeLookupService));
	}

	private boolean isHelpToken(String raw) {
		if (raw == null) {
			return false;
		}
		String normalized = raw.trim().toLowerCase(Locale.ROOT);
		return normalized.equals("help") || normalized.equals("-h")
				|| normalized.equals("--help") || normalized.equals("?");
	}

	private void sendHelp(@Nonnull CommandContext context) {
		context.sendMessage(Message.raw("[Skills] Opens the skills overview interface."));
		context.sendMessage(Message.raw("[Skills] Usage: /skills"));
		context.sendMessage(Message.raw("[Skills] Help: /skills help"));
	}
}
