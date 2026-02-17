package org.runetale.skills.service;

import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;

/**
 * Resolves block-break outcomes for gathering node interactions.
 */
public class SkillNodeBreakResolutionService {

	private final List<String> nodeCandidateTokens;

	public SkillNodeBreakResolutionService() {
		this(List.of("log", "tree", "ore", "rock"));
	}

	public SkillNodeBreakResolutionService(@Nonnull List<String> nodeCandidateTokens) {
		this.nodeCandidateTokens = List.copyOf(nodeCandidateTokens);
	}

	@Nonnull
	public SkillNodeBreakResolutionResult resolveMissingNode(@Nonnull String blockId) {
		if (!looksLikeSkillNodeCandidate(blockId)) {
			return SkillNodeBreakResolutionResult.noAction();
		}

		return SkillNodeBreakResolutionResult
				.cancelWithWarning("[Skills] This resource is not configured yet. Try a supported node.");
	}

	@Nonnull
	public SkillNodeBreakResolutionResult resolveConfiguredNode(
			@Nonnull SkillNodeDefinition node,
			int currentLevel,
			@Nonnull RequirementCheckResult toolCheck) {
		if (currentLevel < node.getRequiredSkillLevel()) {
			SkillType skill = node.getSkillType();
			String message = String.format(Locale.ROOT,
					"[Skills] %s level %d/%d (current/required).",
					formatSkillName(skill),
					currentLevel,
					node.getRequiredSkillLevel());
			return SkillNodeBreakResolutionResult.cancelWithWarning(message);
		}

		if (!toolCheck.isSuccess()) {
			return SkillNodeBreakResolutionResult.noAction();
		}

		return SkillNodeBreakResolutionResult.dispatchXp(
				node.getSkillType(),
				node.getExperienceReward(),
				"node:" + node.getId());
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skill) {
		String name = skill.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private boolean looksLikeSkillNodeCandidate(@Nonnull String blockId) {
		String lowered = blockId.toLowerCase(Locale.ROOT);
		for (String token : this.nodeCandidateTokens) {
			if (lowered.contains(token)) {
				return true;
			}
		}
		return false;
	}
}
