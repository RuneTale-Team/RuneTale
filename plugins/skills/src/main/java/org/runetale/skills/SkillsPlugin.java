package org.runetale.skills;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import org.runetale.skills.config.SkillsConfigService;
import org.runetale.skills.config.SkillsExternalConfigBootstrap;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.command.CombatStyleCommand;
import org.runetale.skills.command.SkillCommand;
import org.runetale.skills.command.SkillsPageCommand;
import org.runetale.skills.command.debug.SkillXpCommand;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.interaction.OpenSmeltingUIInteraction;
import org.runetale.skills.interaction.OpenSmithingUIInteraction;
import org.runetale.skills.progression.service.SkillProgressionService;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.progression.system.SkillXpGrantSystem;
import org.runetale.skills.service.CombatStyleService;
import org.runetale.skills.service.CraftingRecipeTagService;
import org.runetale.skills.service.XpService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.SkillSessionStatsService;
import org.runetale.skills.service.SkillXpToastHudService;
import org.runetale.skills.system.CombatDamageXpSystem;
import org.runetale.skills.system.CraftingRecipeUnlockSystem;
import org.runetale.skills.system.CraftingPageProgressSystem;
import org.runetale.skills.system.CraftingXpSystem;
import org.runetale.skills.system.EnsurePlayerSkillProfileSystem;
import org.runetale.skills.system.PlayerJoinRecipeUnlockSystem;
import org.runetale.skills.system.SkillNodeBreakBlockSystem;
import org.runetale.skills.system.SkillXpToastHudExpirySystem;

import javax.annotation.Nonnull;

/**
 * SkillsPlugin - A Hytale server plugin.
 */
public class SkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
    private XpService xpService;

    /**
     * Config snapshot service for tunable gameplay/runtime values.
     */
    private SkillsConfigService skillsConfigService;

    /**
     * Service that resolves data-driven skill-node assets for broken blocks.
     */
    private SkillNodeLookupService nodeLookupService;

    /**
     * Session-scoped telemetry used by skill UI and feedback messaging.
     */
    private SkillSessionStatsService sessionStatsService;

    /**
     * Session-scoped player melee combat style preferences.
     */
    private CombatStyleService combatStyleService;

    /**
     * Session-scoped custom HUD toasts for XP gains.
     */
    private SkillXpToastHudService skillXpToastHudService;

    /**
     * Central progression mutation service used by all XP sources.
     */
    private SkillProgressionService progressionService;

    /**
     * Dispatch service used by systems/APIs to enqueue XP grants.
     */
    private SkillXpDispatchService xpDispatchService;

    /**
     * Stateless utility for extracting skill tags from crafting recipes.
     */
    private CraftingRecipeTagService craftingRecipeTagService;

    /**
     * Runtime path layout for external config and plugin data.
     */
    private SkillsPathLayout pathLayout;

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

    public SkillSessionStatsService getSessionStatsService() {
        return this.sessionStatsService;
    }

    public SkillXpDispatchService getXpDispatchService() {
        return this.xpDispatchService;
    }

    public CraftingRecipeTagService getCraftingRecipeTagService() {
        return this.craftingRecipeTagService;
    }

    public SkillsConfigService getSkillsConfigService() {
        return this.skillsConfigService;
    }

    /**
     * Public plugin API: queue an XP grant by strict skill id.
     */
    public boolean grantSkillXp(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String skillId,
            double experience,
            @Nonnull String source,
            boolean notifyPlayer) {
        if (this.xpDispatchService == null) {
            LOGGER.atWarning().log("Rejected XP grant because dispatch service is unavailable.");
            return false;
        }
        return this.xpDispatchService.grantSkillXp(accessor, playerRef, skillId, experience, source, notifyPlayer);
    }

    /**
     * Public plugin API: queue an XP grant by enum skill identity.
     */
    public boolean grantSkillXp(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull SkillType skillType,
            double experience,
            @Nonnull String source,
            boolean notifyPlayer) {
        if (this.xpDispatchService == null) {
            LOGGER.atWarning().log("Rejected XP grant because dispatch service is unavailable.");
            return false;
        }
        return this.xpDispatchService.grantSkillXp(accessor, playerRef, skillType, experience, source, notifyPlayer);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills runtime framework...");

        // Deterministic setup ordering is intentional and documented by this method
        // sequence.
        registerServices();
        registerCodecs();
        registerAssets();
        registerComponents();
        this.getCommandRegistry().registerCommand(new SkillCommand(this.xpService));
        this.getCommandRegistry().registerCommand(new CombatStyleCommand(this.combatStyleService));
        this.getCommandRegistry().registerCommand(new SkillXpCommand());
        this.getCommandRegistry().registerCommand(
                new SkillsPageCommand(
                        this.playerSkillProfileComponentType,
                        this.xpService,
                        this.nodeLookupService));
        registerSystems();

        LOGGER.atInfo().log("Skills runtime setup complete.");
    }

    /**
     * Registers shared runtime services in deterministic order before any
     * component/system wiring.
     */
    private void registerServices() {
        LOGGER.atInfo().log("[Skills] Registering services...");
        this.pathLayout = SkillsPathLayout.fromDataDirectory(this.getDataDirectory());
        LOGGER.atInfo().log("[Skills] Runtime paths mods=%s config=%s runtime=%s",
                this.pathLayout.modsRoot(),
                this.pathLayout.pluginConfigRoot(),
                this.pathLayout.pluginRuntimeRoot());
        SkillsExternalConfigBootstrap.seedMissingDefaults(this.pathLayout);
        this.skillsConfigService = new SkillsConfigService(this.pathLayout.pluginConfigRoot());
        this.xpService = new XpService(this.skillsConfigService.getXpConfig());
        this.nodeLookupService = new SkillNodeLookupService(this.pathLayout.pluginConfigRoot());
        this.sessionStatsService = new SkillSessionStatsService();
        this.combatStyleService = new CombatStyleService();
        this.skillXpToastHudService = new SkillXpToastHudService(this.skillsConfigService.getHudConfig());
        this.xpDispatchService = new SkillXpDispatchService();
        this.craftingRecipeTagService = new CraftingRecipeTagService(this.skillsConfigService.getCraftingConfig());
        LOGGER.atInfo().log("[Skills] Services registered.");
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
        LOGGER.atInfo().log("[Skills] Registering codecs...");
        this.getCodecRegistry(Interaction.CODEC)
                .register(OpenSmeltingUIInteraction.TYPE_NAME, OpenSmeltingUIInteraction.class, OpenSmeltingUIInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC)
                .register(OpenSmithingUIInteraction.TYPE_NAME, OpenSmithingUIInteraction.class, OpenSmithingUIInteraction.CODEC);
        LOGGER.atInfo().log("[Skills] Codecs registered.");
    }

    /**
     * Registers data-driven skill-node assets that define gathering behavior.
     */
    private void registerAssets() {
        LOGGER.atInfo().log("[Skills] Registering asset lookup hooks...");
        this.nodeLookupService.initializeDefaults();
        LOGGER.atInfo().log("[Skills] Asset lookup hooks registered.");
    }

    /**
     * Registers ECS components in deterministic order.
     */
    private void registerComponents() {
        LOGGER.atInfo().log("[Skills] Registering components...");
        this.playerSkillProfileComponentType = this.getEntityStoreRegistry()
                .registerComponent(PlayerSkillProfileComponent.class, "PlayerSkillProfile",
                        PlayerSkillProfileComponent.CODEC);
        this.progressionService = new SkillProgressionService(this.playerSkillProfileComponentType, this.xpService);
        LOGGER.atInfo().log("[Skills] Components registered.");
    }

    /**
     * Registers ECS systems in deterministic order.
     */
    private void registerSystems() {
        LOGGER.atInfo().log("[Skills] Registering systems...");

        // First ensure all players receive a profile component for persistence
        // correctness.
        this.getEntityStoreRegistry()
                .registerSystem(new EnsurePlayerSkillProfileSystem(this.playerSkillProfileComponentType));

        // Apply all queued XP grants through the centralized progression pipeline.
        this.getEntityStoreRegistry().registerSystem(
                new SkillXpGrantSystem(this.progressionService, this.sessionStatsService, this.skillXpToastHudService));

        // Apply combat-derived XP from damage events (melee style + ranged).
        this.getEntityStoreRegistry().registerSystem(
                new CombatDamageXpSystem(this.xpDispatchService, this.combatStyleService, this.skillsConfigService.getCombatConfig()));

        // Keep custom XP toasts transient and auto-expiring.
        this.getEntityStoreRegistry().registerSystem(
                new SkillXpToastHudExpirySystem(this.skillXpToastHudService, this.skillsConfigService.getHudConfig()));

        // Drive timed progress updates for custom smithing/smelting pages.
        this.getEntityStoreRegistry().registerSystem(new CraftingPageProgressSystem(this.skillsConfigService.getCraftingConfig()));

        // Then process block-break events with requirement checks and XP grants.
        this.getEntityStoreRegistry().registerSystem(
                new SkillNodeBreakBlockSystem(
                        this.playerSkillProfileComponentType,
                        this.xpDispatchService,
                        this.nodeLookupService,
                        this.skillsConfigService.getHeuristicsConfig()));

        // Grant XP from crafting recipes tagged with skill XP rewards.
        this.getEntityStoreRegistry().registerSystem(
                new CraftingXpSystem(
                        this.playerSkillProfileComponentType,
                        this.xpDispatchService,
                        this.craftingRecipeTagService));

        // Unlock recipes when a player levels up in a skill.
        this.getEntityStoreRegistry().registerSystem(
                new CraftingRecipeUnlockSystem(
                        this.playerSkillProfileComponentType,
                        this.craftingRecipeTagService));

        // Sync recipe unlocks on player join for catch-up.
        this.getEntityStoreRegistry().registerSystem(
                new PlayerJoinRecipeUnlockSystem(
                        this.playerSkillProfileComponentType,
                        this.craftingRecipeTagService));

        LOGGER.atInfo().log("[Skills] Systems registered.");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills plugin...");

        // Clear explicit singleton/state references for clean hot reload behavior.
        this.playerSkillProfileComponentType = null;
        this.xpService = null;
        this.skillsConfigService = null;
        this.nodeLookupService = null;
        this.sessionStatsService = null;
        this.combatStyleService = null;
        this.skillXpToastHudService = null;
        this.progressionService = null;
        this.xpDispatchService = null;
        this.craftingRecipeTagService = null;
        this.pathLayout = null;

        instance = null;
    }
}
