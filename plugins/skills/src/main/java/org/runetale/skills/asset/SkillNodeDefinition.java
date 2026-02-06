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
			.append(new KeyedCodec<>("DepletionChance", Codec.DOUBLE), (o, i) -> o.depletionChance = i,
					SkillNodeDefinition::getDepletionChance)
			.add()
			.append(new KeyedCodec<>("Depletes", Codec.BOOLEAN), (o, i) -> o.depletes = i,
					SkillNodeDefinition::isDepletes)
			.add()
			.append(new KeyedCodec<>("RespawnSeconds", Codec.INTEGER), (o, i) -> o.respawnSeconds = i,
					SkillNodeDefinition::getRespawnSeconds)
			.add()
			.build();

	private String id = "unknown";
	private SkillType skillType = SkillType.WOODCUTTING;
	private String blockId = "Empty";
	private int requiredSkillLevel = 1;
	private ToolTier requiredToolTier = ToolTier.NONE;
	private String requiredToolKeyword = "axe";
	private double experienceReward = 0.0D;
	/**
	 * Probability (0.0..1.0) that this node depletes on a successful gather.
	 *
	 * <p>
	 * A value of 1.0 means always deplete; 0.0 means never deplete. This keeps
	 * OSRS-like depletion behavior data-driven without hardcoding chance logic in
	 * event systems.
	 */
	private double depletionChance = 1.0D;
	private boolean depletes = true;
	private int respawnSeconds = 5;

	/**
	 * Codec constructor.
	 */
	protected SkillNodeDefinition() {
	}

	public SkillNodeDefinition(String id, SkillType skillType, String blockId, int requiredSkillLevel,
			ToolTier requiredToolTier, String requiredToolKeyword, double experienceReward,
			double depletionChance, boolean depletes, int respawnSeconds) {
		this.id = id;
		this.skillType = skillType;
		this.blockId = blockId;
		this.requiredSkillLevel = requiredSkillLevel;
		this.requiredToolTier = requiredToolTier;
		this.requiredToolKeyword = requiredToolKeyword;
		this.experienceReward = experienceReward;
		this.depletionChance = depletionChance;
		this.depletes = depletes;
		this.respawnSeconds = respawnSeconds;
	}

	public String getId() {
		return id;
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

	/**
	 * Returns depletion chance clamped to [0.0, 1.0] for defensive runtime use.
	 */
	public double getDepletionChance() {
		return Math.max(0.0D, Math.min(1.0D, depletionChance));
	}

	public boolean isDepletes() {
		return depletes;
	}

	public int getRespawnSeconds() {
		return respawnSeconds;
	}
}
