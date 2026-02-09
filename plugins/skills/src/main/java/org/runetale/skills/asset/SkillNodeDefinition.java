package org.runetale.skills.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;

/**
 * Serializable, data-oriented definition for a gatherable skill node.
 *
 * <p>
 * This model is intentionally independent from runtime systems so it can be
 * loaded from any
 * future source (assets, config files, network payloads, or test fixtures) with
 * minimal changes.
 */
public class SkillNodeDefinition {

	/**
	 * Builder codec enabling externalized data loading in a future-proof format.
	 */
	public static final BuilderCodec<SkillNodeDefinition> CODEC = BuilderCodec
			.builder(SkillNodeDefinition.class, SkillNodeDefinition::new)
			.append(new KeyedCodec<>("Id", Codec.STRING), (o, i) -> o.id = i, SkillNodeDefinition::getId)
			.add()
			.append(new KeyedCodec<>("Label", Codec.STRING), (o, i) -> o.label = i, SkillNodeDefinition::getLabel)
			.add()
			.append(new KeyedCodec<>("Skill", new EnumCodec<>(SkillType.class)), (o, i) -> o.skillType = i,
					SkillNodeDefinition::getSkillType)
			.add()
			.append(new KeyedCodec<>("BlockId", Codec.STRING), (o, i) -> o.blockId = i, SkillNodeDefinition::getBlockId)
			.add()
			.append(new KeyedCodec<>("RequiredSkillLevel", Codec.INTEGER), (o, i) -> o.requiredSkillLevel = i,
					SkillNodeDefinition::getRequiredSkillLevel)
			.add()
			.append(new KeyedCodec<>("RequiredToolTier", new EnumCodec<>(ToolTier.class)),
					(o, i) -> o.requiredToolTier = i, SkillNodeDefinition::getRequiredToolTier)
			.add()
			.append(new KeyedCodec<>("RequiredToolKeyword", Codec.STRING), (o, i) -> o.requiredToolKeyword = i,
					SkillNodeDefinition::getRequiredToolKeyword)
			.add()
			.append(new KeyedCodec<>("ExperienceReward", Codec.DOUBLE), (o, i) -> o.experienceReward = i,
					SkillNodeDefinition::getExperienceReward)
			.add()
			.build();

	private String id = "unknown";
	private String label = "";
	private SkillType skillType = SkillType.WOODCUTTING;
	private String blockId = "Empty";
	private int requiredSkillLevel = 1;
	private ToolTier requiredToolTier = ToolTier.NONE;
	private String requiredToolKeyword = "axe";
	private double experienceReward = 0.0D;

	/**
	 * Codec constructor.
	 */
	protected SkillNodeDefinition() {
	}

	public SkillNodeDefinition(String id, SkillType skillType, String blockId, int requiredSkillLevel,
			ToolTier requiredToolTier, String requiredToolKeyword, double experienceReward) {
		this(id, "", skillType, blockId, requiredSkillLevel, requiredToolTier, requiredToolKeyword,
				experienceReward);
	}

	public SkillNodeDefinition(String id, String label, SkillType skillType, String blockId, int requiredSkillLevel,
			ToolTier requiredToolTier, String requiredToolKeyword, double experienceReward) {
		this.id = id;
		this.label = label;
		this.skillType = skillType;
		this.blockId = blockId;
		this.requiredSkillLevel = requiredSkillLevel;
		this.requiredToolTier = requiredToolTier;
		this.requiredToolKeyword = requiredToolKeyword;
		this.experienceReward = experienceReward;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public SkillType getSkillType() {
		return skillType;
	}

	public String getBlockId() {
		return blockId;
	}

	public int getRequiredSkillLevel() {
		return requiredSkillLevel;
	}

	public ToolTier getRequiredToolTier() {
		return requiredToolTier;
	}

	public String getRequiredToolKeyword() {
		return requiredToolKeyword;
	}

	public double getExperienceReward() {
		return experienceReward;
	}
}
