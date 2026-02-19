package org.runetale.skills.equipment;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.equipment.config.EquipmentExternalConfigBootstrap;
import org.runetale.skills.equipment.service.EquipmentGateNotificationService;
import org.runetale.skills.equipment.service.EquipmentRequirementTagService;
import org.runetale.skills.equipment.system.ActiveSlotRequirementGateSystem;
import org.runetale.skills.equipment.system.EquipmentRequirementEnforcementSystem;

import javax.annotation.Nonnull;

public class EquipmentSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EquipmentConfig equipmentConfig;
    private EquipmentRequirementTagService requirementTagService;
    private EquipmentGateNotificationService notificationService;

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
        this.requirementTagService = new EquipmentRequirementTagService(this.equipmentConfig);
        this.notificationService = new EquipmentGateNotificationService(this.equipmentConfig);
    }

    private void registerSystems() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; equipment systems not registered.");
            return;
        }

        if (this.equipmentConfig == null || this.requirementTagService == null || this.notificationService == null) {
            LOGGER.atSevere().log("Equipment config unavailable; equipment systems not registered.");
            return;
        }

        if (this.equipmentConfig.enforceActiveHand()) {
            this.getEntityStoreRegistry().registerSystem(new ActiveSlotRequirementGateSystem(
                    runtimeApi,
                    this.equipmentConfig,
                    this.requirementTagService,
                    this.notificationService));
        }

        if (this.equipmentConfig.enforceArmor() || this.equipmentConfig.enforceActiveHandReconcile()) {
            this.getEntityStoreRegistry().registerSystem(new EquipmentRequirementEnforcementSystem(
                    runtimeApi,
                    this.equipmentConfig,
                    this.requirementTagService,
                    this.notificationService));
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
        this.requirementTagService = null;
        this.notificationService = null;
    }
}
