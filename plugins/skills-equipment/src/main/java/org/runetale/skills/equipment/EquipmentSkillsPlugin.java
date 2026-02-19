package org.runetale.skills.equipment;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.equipment.config.EquipmentExternalConfigBootstrap;

import javax.annotation.Nonnull;

public class EquipmentSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EquipmentConfig equipmentConfig;

    public EquipmentSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills equipment plugin...");
        registerServices();
        registerSystems();
        LOGGER.atInfo().log("Skills equipment setup complete.");
    }

    private void registerServices() {
        SkillsPathLayout pathLayout = SkillsPathLayout.fromDataDirectory(this.getDataDirectory());
        EquipmentExternalConfigBootstrap.seedMissingDefaults(pathLayout);
        this.equipmentConfig = EquipmentConfig.load(pathLayout.pluginConfigRoot());
    }

    private void registerSystems() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; equipment systems not registered.");
            return;
        }

        if (this.equipmentConfig == null) {
            LOGGER.atSevere().log("Equipment config unavailable; equipment systems not registered.");
        }
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills equipment plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills equipment plugin...");
        this.equipmentConfig = null;
    }
}
