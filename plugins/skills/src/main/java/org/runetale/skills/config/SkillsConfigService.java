package org.runetale.skills.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;

public class SkillsConfigService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final XpConfig xpConfig;
    private final CombatConfig combatConfig;
    private final CraftingConfig craftingConfig;
    private final HudConfig hudConfig;
    private final ToolingConfig toolingConfig;
    private final HeuristicsConfig heuristicsConfig;

    public SkillsConfigService() {
        this.xpConfig = XpConfig.load();
        this.combatConfig = CombatConfig.load();
        this.craftingConfig = CraftingConfig.load();
        this.hudConfig = HudConfig.load();
        this.toolingConfig = ToolingConfig.load();
        this.heuristicsConfig = HeuristicsConfig.load();
        logSnapshot();
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
    public CraftingConfig getCraftingConfig() {
        return this.craftingConfig;
    }

    @Nonnull
    public HudConfig getHudConfig() {
        return this.hudConfig;
    }

    @Nonnull
    public ToolingConfig getToolingConfig() {
        return this.toolingConfig;
    }

    @Nonnull
    public HeuristicsConfig getHeuristicsConfig() {
        return this.heuristicsConfig;
    }

    private void logSnapshot() {
        LOGGER.atInfo().log("[Skills] Config snapshot: xp.maxLevel=%d rounding=%s", this.xpConfig.maxLevel(), this.xpConfig.roundingMode());
        LOGGER.atInfo().log("[Skills] Config snapshot: combat.xpPerDamage=%.2f", this.combatConfig.xpPerDamage());
        LOGGER.atInfo().log(
                "[Skills] Config snapshot: crafting.anvilBench=%s furnaceBench=%s maxCraft=%d",
                this.craftingConfig.anvilBenchId(),
                this.craftingConfig.furnaceBenchId(),
                this.craftingConfig.maxCraftCount());
        LOGGER.atInfo().log(
                "[Skills] Config snapshot: hud.toast=%dms fade=%dms",
                this.hudConfig.toastDurationMillis(),
                this.hudConfig.toastFadeDurationMillis());
        LOGGER.atInfo().log(
                "[Skills] Config snapshot: heuristics.nodeTokens=%s",
                this.heuristicsConfig.nodeCandidateTokens());
    }
}
