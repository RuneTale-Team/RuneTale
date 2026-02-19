package org.runetale.skills.equipment.service;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.equipment.domain.EquipmentLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EquipmentRequirementTagService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EquipmentConfig equipmentConfig;
    private final Map<String, EquipmentLocation> locationsByAlias;
    private final Map<String, Map<EquipmentLocation, SkillRequirement>> requirementsByItemId = new ConcurrentHashMap<>();

    public EquipmentRequirementTagService(@Nonnull EquipmentConfig equipmentConfig) {
        this.equipmentConfig = equipmentConfig;
        this.locationsByAlias = buildAliasMap(equipmentConfig.locationAliases());
    }

    @Nullable
    public SkillRequirement getRequirementForLocation(@Nonnull Item item, @Nonnull EquipmentLocation location) {
        return getRequirementsByLocation(item).get(location);
    }

    @Nonnull
    public Map<EquipmentLocation, SkillRequirement> getRequirementsByLocation(@Nonnull Item item) {
        String itemId = item.getId();
        if (itemId == null || itemId.isBlank()) {
            return Collections.emptyMap();
        }

        return this.requirementsByItemId.computeIfAbsent(itemId, ignored -> {
            AssetExtraInfo.Data data = item.getData();
            if (data == null) {
                return Collections.emptyMap();
            }

            Map<String, String[]> rawTags = data.getRawTags();
            if (rawTags == null || rawTags.isEmpty()) {
                return Collections.emptyMap();
            }

            return parseRequirements(itemId, rawTags);
        });
    }

    @Nonnull
    Map<EquipmentLocation, SkillRequirement> parseRequirements(
            @Nonnull String itemId,
            @Nonnull Map<String, String[]> rawTags) {

        String[] skillEntries = rawTags.get(this.equipmentConfig.tagSkillRequired());
        if (skillEntries == null || skillEntries.length == 0) {
            return Collections.emptyMap();
        }

        String[] levelEntries = rawTags.get(this.equipmentConfig.tagLevelRequirement());
        EnumMap<EquipmentLocation, SkillRequirement> parsed = new EnumMap<>(EquipmentLocation.class);
        for (int i = 0; i < skillEntries.length; i++) {
            ParsedSkillEntry skillEntry = parseSkillEntry(itemId, i, skillEntries[i]);
            if (skillEntry == null) {
                continue;
            }

            String rawLevel = levelEntries != null && i < levelEntries.length ? levelEntries[i] : null;
            int level = parseLevelEntry(itemId, i, rawLevel, skillEntry.location());
            parsed.put(skillEntry.location(), new SkillRequirement(skillEntry.skillType(), level));
        }
        return parsed.isEmpty() ? Collections.emptyMap() : Map.copyOf(parsed);
    }

    @Nullable
    private ParsedSkillEntry parseSkillEntry(@Nonnull String itemId, int index, @Nullable String rawSkillEntry) {
        ParsedPair pair = parsePair(rawSkillEntry);
        if (pair == null) {
            LOGGER.atWarning().log("Invalid %s entry '%s' at index %d on item %s; expected <location>%s<skill>",
                    this.equipmentConfig.tagSkillRequired(),
                    rawSkillEntry,
                    index,
                    itemId,
                    this.equipmentConfig.tagValueSeparator());
            return null;
        }

        EquipmentLocation location = resolveLocation(pair.left());
        if (location == null) {
            LOGGER.atWarning().log("Unknown location '%s' in %s at index %d on item %s; skipping",
                    pair.left(),
                    this.equipmentConfig.tagSkillRequired(),
                    index,
                    itemId);
            return null;
        }

        SkillType skillType = SkillType.tryParseStrict(pair.right());
        if (skillType == null) {
            LOGGER.atWarning().log("Invalid skill '%s' in %s at index %d on item %s; skipping",
                    pair.right(),
                    this.equipmentConfig.tagSkillRequired(),
                    index,
                    itemId);
            return null;
        }

        return new ParsedSkillEntry(location, skillType);
    }

    private int parseLevelEntry(
            @Nonnull String itemId,
            int index,
            @Nullable String rawLevelEntry,
            @Nonnull EquipmentLocation expectedLocation) {
        if (rawLevelEntry == null || rawLevelEntry.isBlank()) {
            return this.equipmentConfig.defaultRequiredLevel();
        }

        ParsedPair pair = parsePair(rawLevelEntry);
        String levelToken = rawLevelEntry;
        if (pair != null) {
            levelToken = pair.right();
            EquipmentLocation levelLocation = resolveLocation(pair.left());
            if (levelLocation != null
                    && this.equipmentConfig.requireLocationMatchBetweenTags()
                    && levelLocation != expectedLocation) {
                LOGGER.atWarning().log("Location mismatch between %s and %s at index %d on item %s (%s vs %s)",
                        this.equipmentConfig.tagSkillRequired(),
                        this.equipmentConfig.tagLevelRequirement(),
                        index,
                        itemId,
                        expectedLocation.canonicalKey(),
                        levelLocation.canonicalKey());
            }
        }

        try {
            int parsedLevel = Integer.parseInt(levelToken.trim());
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

    @Nullable
    private ParsedPair parsePair(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String separator = this.equipmentConfig.tagValueSeparator();
        int separatorIndex = raw.indexOf(separator);
        if (separatorIndex <= 0 || separatorIndex >= raw.length() - separator.length()) {
            return null;
        }

        String left = raw.substring(0, separatorIndex).trim();
        String right = raw.substring(separatorIndex + separator.length()).trim();
        if (left.isEmpty() || right.isEmpty()) {
            return null;
        }
        return new ParsedPair(left, right);
    }

    @Nullable
    private EquipmentLocation resolveLocation(@Nonnull String rawLocation) {
        return this.locationsByAlias.get(EquipmentLocation.normalize(rawLocation));
    }

    @Nonnull
    private static Map<String, EquipmentLocation> buildAliasMap(@Nonnull Map<String, List<String>> aliasesByLocation) {
        Map<String, EquipmentLocation> mapped = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : aliasesByLocation.entrySet()) {
            EquipmentLocation location = EquipmentLocation.fromCanonicalKey(entry.getKey());
            if (location == null) {
                continue;
            }

            mapped.put(location.canonicalKey(), location);
            for (String alias : entry.getValue()) {
                mapped.put(EquipmentLocation.normalize(alias), location);
            }
        }
        return Map.copyOf(mapped);
    }

    private record ParsedPair(@Nonnull String left, @Nonnull String right) {
    }

    private record ParsedSkillEntry(@Nonnull EquipmentLocation location, @Nonnull SkillType skillType) {
    }
}
