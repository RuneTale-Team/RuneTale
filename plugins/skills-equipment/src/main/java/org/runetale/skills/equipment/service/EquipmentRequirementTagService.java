package org.runetale.skills.equipment.service;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EquipmentRequirementTagService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EquipmentConfig equipmentConfig;
    private final Map<String, List<SkillRequirement>> requirementsByItemId = new ConcurrentHashMap<>();

    public EquipmentRequirementTagService(@Nonnull EquipmentConfig equipmentConfig) {
        this.equipmentConfig = equipmentConfig;
    }

    @Nonnull
    public List<SkillRequirement> getRequirements(@Nonnull Item item) {
        String itemId = item.getId();
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }

        return this.requirementsByItemId.computeIfAbsent(itemId, ignored -> {
            AssetExtraInfo.Data data = item.getData();
            if (data == null) {
                return List.of();
            }

            Map<String, String[]> rawTags = data.getRawTags();
            if (rawTags == null || rawTags.isEmpty()) {
                return List.of();
            }

            return parseRequirements(itemId, rawTags);
        });
    }

    @Nonnull
    List<SkillRequirement> parseRequirements(
            @Nonnull String itemId,
            @Nonnull Map<String, String[]> rawTags) {

        String[] skillEntries = rawTags.get(this.equipmentConfig.tagSkillRequired());
        if (skillEntries == null || skillEntries.length == 0) {
            return List.of();
        }

        String[] levelEntries = rawTags.get(this.equipmentConfig.tagLevelRequirement());
        List<SkillRequirement> parsed = new ArrayList<>(skillEntries.length);
        for (int i = 0; i < skillEntries.length; i++) {
            SkillType skillType = parseSkill(itemId, i, skillEntries[i]);
            if (skillType == null) {
                continue;
            }

            String rawLevel = levelEntries != null && i < levelEntries.length ? levelEntries[i] : null;
            int level = parseLevelEntry(itemId, i, rawLevel);
            parsed.add(new SkillRequirement(skillType, level));
        }
        return parsed.isEmpty() ? List.of() : List.copyOf(parsed);
    }

    @Nullable
    private SkillType parseSkill(@Nonnull String itemId, int index, @Nullable String rawSkillEntry) {
        SkillType skillType = SkillType.tryParseStrict(rawSkillEntry);
        if (skillType == null) {
            LOGGER.atWarning().log("Invalid %s entry '%s' at index %d on item %s; skipping",
                    this.equipmentConfig.tagSkillRequired(),
                    rawSkillEntry,
                    index,
                    itemId);
            return null;
        }

        return skillType;
    }

    private int parseLevelEntry(
            @Nonnull String itemId,
            int index,
            @Nullable String rawLevelEntry) {
        if (rawLevelEntry == null || rawLevelEntry.isBlank()) {
            return this.equipmentConfig.defaultRequiredLevel();
        }

        try {
            int parsedLevel = Integer.parseInt(rawLevelEntry.trim());
            return Math.max(1, parsedLevel);
        } catch (NumberFormatException ex) {
            LOGGER.atWarning().log("Invalid %s entry '%s' at index %d on item %s; defaulting to %d",
                    this.equipmentConfig.tagLevelRequirement(),
                    rawLevelEntry,
                    index,
                    itemId,
                    this.equipmentConfig.defaultRequiredLevel());
            return this.equipmentConfig.defaultRequiredLevel();
        }
    }
}
