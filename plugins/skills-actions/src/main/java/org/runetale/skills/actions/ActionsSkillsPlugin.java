package org.runetale.skills.actions;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.skills.actions.config.ItemActionsExternalConfigBootstrap;
import org.runetale.skills.actions.listener.ItemXpActionMouseButtonListener;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.ItemActionsConfig;
import org.runetale.skills.config.SkillsPathLayout;

import javax.annotation.Nonnull;

public class ActionsSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ItemActionsConfig itemActionsConfig;
    private ItemXpActionMouseButtonListener actionListener;

    public ActionsSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills actions plugin...");
        registerServices();
        registerListeners();
        LOGGER.atInfo().log("Skills actions setup complete.");
    }

    private void registerServices() {
        SkillsPathLayout pathLayout = SkillsPathLayout.fromDataDirectory(this.getDataDirectory());
        ItemActionsExternalConfigBootstrap.seedMissingDefaults(pathLayout);
        this.itemActionsConfig = ItemActionsConfig.load(pathLayout.pluginConfigRoot());
    }

    private void registerListeners() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; actions listener not registered.");
            return;
        }
        if (this.itemActionsConfig == null) {
            LOGGER.atSevere().log("Actions config unavailable; actions listener not registered.");
            return;
        }

        this.actionListener = new ItemXpActionMouseButtonListener(runtimeApi, this.itemActionsConfig);
        this.getEventRegistry().registerGlobal(PlayerMouseButtonEvent.class, this.actionListener::handle);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills actions plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills actions plugin...");
        this.itemActionsConfig = null;
        this.actionListener = null;
    }
}
