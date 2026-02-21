package org.runetale.skills.equipment.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentRequirementTagServiceTest {

    @Test
    void parseRequirementsMapsSkillAndLevelByPosition() {
        EquipmentRequirementTagService service = new EquipmentRequirementTagService(config());

        List<SkillRequirement> parsed = service.parseRequirements(
                "RuneTale_Sword",
                Map.of(
                        "EquipSkillRequirement", new String[]{"attack", "mining"},
                        "EquipLevelRequirement", new String[]{"20", "40"}));

        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0).skillType()).isEqualTo(SkillType.ATTACK);
        assertThat(parsed.get(0).requiredLevel()).isEqualTo(20);
        assertThat(parsed.get(1).skillType()).isEqualTo(SkillType.MINING);
        assertThat(parsed.get(1).requiredLevel()).isEqualTo(40);
    }

    @Test
    void parseRequirementsDefaultsInvalidLevelAndSkipsInvalidSkill() {
        EquipmentRequirementTagService service = new EquipmentRequirementTagService(config());

        List<SkillRequirement> parsed = service.parseRequirements(
                "RuneTale_Helmet",
                Map.of(
                        "EquipSkillRequirement", new String[]{"defence", "not_a_skill"},
                        "EquipLevelRequirement", new String[]{"NaN", "50"}));

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).skillType()).isEqualTo(SkillType.DEFENCE);
        assertThat(parsed.get(0).requiredLevel()).isEqualTo(1);
    }

    @Test
    void parseRequirementsIgnoresLegacyPairFormatWithoutSkillTag() {
        EquipmentRequirementTagService service = new EquipmentRequirementTagService(config());

        List<SkillRequirement> parsed = service.parseRequirements(
                "RuneTale_Legacy_Item",
                Map.of("EquipLevelRequirement", new String[]{"Attack", "60"}));

        assertThat(parsed).isEmpty();
    }

    private static EquipmentConfig config() {
        return new EquipmentConfig(
                "EquipSkillRequirement",
                "EquipLevelRequirement",
                ":",
                false,
                1,
                true,
                true,
                false,
                true,
                true,
                true,
                -1,
                -8,
                9,
                9,
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
