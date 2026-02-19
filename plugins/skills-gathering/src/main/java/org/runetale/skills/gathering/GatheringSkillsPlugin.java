package org.runetale.skills.gathering;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.command.SkillsBypassCommand;
import org.runetale.skills.command.SkillsPageCommand;
import org.runetale.skills.config.HeuristicsConfig;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.gathering.config.GatheringExternalConfigBootstrap;
import org.runetale.skills.service.GatheringBypassService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.system.SkillNodeBreakBlockSystem;
import org.runetale.skills.system.SkillNodeDamageBlockGateSystem;

import javax.annotation.Nonnull;

public class GatheringSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private SkillNodeLookupService nodeLookupService;
    private HeuristicsConfig heuristicsConfig;
    private GatheringBypassService bypassService;

    public SkillNodeLookupService getNodeLookupService() {
        return this.nodeLookupService;
    }

    public GatheringSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills gathering systems...");
        registerServices();
        registerCommands();
        registerSystems();
        LOGGER.atInfo().log("Skills gathering setup complete.");
    }

    private void registerServices() {
        SkillsPathLayout pathLayout = SkillsPathLayout.fromDataDirectory(this.getDataDirectory());
        GatheringExternalConfigBootstrap.seedMissingDefaults(pathLayout);
        this.heuristicsConfig = HeuristicsConfig.load(pathLayout.pluginConfigRoot());
        this.nodeLookupService = new SkillNodeLookupService(pathLayout.pluginConfigRoot());
        this.nodeLookupService.initializeDefaults();
        this.bypassService = new GatheringBypassService();
    }

    private void registerCommands() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; gathering command registration skipped.");
            return;
        }

        this.getCommandRegistry().registerCommand(
                new SkillsPageCommand(
                        runtimeApi,
                        this.nodeLookupService));
        this.getCommandRegistry().registerCommand(new SkillsBypassCommand(this.bypassService));
    }

    private void registerSystems() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; gathering systems not registered.");
            return;
        }

        this.getEntityStoreRegistry().registerSystem(
                new SkillNodeDamageBlockGateSystem(
                        runtimeApi,
                        this.nodeLookupService,
                        this.heuristicsConfig,
                        this.bypassService,
                        "skills"));

        this.getEntityStoreRegistry().registerSystem(
                new SkillNodeBreakBlockSystem(
                        runtimeApi,
                        this.nodeLookupService,
                        this.heuristicsConfig,
                        this.bypassService,
                        "skills"));
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills gathering plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills gathering plugin...");
        this.nodeLookupService = null;
        this.heuristicsConfig = null;
        this.bypassService = null;
    }
}
