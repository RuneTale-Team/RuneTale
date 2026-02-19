package org.runetale.skills.domain;

/**
 * Immutable result object for requirement checks.
 */
public class RequirementCheckResult {

	private final boolean success;
	private final ToolTier detectedTier;
	private final String heldItemId;

	private RequirementCheckResult(boolean success, ToolTier detectedTier, String heldItemId) {
		this.success = success;
		this.detectedTier = detectedTier;
		this.heldItemId = heldItemId;
	}

	public static RequirementCheckResult success(ToolTier detectedTier, String heldItemId) {
		return new RequirementCheckResult(true, detectedTier, heldItemId);
	}

	public static RequirementCheckResult failure(ToolTier detectedTier, String heldItemId) {
		return new RequirementCheckResult(false, detectedTier, heldItemId);
	}

	public boolean isSuccess() {
		return success;
	}

	public ToolTier getDetectedTier() {
		return detectedTier;
	}

	public String getHeldItemId() {
		return heldItemId;
	}
}
