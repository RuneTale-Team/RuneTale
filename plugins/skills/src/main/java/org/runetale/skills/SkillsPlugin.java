package org.runetale.skills;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.command.SkillCommand;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.service.OsrsXpService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.SkillNodeRuntimeService;
import org.runetale.skills.service.ToolRequirementEvaluator;
import org.runetale.skills.system.EnsurePlayerSkillProfileSystem;
import org.runetale.skills.system.SkillNodeBreakBlockSystem;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SkillsPlugin - A Hytale server plugin.
 */
public class SkillsPlugin extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger(SkillsPlugin.class.getName());

    /**
     * Singleton plugin instance for simple static access from component helper
     * methods.
     */
    private static SkillsPlugin instance;

    /**
     * Persistent, serialized per-player progression component for all skills.
     */
    private ComponentType<EntityStore, PlayerSkillProfileComponent> playerSkillProfileComponentType;

    /**
     * Service that encapsulates OSRS-like nonlinear XP/level mathematics.
     */
    private OsrsXpService xpService;

    /**
     * Service that resolves data-driven skill-node assets for broken blocks.
     */
    private SkillNodeLookupService nodeLookupService;

    /**
     * Runtime service responsible for node depletion and timed respawn tracking.
     */
    private SkillNodeRuntimeService nodeRuntimeService;

    /**
     * Service that evaluates held-tool requirements against configured tiers.
     */
    private ToolRequirementEvaluator toolRequirementEvaluator;

    public SkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static SkillsPlugin getInstance() {
        return instance;
    }

    /**
     * Returns the component type used to persist each player's skill progression
     * profile.
     */
    public ComponentType<EntityStore, PlayerSkillProfileComponent> getPlayerSkillProfileComponentType() {
        return playerSkillProfileComponentType;
    }

    @Override
    protected void setup() {
        LOGGER.log(Level.INFO, "Setting up skills runtime framework...");

        // Deterministic setup ordering is intentional and documented by this method
        // sequence.
        registerServices();
        registerCodecs();
        registerAssets();
        registerComponents();
        this.getCommandRegistry().registerCommand(new SkillCommand(this.xpService));
        registerSystems();

        LOGGER.log(Level.INFO, "Skills runtime setup complete.");
    }

    /**
     * Registers shared runtime services in deterministic order before any
     * component/system wiring.
     */
    private void registerServices() {
        LOGGER.log(Level.INFO, "[Skills] Registering services...");
        this.xpService = new OsrsXpService();
        this.nodeLookupService = new SkillNodeLookupService();
        this.nodeRuntimeService = new SkillNodeRuntimeService();
        this.toolRequirementEvaluator = new ToolRequirementEvaluator();
        LOGGER.log(Level.INFO, "[Skills] Services registered.");
    }

    /**
     * Registers codec map entries in deterministic order.
     *
     * <p>
     * For this subtask, all codec needs are covered by direct static
     * [`BuilderCodec`](build/decompiled/hytale-server-hypixel/com/hypixel/hytale/codec/builder/BuilderCodec.java:42)
     * declarations on concrete classes, so this is currently a no-op with explicit
     * logging.
     */
    private void registerCodecs() {
        LOGGER.log(Level.INFO, "[Skills] Registering codecs...");
        LOGGER.log(Level.FINE, "[Skills] No codec-map registrations required for current woodcutting runtime.");
        LOGGER.log(Level.INFO, "[Skills] Codecs registered.");
    }

    /**
     * Registers data-driven skill-node assets that define gathering behavior.
     */
    private void registerAssets() {
        LOGGER.log(Level.INFO, "[Skills] Registering asset lookup hooks...");
        this.nodeLookupService.initializeDefaults();
        LOGGER.log(Level.INFO, "[Skills] Asset lookup hooks registered.");
    }

    /**
     * Registers ECS components in deterministic order.
     */
    private void registerComponents() {
        LOGGER.log(Level.INFO, "[Skills] Registering components...");
        this.playerSkillProfileComponentType = this.getEntityStoreRegistry()
                .registerComponent(PlayerSkillProfileComponent.class, "PlayerSkillProfile",
                        PlayerSkillProfileComponent.CODEC);
        LOGGER.log(Level.INFO, "[Skills] Components registered.");
    }

    /**
     * Registers ECS systems in deterministic order.
     */
    private void registerSystems() {
        LOGGER.log(Level.INFO, "[Skills] Registering systems...");

        // First ensure all players receive a profile component for persistence
        // correctness.
        this.getEntityStoreRegistry()
                .registerSystem(new EnsurePlayerSkillProfileSystem(this.playerSkillProfileComponentType));

        // Then process block-break events with requirement checks, XP, and depletion
        // logic.
        this.getEntityStoreRegistry().registerSystem(
                new SkillNodeBreakBlockSystem(
                        this.playerSkillProfileComponentType,
                        this.xpService,
                        this.nodeLookupService,
                        this.nodeRuntimeService,
                        this.toolRequirementEvaluator));

        LOGGER.log(Level.INFO, "[Skills] Systems registered.");
    }

    @Override
    protected void start() {
        LOGGER.log(Level.INFO, "Started skills plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.log(Level.INFO, "Shutting down skills plugin...");

        // Clear explicit singleton/state references for clean hot reload behavior.
        this.playerSkillProfileComponentType = null;
        this.xpService = null;
        this.nodeLookupService = null;
        this.nodeRuntimeService = null;
        this.toolRequirementEvaluator = null;

        instance = null;
    }
}
