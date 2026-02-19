package org.runetale.skills.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class SkillsConfigService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final XpConfig xpConfig;
    private final CombatConfig combatConfig;
    private final HudConfig hudConfig;

    public SkillsConfigService(@Nonnull Path externalConfigRoot) {
        LOGGER.atInfo().log("[Skills] Loading config set from externalRoot=%s", externalConfigRoot);
        this.xpConfig = XpConfig.load(externalConfigRoot);
        this.combatConfig = CombatConfig.load(externalConfigRoot);
        this.hudConfig = HudConfig.load(externalConfigRoot);
        logSnapshot();
        LOGGER.atInfo().log("[Skills] Config load complete");
    }

    @Nonnull
    public XpConfig getXpConfig() {
        return this.xpConfig;
    }

    @Nonnull
    public CombatConfig getCombatConfig() {
        return this.combatConfig;
    }

    @Nonnull
    public HudConfig getHudConfig() {
        return this.hudConfig;
    }

    private void logSnapshot() {
        LOGGER.atInfo().log("[Skills] Config snapshot: xp.maxLevel=%d rounding=%s", this.xpConfig.maxLevel(), this.xpConfig.roundingMode());
        LOGGER.atInfo().log("[Skills] Config snapshot: combat.xpPerDamage=%.2f", this.combatConfig.xpPerDamage());
        LOGGER.atInfo().log(
                "[Skills] Config snapshot: hud.toast=%dms fade=%dms",
                this.hudConfig.toastDurationMillis(),
                this.hudConfig.toastFadeDurationMillis());
    }
}
