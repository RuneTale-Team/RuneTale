package org.runetale.skills.command.debug;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.SkillsPlugin;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Debug/admin command for queueing XP grants through the shared progression pipeline.
 */
public class SkillXpCommand extends AbstractPlayerCommand {

	private final OptionalArg<String> skillArg;
	private final OptionalArg<Double> xpArg;
	private final OptionalArg<String> sourceArg;
	private final FlagArg silentFlag;

	public SkillXpCommand() {
		super("skillxp", "Queues a debug XP grant for one of your skills.");
		this.setPermissionGroup(GameMode.Creative);
		this.skillArg = this.withOptionalArg("skill", "Skill id (e.g. MINING)", ArgTypes.STRING);
		this.xpArg = this.withOptionalArg("xp", "XP amount to grant", ArgTypes.DOUBLE);
		this.sourceArg = this.withOptionalArg("source", "Optional telemetry source label", ArgTypes.STRING);
		this.silentFlag = this.withFlagArg("silent", "Suppress player XP/level notifications.");
	}

	@Override
	protected void execute(
			@Nonnull CommandContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {

		if (!this.skillArg.provided(context)) {
			sendHelp(context);
			return;
		}

		String rawSkill = this.skillArg.get(context);
		if (isHelpToken(rawSkill)) {
			sendHelp(context);
			return;
		}

		if (!this.xpArg.provided(context)) {
			context.sendMessage(Message.raw("[Skills] Missing required argument: xp."));
			sendHelp(context);
			return;
		}

		SkillType skillType = SkillType.tryParseStrict(rawSkill);
		if (skillType == null) {
			context.sendMessage(Message.raw("[Skills] Unknown skill id: " + rawSkill + "."));
			context.sendMessage(Message.raw("[Skills] Valid skills: " + validSkillIds()));
			context.sendMessage(Message.raw("[Skills] Type /skillxp help for usage examples."));
			return;
		}

		double xp = this.xpArg.get(context);
		if (xp <= 0.0D) {
			context.sendMessage(Message.raw("[Skills] XP must be greater than zero."));
			context.sendMessage(Message.raw("[Skills] Example: /skillxp MINING 25"));
			return;
		}

		String source = this.sourceArg.provided(context)
				? this.sourceArg.get(context)
				: "command:skillxp";
		boolean notifyPlayer = !this.silentFlag.get(context);

		boolean queued = SkillsPlugin.getInstance().grantSkillXp(
				store,
				ref,
				skillType,
				xp,
				source,
				notifyPlayer);
		if (!queued) {
			context.sendMessage(Message.raw("[Skills] Failed to queue XP grant."));
			return;
		}

		context.sendMessage(Message.raw(String.format(
				Locale.ROOT,
				"[Skills] Queued +%.2f XP for %s (source=%s, notify=%s).",
				xp,
				formatSkillName(skillType),
				source,
				notifyPlayer)));
	}

	@Nonnull
	private String validSkillIds() {
		return Arrays.stream(SkillType.values())
				.map(Enum::name)
				.collect(Collectors.joining(", "));
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skillType) {
		String lowered = skillType.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
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
		context.sendMessage(Message.raw("[Skills] Queues XP through the shared progression pipeline."));
		context.sendMessage(Message.raw("[Skills] Usage: /skillxp <skill> <xp> [source] [--silent]"));
		context.sendMessage(Message.raw("[Skills] Example: /skillxp MINING 25 command:test"));
		context.sendMessage(Message.raw("[Skills] Valid skills: " + validSkillIds()));
	}
}
