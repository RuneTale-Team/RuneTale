package org.runetale.skills.service;

public final class GatheringBypassService {

	public static final String MANAGE_PERMISSION = "runetale.skills.gathering.bypass.manage";
	public static final String OP_EXEMPT_PERMISSION = "runetale.skills.gathering.bypass.exempt";

	private volatile boolean opExemptionEnabled;

	public boolean isOpExemptionEnabled() {
		return this.opExemptionEnabled;
	}

	public void setOpExemptionEnabled(boolean enabled) {
		this.opExemptionEnabled = enabled;
	}
}
