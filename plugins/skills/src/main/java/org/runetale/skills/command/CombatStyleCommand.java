package org.runetale.skills.command;

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
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.service.CombatStyleService;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Player command for choosing which melee skill receives combat XP.
 */
public class CombatStyleCommand extends AbstractPlayerCommand {

	private final OptionalArg<String> styleArg;
	private final CombatStyleService combatStyleService;

	public CombatStyleCommand(@Nonnull CombatStyleService combatStyleService) {
		super("combatstyle", "Sets your melee combat XP style (attack, strength, defense).");
		this.setPermissionGroup(GameMode.Adventure);
		this.styleArg = this.withOptionalArg("style", "attack|strength|defense", ArgTypes.STRING);
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
			context.sendMessage(Message.raw(String.format(
					Locale.ROOT,
					"[Skills] Current melee combat style: %s.",
					formatStyleName(currentStyle))));
			context.sendMessage(Message.raw("[Skills] Usage: /combatstyle <attack|strength|defense>."));
			return;
		}

		String rawStyle = this.styleArg.get(context);
		CombatStyleType parsedStyle = CombatStyleType.tryParse(rawStyle);
		if (parsedStyle == null) {
			context.sendMessage(Message.raw("[Skills] Unknown combat style: " + rawStyle + "."));
			context.sendMessage(Message.raw("[Skills] Valid styles: " + validStyles() + "."));
			return;
		}

		this.combatStyleService.setCombatStyle(playerRef.getUuid(), parsedStyle);
		context.sendMessage(Message.raw(String.format(
				Locale.ROOT,
				"[Skills] Melee combat style set to %s.",
				formatStyleName(parsedStyle))));
	}

	@Nonnull
	private String validStyles() {
		return Arrays.stream(CombatStyleType.values())
				.map(value -> value.name().toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(", "));
	}

	@Nonnull
	private String formatStyleName(@Nonnull CombatStyleType style) {
		String lowered = style.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}
}
