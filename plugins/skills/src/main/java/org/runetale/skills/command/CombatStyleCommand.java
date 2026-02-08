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
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.page.CombatStylePage;
import org.runetale.skills.service.CombatStyleService;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Player command for choosing which melee skill receives combat XP.
 */
public class CombatStyleCommand extends AbstractPlayerCommand {

	private final OptionalArg<String> styleArg;
	private final CombatStyleService combatStyleService;

	public CombatStyleCommand(@Nonnull CombatStyleService combatStyleService) {
		super("combatstyle", "Sets your melee XP mode (accurate/aggressive/defensive/controlled). Use /combatstyle ui.");
		this.setPermissionGroup(GameMode.Adventure);
		this.styleArg = this.withOptionalArg("mode", "accurate|aggressive|defensive|controlled|ui|current", ArgTypes.STRING);
		this.combatStyleService = combatStyleService;
	}

	@Override
	protected void execute(
			@Nonnull CommandContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {

		CombatStyleType currentStyle = this.combatStyleService.getCombatStyle(playerRef.getUuid());
		if (!this.styleArg.provided(context)) {
			openModePage(store, ref, playerRef);
			context.sendMessage(Message.raw(String.format(
					Locale.ROOT,
					"[Skills] Current melee mode: %s (%s).",
					currentStyle.getDisplayName(),
					currentStyle.describeMeleeXpRouting())));
			return;
		}

		String rawStyle = this.styleArg.get(context).trim();
		if (isHelpToken(rawStyle)) {
			sendHelp(context);
			return;
		}

		if ("ui".equalsIgnoreCase(rawStyle)) {
			openModePage(store, ref, playerRef);
			return;
		}

		if ("current".equalsIgnoreCase(rawStyle)) {
			context.sendMessage(Message.raw(String.format(
					Locale.ROOT,
					"[Skills] Current melee mode: %s (%s).",
					currentStyle.getDisplayName(),
					currentStyle.describeMeleeXpRouting())));
			return;
		}

		CombatStyleType parsedStyle = CombatStyleType.tryParse(rawStyle);
		if (parsedStyle == null) {
			context.sendMessage(Message.raw("[Skills] Unknown combat mode: " + rawStyle + "."));
			context.sendMessage(Message.raw("[Skills] Valid modes: " + CombatStyleType.validModeHint() + "."));
			context.sendMessage(Message.raw("[Skills] Legacy aliases still work: attack, strength, defense."));
			context.sendMessage(Message.raw("[Skills] Type /combatstyle help for usage examples."));
			return;
		}

		this.combatStyleService.setCombatStyle(playerRef.getUuid(), parsedStyle);
		context.sendMessage(Message.raw(String.format(
				Locale.ROOT,
				"[Skills] Melee mode set to %s (%s).",
				parsedStyle.getDisplayName(),
				parsedStyle.describeMeleeXpRouting())));
	}

	private void openModePage(
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef) {
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		player.getPageManager().openCustomPage(
				ref,
				store,
				new CombatStylePage(playerRef, this.combatStyleService));
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
		context.sendMessage(Message.raw("[Skills] Sets your melee XP routing mode."));
		context.sendMessage(Message.raw("[Skills] Usage: /combatstyle <mode|ui|current>"));
		context.sendMessage(Message.raw("[Skills] Modes: " + CombatStyleType.validModeHint()));
		context.sendMessage(Message.raw("[Skills] Examples: /combatstyle aggressive, /combatstyle ui"));
	}
}
