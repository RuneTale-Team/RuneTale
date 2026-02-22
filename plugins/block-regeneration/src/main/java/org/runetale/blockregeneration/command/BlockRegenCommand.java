package org.runetale.blockregeneration.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.blockregeneration.domain.BlockRegenDefinition;
import org.runetale.blockregeneration.service.BlockRegenCoordinatorService;
import org.runetale.blockregeneration.service.BlockRegenRuntimeService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public class BlockRegenCommand extends AbstractPlayerCommand {

    private final BlockRegenCoordinatorService coordinatorService;
    private final OptionalArg<String> actionArg;
    private final OptionalArg<String> xArg;
    private final OptionalArg<String> yArg;
    private final OptionalArg<String> zArg;

    public BlockRegenCommand(@Nonnull BlockRegenCoordinatorService coordinatorService) {
        super("blockregen", "Manages block regeneration runtime.");
        this.setPermissionGroup(GameMode.Creative);
        this.coordinatorService = coordinatorService;
        this.actionArg = this.withOptionalArg("action", "reload|inspect|stats", ArgTypes.STRING);
        this.xArg = this.withOptionalArg("x", "Block x", ArgTypes.STRING);
        this.yArg = this.withOptionalArg("y", "Block y", ArgTypes.STRING);
        this.zArg = this.withOptionalArg("z", "Block z", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        String action = this.actionArg.provided(context) ? this.actionArg.get(context).trim().toLowerCase(Locale.ROOT) : "help";
        switch (action) {
            case "reload":
                handleReload(context);
                return;
            case "stats":
                handleStats(context);
                return;
            case "inspect":
                handleInspect(context, world);
                return;
            default:
                sendHelp(context);
        }
    }

    private void handleReload(@Nonnull CommandContext context) {
        BlockRegenCoordinatorService.ReloadResult result = this.coordinatorService.reload();
        context.sendMessage(Message.raw(String.format(
                Locale.ROOT,
                "[BlockRegen] Reloaded. enabled=%s definitions=%d (runtime state cleared)",
                result.enabled(),
                result.definitionsLoaded())));
    }

    private void handleStats(@Nonnull CommandContext context) {
        BlockRegenRuntimeService.MetricsSnapshot stats = this.coordinatorService.metricsSnapshot();
        context.sendMessage(Message.raw(String.format(
                Locale.ROOT,
                "[BlockRegen] matched=%d blocked=%d depletions=%d respawns=%d active=%d",
                stats.matchedInteractions(),
                stats.blockedInteractions(),
                stats.depletions(),
                stats.respawns(),
                stats.activeStates())));
    }

    private void handleInspect(@Nonnull CommandContext context, @Nonnull World world) {
        Integer x = parseCoordinate(context, this.xArg, "x");
        Integer y = parseCoordinate(context, this.yArg, "y");
        Integer z = parseCoordinate(context, this.zArg, "z");
        if (x == null || y == null || z == null) {
            context.sendMessage(Message.raw("[BlockRegen] Usage: /blockregen inspect <x> <y> <z>"));
            return;
        }

        BlockType blockType = world.getBlockType(x, y, z);
        String blockId = blockType == null ? "<unknown>" : blockType.getId();
        BlockRegenDefinition definition = this.coordinatorService.findDefinition(blockId);
        BlockRegenRuntimeService.RuntimeSnapshot snapshot = this.coordinatorService.inspectState(world.getName(), x, y, z);

        context.sendMessage(Message.raw("[BlockRegen] Inspect at " + x + " " + y + " " + z));
        context.sendMessage(Message.raw("[BlockRegen] currentBlock=" + blockId));
        context.sendMessage(Message.raw("[BlockRegen] matchedDefinition=" + (definition == null ? "<none>" : definition.id())));
        if (snapshot == null) {
            context.sendMessage(Message.raw("[BlockRegen] state=<none>"));
            return;
        }

        context.sendMessage(Message.raw(String.format(
                Locale.ROOT,
                "[BlockRegen] phase=%s gather=%d/%d respawnDue=%d",
                snapshot.phase(),
                snapshot.gatherCount(),
                snapshot.gatherThreshold(),
                snapshot.respawnDueMillis())));
    }

    @Nullable
    private Integer parseCoordinate(@Nonnull CommandContext context, @Nonnull OptionalArg<String> arg, @Nonnull String label) {
        if (!arg.provided(context)) {
            return null;
        }

        String raw = arg.get(context);
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            context.sendMessage(Message.raw("[BlockRegen] Invalid " + label + " coordinate: " + raw));
            return null;
        }
    }

    private void sendHelp(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("[BlockRegen] Manages node regeneration runtime."));
        context.sendMessage(Message.raw("[BlockRegen] Usage: /blockregen reload"));
        context.sendMessage(Message.raw("[BlockRegen] Usage: /blockregen stats"));
        context.sendMessage(Message.raw("[BlockRegen] Usage: /blockregen inspect <x> <y> <z>"));
    }
}
