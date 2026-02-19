package org.runetale.skills.gathering;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.skills.SkillsPlugin;
import org.runetale.skills.command.SkillsPageCommand;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.system.SkillNodeBreakBlockSystem;

import javax.annotation.Nonnull;

public class GatheringSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static GatheringSkillsPlugin instance;

    private SkillNodeLookupService nodeLookupService;

    public static GatheringSkillsPlugin getInstance() {
        return instance;
    }

    public SkillNodeLookupService getNodeLookupService() {
        return this.nodeLookupService;
    }

    public GatheringSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
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
        this.nodeLookupService = new SkillNodeLookupService(pathLayout.pluginConfigRoot());
        this.nodeLookupService.initializeDefaults();
    }

    private void registerCommands() {
        SkillsPlugin corePlugin = SkillsPlugin.getInstance();
        if (corePlugin == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; gathering command registration skipped.");
            return;
        }

        this.getCommandRegistry().registerCommand(
                new SkillsPageCommand(
                        corePlugin.getPlayerSkillProfileComponentType(),
                        corePlugin.getXpService(),
                        this.nodeLookupService));
    }

    private void registerSystems() {
        SkillsPlugin corePlugin = SkillsPlugin.getInstance();
        if (corePlugin == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; gathering systems not registered.");
            return;
        }

        this.getEntityStoreRegistry().registerSystem(
                new SkillNodeBreakBlockSystem(
                        corePlugin.getPlayerSkillProfileComponentType(),
                        corePlugin.getXpDispatchService(),
                        this.nodeLookupService,
                        corePlugin.getSkillsConfigService().getHeuristicsConfig(),
                        corePlugin.getDebugModeService()));
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills gathering plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills gathering plugin...");
        this.nodeLookupService = null;
        instance = null;
    }
}
