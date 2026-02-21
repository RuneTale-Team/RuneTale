package org.runetale.skills.equipment.domain;

import org.runetale.skills.domain.SkillRequirement;

import javax.annotation.Nonnull;

public record EquipmentSkillRequirement(
        @Nonnull EquipmentLocation location,
        @Nonnull SkillRequirement requirement) {
}
