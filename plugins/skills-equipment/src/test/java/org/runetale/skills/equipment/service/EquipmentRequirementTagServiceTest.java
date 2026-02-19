package org.runetale.skills.equipment.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.equipment.domain.EquipmentLocation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentRequirementTagServiceTest {

    @Test
    void parseRequirementsMapsSkillAndLevelByPositionAndLocation() {
        EquipmentRequirementTagService service = new EquipmentRequirementTagService(config());

        Map<EquipmentLocation, org.runetale.skills.domain.SkillRequirement> parsed = service.parseRequirements(
                "RuneTale_Sword",
                Map.of(
                        "EquipSkillRequired", new String[]{"mainhand:attack", "head:defense"},
                        "EquipLevelRequirement", new String[]{"mainhand:20", "head:40"}));

        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(EquipmentLocation.MAINHAND).skillType()).isEqualTo(SkillType.ATTACK);
        assertThat(parsed.get(EquipmentLocation.MAINHAND).requiredLevel()).isEqualTo(20);
        assertThat(parsed.get(EquipmentLocation.HEAD).skillType()).isEqualTo(SkillType.DEFENSE);
        assertThat(parsed.get(EquipmentLocation.HEAD).requiredLevel()).isEqualTo(40);
    }

    @Test
    void parseRequirementsDefaultsInvalidLevelAndSkipsInvalidSkill() {
        EquipmentRequirementTagService service = new EquipmentRequirementTagService(config());

        Map<EquipmentLocation, org.runetale.skills.domain.SkillRequirement> parsed = service.parseRequirements(
                "RuneTale_Helmet",
                Map.of(
                        "EquipSkillRequired", new String[]{"head:defense", "mainhand:not_a_skill"},
                        "EquipLevelRequirement", new String[]{"head:NaN", "mainhand:50"}));

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(EquipmentLocation.HEAD).skillType()).isEqualTo(SkillType.DEFENSE);
        assertThat(parsed.get(EquipmentLocation.HEAD).requiredLevel()).isEqualTo(1);
    }

    private static EquipmentConfig config() {
        return new EquipmentConfig(
                "EquipSkillRequired",
                "EquipLevelRequirement",
                ":",
                false,
                1,
                true,
                true,
                -1,
                -8,
                0.25F,
                1000L,
                "[Skills] %s level %d/%d required to equip %s in %s.",
                Map.of(
                        "mainhand", List.of("mainhand", "main_hand"),
                        "head", List.of("head"),
                        "chest", List.of("chest"),
                        "hands", List.of("hands"),
                        "legs", List.of("legs"),
                        "offhand", List.of("offhand")),
                "skills-equipment");
    }
}
