package org.runetale.skills;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.SkillsConfigService;
import org.runetale.skills.config.SkillsExternalConfigBootstrap;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.command.SkillCommand;
import org.runetale.skills.command.debug.RtDebugCommand;
import org.runetale.skills.command.debug.SkillXpCommand;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.service.SkillProgressionService;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.progression.system.SkillXpGrantSystem;
import org.runetale.skills.service.DebugModeService;
import org.runetale.skills.service.SkillSessionStatsService;
import org.runetale.skills.service.SkillXpToastHudService;
import org.runetale.skills.service.XpService;
import org.runetale.skills.system.EnsurePlayerSkillProfileSystem;
import org.runetale.skills.system.PlayerDisconnectInventoryHardeningListener;
import org.runetale.skills.system.PlayerSessionCleanupSystem;
import org.runetale.skills.system.SkillXpToastHudExpirySystem;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * SkillsPlugin - A Hytale server plugin.
 */
public class SkillsPlugin extends JavaPlugin implements SkillsRuntimeApi {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
     * Session-scoped telemetry used by skill UI and feedback messaging.
     */
    private SkillSessionStatsService sessionStatsService;

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
     * Runtime toggles for per-plugin deep diagnostics.
     */
    private DebugModeService debugModeService;

    /**
     * Runtime path layout for external config and plugin data.
     */
    private SkillsPathLayout pathLayout;

    public SkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
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

    public XpService getXpService() {
        return this.xpService;
    }

    public SkillsConfigService getSkillsConfigService() {
        return this.skillsConfigService;
    }

    public DebugModeService getDebugModeService() {
        return this.debugModeService;
    }

    @Override
    public boolean hasSkillProfile(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> playerRef) {
        return accessor.getComponent(playerRef, this.playerSkillProfileComponentType) != null;
    }

    @Override
    public int getSkillLevel(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull SkillType skillType) {
        PlayerSkillProfileComponent profile = accessor.getComponent(playerRef, this.playerSkillProfileComponentType);
        return profile == null ? 1 : profile.getLevel(skillType);
    }

    @Override
    public long getSkillExperience(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull SkillType skillType) {
        PlayerSkillProfileComponent profile = accessor.getComponent(playerRef, this.playerSkillProfileComponentType);
        return profile == null ? 0L : profile.getExperience(skillType);
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
    public int getMaxLevel() {
        if (this.xpService == null) {
            return 1;
        }
        return this.xpService.getMaxLevel();
    }

    @Override
    public long xpForLevel(int level) {
        if (this.xpService == null) {
            return 0L;
        }
        return this.xpService.xpForLevel(level);
    }

    @Override
    public boolean isDebugEnabled(@Nonnull String pluginKey) {
        if (this.debugModeService == null) {
            return false;
        }
        return this.debugModeService.isEnabled(pluginKey);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills runtime framework...");

        // Deterministic setup ordering is intentional and documented by this method
        // sequence.
        registerServices();
        registerCodecs();
        registerComponents();
        this.getCommandRegistry().registerCommand(new SkillCommand(this.xpService, this.playerSkillProfileComponentType));
        this.getCommandRegistry().registerCommand(new SkillXpCommand(this.xpDispatchService));
        this.getCommandRegistry().registerCommand(new RtDebugCommand(this.debugModeService));
        registerSystems();

        PlayerDisconnectInventoryHardeningListener inventoryHardeningListener = new PlayerDisconnectInventoryHardeningListener();
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, inventoryHardeningListener::handle);

        SkillsRuntimeRegistry.register(this);

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
        this.sessionStatsService = new SkillSessionStatsService();
        this.skillXpToastHudService = new SkillXpToastHudService(this.skillsConfigService.getHudConfig());
        this.debugModeService = new DebugModeService(List.of("skills", "skills-actions"));
        this.xpDispatchService = new SkillXpDispatchService(this.debugModeService);
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
        LOGGER.atInfo().log("[Skills] Codecs registered.");
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
                new SkillXpGrantSystem(
                        this.progressionService,
                        this.sessionStatsService,
                        this.skillXpToastHudService,
                        this.debugModeService));

        // Keep custom XP toasts transient and auto-expiring.
        this.getEntityStoreRegistry().registerSystem(
                new SkillXpToastHudExpirySystem(this.skillXpToastHudService, this.skillsConfigService.getHudConfig()));

        // Clear session-scoped state maps when a player entity is removed.
        this.getEntityStoreRegistry().registerSystem(new PlayerSessionCleanupSystem(
                this.sessionStatsService,
                this.skillXpToastHudService));

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
        this.sessionStatsService = null;
        this.skillXpToastHudService = null;
        this.progressionService = null;
        this.xpDispatchService = null;
        this.debugModeService = null;
        this.pathLayout = null;
        SkillsRuntimeRegistry.clear(this);
    }
}
