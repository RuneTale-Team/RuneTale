package org.runetale.skills.crafting;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.CraftingConfig;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.crafting.config.CraftingExternalConfigBootstrap;
import org.runetale.skills.interaction.OpenSmeltingUIInteraction;
import org.runetale.skills.interaction.OpenSmithingUIInteraction;
import org.runetale.skills.service.CraftingPageTrackerService;
import org.runetale.skills.service.CraftingRecipeTagService;
import org.runetale.skills.system.CraftingPageProgressSystem;
import org.runetale.skills.system.CraftingRecipeUnlockSystem;
import org.runetale.skills.system.CraftingSessionCleanupSystem;
import org.runetale.skills.system.CraftingXpSystem;
import org.runetale.skills.system.PlayerJoinRecipeUnlockSystem;

import javax.annotation.Nonnull;

public class CraftingSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static CraftingSkillsPlugin instance;

    private CraftingConfig craftingConfig;
    private CraftingRecipeTagService craftingRecipeTagService;
    private CraftingPageTrackerService craftingPageTrackerService;

    public static CraftingSkillsPlugin getInstance() {
        return instance;
    }

    public CraftingRecipeTagService getCraftingRecipeTagService() {
        return this.craftingRecipeTagService;
    }

    public CraftingPageTrackerService getCraftingPageTrackerService() {
        return this.craftingPageTrackerService;
    }

    public CraftingConfig getCraftingConfig() {
        return this.craftingConfig;
    }

    public CraftingSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills crafting plugin...");
        registerServices();
        registerCodecs();
        registerSystems();
        LOGGER.atInfo().log("Skills crafting setup complete.");
    }

    private void registerServices() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; crafting services not initialized.");
            return;
        }

        SkillsPathLayout pathLayout = SkillsPathLayout.fromDataDirectory(this.getDataDirectory());
        CraftingExternalConfigBootstrap.seedMissingDefaults(pathLayout);
        this.craftingConfig = CraftingConfig.load(pathLayout.pluginConfigRoot());
        this.craftingRecipeTagService = new CraftingRecipeTagService(this.craftingConfig);
        this.craftingPageTrackerService = new CraftingPageTrackerService();
    }

    private void registerCodecs() {
        LOGGER.atInfo().log("[Skills Crafting] Registering interaction codecs...");
        this.getCodecRegistry(Interaction.CODEC)
                .register(OpenSmeltingUIInteraction.TYPE_NAME, OpenSmeltingUIInteraction.class, OpenSmeltingUIInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC)
                .register(OpenSmithingUIInteraction.TYPE_NAME, OpenSmithingUIInteraction.class, OpenSmithingUIInteraction.CODEC);
        LOGGER.atInfo().log("[Skills Crafting] Interaction codecs registered.");
    }

    private void registerSystems() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; crafting systems not registered.");
            return;
        }
        if (this.craftingConfig == null || this.craftingRecipeTagService == null || this.craftingPageTrackerService == null) {
            LOGGER.atSevere().log("Crafting services unavailable; crafting systems not registered.");
            return;
        }

        this.getEntityStoreRegistry().registerSystem(new CraftingPageProgressSystem(
                this.craftingConfig,
                this.craftingPageTrackerService));

        this.getEntityStoreRegistry().registerSystem(
                new CraftingXpSystem(
                        runtimeApi,
                        this.craftingRecipeTagService));

        this.getEntityStoreRegistry().registerSystem(
                new CraftingRecipeUnlockSystem(
                        runtimeApi,
                        this.craftingRecipeTagService));

        this.getEntityStoreRegistry().registerSystem(
                new PlayerJoinRecipeUnlockSystem(
                        runtimeApi,
                        this.craftingRecipeTagService));

        this.getEntityStoreRegistry().registerSystem(new CraftingSessionCleanupSystem(this.craftingPageTrackerService));
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills crafting plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills crafting plugin...");
        this.craftingConfig = null;
        this.craftingRecipeTagService = null;
        this.craftingPageTrackerService = null;
        instance = null;
    }
}
