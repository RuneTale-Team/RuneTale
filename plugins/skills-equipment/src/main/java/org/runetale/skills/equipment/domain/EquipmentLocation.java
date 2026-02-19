package org.runetale.skills.equipment.domain;

import com.hypixel.hytale.protocol.ItemArmorSlot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public enum EquipmentLocation {
    MAINHAND("mainhand", "Main hand"),
    OFFHAND("offhand", "Off hand"),
    HEAD("head", "Head"),
    CHEST("chest", "Chest"),
    HANDS("hands", "Hands"),
    LEGS("legs", "Legs");

    private final String canonicalKey;
    private final String displayName;

    EquipmentLocation(@Nonnull String canonicalKey, @Nonnull String displayName) {
        this.canonicalKey = canonicalKey;
        this.displayName = displayName;
    }

    @Nonnull
    public String canonicalKey() {
        return this.canonicalKey;
    }

    @Nonnull
    public String displayName() {
        return this.displayName;
    }

    @Nullable
    public static EquipmentLocation fromCanonicalKey(@Nonnull String raw) {
        String normalized = normalize(raw);
        for (EquipmentLocation value : values()) {
            if (value.canonicalKey.equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    @Nonnull
    public static EquipmentLocation fromArmorSlot(@Nonnull ItemArmorSlot slot) {
        return switch (slot) {
            case Head -> HEAD;
            case Chest -> CHEST;
            case Hands -> HANDS;
            case Legs -> LEGS;
        };
    }

    @Nonnull
    public static String normalize(@Nonnull String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
