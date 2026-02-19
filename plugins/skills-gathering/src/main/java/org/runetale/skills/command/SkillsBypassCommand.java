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
import org.runetale.skills.service.GatheringBypassService;

import javax.annotation.Nonnull;
import java.util.Locale;

public class SkillsBypassCommand extends AbstractPlayerCommand {

	private final GatheringBypassService bypassService;
	private final OptionalArg<String> targetArg;
	private final OptionalArg<String> actionArg;

	public SkillsBypassCommand(@Nonnull GatheringBypassService bypassService) {
		super("skillsbypass", "Configures gathering requirement bypass options.");
		this.setPermissionGroup(GameMode.Adventure);
		this.bypassService = bypassService;
		this.targetArg = this.withOptionalArg("target", "ops|help", ArgTypes.STRING);
		this.actionArg = this.withOptionalArg("action", "on|off|status", ArgTypes.STRING);
	}

	@Override
	protected void execute(
			@Nonnull CommandContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {

		String rawTarget = this.targetArg.provided(context) ? this.targetArg.get(context) : "ops";
		if (isHelpToken(rawTarget)) {
			sendHelp(context);
			return;
		}

		String target = rawTarget.trim().toLowerCase(Locale.ROOT);
		if (!target.equals("ops")) {
			context.sendMessage(Message.raw("[Skills] Unknown bypass target: " + rawTarget + "."));
			sendHelp(context);
			return;
		}

		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		if (!player.hasPermission(GatheringBypassService.MANAGE_PERMISSION)) {
			context.sendMessage(Message.raw("[Skills] Missing permission: " + GatheringBypassService.MANAGE_PERMISSION));
			return;
		}

		String action = this.actionArg.provided(context) ? this.actionArg.get(context) : "status";
		String normalizedAction = action.trim().toLowerCase(Locale.ROOT);
		switch (normalizedAction) {
			case "on":
			case "enable":
				this.bypassService.setOpExemptionEnabled(true);
				context.sendMessage(Message.raw("[Skills] OP bypass for gathering gates is now ON."));
				break;
			case "off":
			case "disable":
				this.bypassService.setOpExemptionEnabled(false);
				context.sendMessage(Message.raw("[Skills] OP bypass for gathering gates is now OFF."));
				break;
			case "status":
				context.sendMessage(Message.raw(String.format(
						Locale.ROOT,
						"[Skills] OP bypass for gathering gates is %s.",
						this.bypassService.isOpExemptionEnabled() ? "ON" : "OFF")));
				break;
			default:
				context.sendMessage(Message.raw("[Skills] Unknown action: " + action + "."));
				sendHelp(context);
				break;
		}
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
		context.sendMessage(Message.raw("[Skills] Configures gathering gate bypass behavior."));
		context.sendMessage(Message.raw("[Skills] Creative mode is always exempt."));
		context.sendMessage(Message.raw("[Skills] Usage: /skillsbypass ops [on|off|status]"));
		context.sendMessage(Message.raw("[Skills] Requires permission: " + GatheringBypassService.MANAGE_PERMISSION));
	}
}
