package org.runetale.skills.command.debug;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.service.DebugModeService;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Admin command that toggles per-plugin runtime debug diagnostics.
 */
public class RtDebugCommand extends AbstractPlayerCommand {

	private final DebugModeService debugModeService;
	private final OptionalArg<String> pluginArg;
	private final OptionalArg<String> actionArg;

	public RtDebugCommand(@Nonnull DebugModeService debugModeService) {
		super("rtdebug", "Toggles RuneTale plugin debug diagnostics.");
		this.setPermissionGroup(GameMode.Creative);
		this.debugModeService = debugModeService;
		this.pluginArg = this.withOptionalArg("plugin", "Plugin id (e.g. skills)", ArgTypes.STRING);
		this.actionArg = this.withOptionalArg("action", "on|off|status", ArgTypes.STRING);
	}

	@Override
	protected void execute(
			@Nonnull CommandContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {

		if (!this.pluginArg.provided(context)) {
			sendHelp(context);
			return;
		}

		String rawPlugin = this.pluginArg.get(context);
		if (isHelpToken(rawPlugin)) {
			sendHelp(context);
			return;
		}

		String plugin = rawPlugin.trim().toLowerCase(Locale.ROOT);
		if (!this.debugModeService.isSupported(plugin)) {
			context.sendMessage(Message.raw("[RuneTale] Unknown plugin: " + rawPlugin + "."));
			context.sendMessage(Message.raw("[RuneTale] Supported plugins: " + supportedPluginsHint()));
			return;
		}

		String action = this.actionArg.provided(context) ? this.actionArg.get(context) : "on";
		String normalizedAction = action.trim().toLowerCase(Locale.ROOT);
		switch (normalizedAction) {
			case "on":
			case "enable":
				this.debugModeService.enable(plugin);
				context.sendMessage(Message.raw("[RuneTale] Debug mode enabled for plugin=" + plugin + "."));
				break;
			case "off":
			case "disable":
				this.debugModeService.disable(plugin);
				context.sendMessage(Message.raw("[RuneTale] Debug mode disabled for plugin=" + plugin + "."));
				break;
			case "status":
				context.sendMessage(Message.raw(String.format(
						Locale.ROOT,
						"[RuneTale] Debug mode for plugin=%s is %s.",
						plugin,
						this.debugModeService.isEnabled(plugin) ? "ON" : "OFF")));
				break;
			default:
				context.sendMessage(Message.raw("[RuneTale] Unknown action: " + action + "."));
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

	@Nonnull
	private String supportedPluginsHint() {
		StringJoiner joiner = new StringJoiner(", ");
		for (String plugin : this.debugModeService.supportedPlugins()) {
			joiner.add(plugin);
		}
		return joiner.toString();
	}

	private void sendHelp(@Nonnull CommandContext context) {
		context.sendMessage(Message.raw("[RuneTale] Toggles deep diagnostic logging per plugin."));
		context.sendMessage(Message.raw("[RuneTale] Usage: /rtdebug <plugin> [on|off|status]"));
		context.sendMessage(Message.raw("[RuneTale] Example: /rtdebug skills on"));
		context.sendMessage(Message.raw("[RuneTale] Supported plugins: " + supportedPluginsHint()));
	}
}
