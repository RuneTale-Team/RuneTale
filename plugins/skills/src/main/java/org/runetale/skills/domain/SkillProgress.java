package org.runetale.skills.domain;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Serializable per-skill progression snapshot.
 *
 * <p>
 * Both XP and level are stored for forward compatibility, auditing, and easier
 * debugging,
 * while runtime logic still derives/validates level from XP through the XP
 * service.
 */
public class SkillProgress {

	/**
	 * Builder codec used by parent profile component persistence.
	 */
	public static final BuilderCodec<SkillProgress> CODEC = BuilderCodec
			.builder(SkillProgress.class, SkillProgress::new)
			.append(new KeyedCodec<>("Experience", Codec.LONG), (o, i) -> o.experience = i,
					SkillProgress::getExperience)
			.add()
			.append(new KeyedCodec<>("Level", Codec.INTEGER), (o, i) -> o.level = i, SkillProgress::getLevel)
			.add()
			.build();

	private long experience;
	private int level;

	/**
	 * Codec constructor.
	 */
	protected SkillProgress() {
		this(0L, 1);
	}

	public SkillProgress(long experience, int level) {
		this.experience = Math.max(0L, experience);
		this.level = Math.max(1, level);
	}

	public long getExperience() {
		return experience;
	}

	public int getLevel() {
		return level;
	}

	/**
	 * Applies normalized progression values.
	 */
	public void set(long experience, int level) {
		this.experience = Math.max(0L, experience);
		this.level = Math.max(1, level);
	}
}
