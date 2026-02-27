package org.runetale.skills.combat;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.command.CombatStyleCommand;
import org.runetale.skills.combat.config.CombatExternalConfigBootstrap;
import org.runetale.skills.config.CombatConfig;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.service.CombatStyleService;
import org.runetale.skills.service.ConstitutionHealthService;
import org.runetale.skills.system.ConstitutionBaselineSystem;
import org.runetale.skills.system.CombatDamageXpSystem;
import org.runetale.skills.system.CombatSessionCleanupSystem;
import org.runetale.skills.system.ConstitutionHealthLevelUpSystem;
import org.runetale.skills.system.ConstitutionHealthSyncOnJoinSystem;

import javax.annotation.Nonnull;

public class CombatSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private CombatConfig combatConfig;
    private CombatStyleService combatStyleService;
    private ConstitutionHealthService constitutionHealthService;

    public CombatSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills combat plugin...");
        registerServices();
        registerCommands();
        registerSystems();
        LOGGER.atInfo().log("Skills combat setup complete.");
    }

    private void registerServices() {
        SkillsPathLayout pathLayout = SkillsPathLayout.fromDataDirectory(this.getDataDirectory());
        CombatExternalConfigBootstrap.seedMissingDefaults(pathLayout);
        this.combatConfig = CombatConfig.load(pathLayout.pluginConfigRoot());
        this.combatStyleService = new CombatStyleService();
    }

    private void registerCommands() {
        if (this.combatStyleService == null) {
            LOGGER.atSevere().log("Combat services unavailable; command registration skipped.");
            return;
        }
        this.getCommandRegistry().registerCommand(new CombatStyleCommand(this.combatStyleService));
    }

    private void registerSystems() {
        SkillsRuntimeApi runtimeApi = SkillsRuntimeRegistry.get();
        if (runtimeApi == null) {
            LOGGER.atSevere().log("Skills core plugin unavailable; combat systems not registered.");
            return;
        }
        if (this.combatConfig == null || this.combatStyleService == null) {
            LOGGER.atSevere().log("Combat services unavailable; combat systems not registered.");
            return;
        }

        this.constitutionHealthService = new ConstitutionHealthService(runtimeApi, this.combatConfig);

        this.getEntityStoreRegistry().registerSystem(
                new ConstitutionBaselineSystem(runtimeApi, this.combatConfig));

        this.getEntityStoreRegistry().registerSystem(
                new ConstitutionHealthSyncOnJoinSystem(this.constitutionHealthService));

        this.getEntityStoreRegistry().registerSystem(
                new ConstitutionHealthLevelUpSystem(this.constitutionHealthService));

        this.getEntityStoreRegistry().registerSystem(
                new CombatDamageXpSystem(runtimeApi, this.combatStyleService, this.combatConfig));

        this.getEntityStoreRegistry().registerSystem(new CombatSessionCleanupSystem(this.combatStyleService));
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills combat plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills combat plugin...");
        this.combatConfig = null;
        this.combatStyleService = null;
        this.constitutionHealthService = null;
    }
}
