package org.runetale.skills.actions;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import org.runetale.skills.actions.config.ItemActionsExternalConfigBootstrap;
import org.runetale.skills.actions.interaction.ConsumeSkillActionInteraction;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.ItemActionsConfig;
import org.runetale.skills.config.SkillsPathLayout;

import javax.annotation.Nonnull;

public class ActionsSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ItemActionsConfig itemActionsConfig;

    public ActionsSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills actions plugin...");
        registerCodecs();
        registerServices();
        LOGGER.atInfo().log("Skills actions setup complete.");
    }

    private void registerCodecs() {
        LOGGER.atInfo().log("[Skills Actions] Registering interaction codecs...");
        this.getCodecRegistry(Interaction.CODEC)
                .register(ConsumeSkillActionInteraction.TYPE_NAME, ConsumeSkillActionInteraction.class, ConsumeSkillActionInteraction.CODEC);
        LOGGER.atInfo().log("[Skills Actions] Interaction codecs registered.");
    }

    private void registerServices() {
        SkillsPathLayout pathLayout = SkillsPathLayout.fromDataDirectory(this.getDataDirectory());
        ItemActionsExternalConfigBootstrap.seedMissingDefaults(pathLayout);
        this.itemActionsConfig = ItemActionsConfig.load(pathLayout.pluginConfigRoot());
        ItemActionsRuntimeRegistry.register(this.itemActionsConfig);

        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi != null && this.itemActionsConfig != null && runtimeApi.isDebugEnabled(this.itemActionsConfig.debugPluginKey())) {
            LOGGER.atInfo().log("[Skills][Diag] Loaded %d configured item actions", this.itemActionsConfig.actions().size());
        }
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills actions plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills actions plugin...");
        if (this.itemActionsConfig != null) {
            ItemActionsRuntimeRegistry.clear(this.itemActionsConfig);
        }
        this.itemActionsConfig = null;
    }
}
