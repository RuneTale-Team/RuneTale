package org.runetale.skills.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugModeServiceTest {

	@Test
	void enableAndDisableAreCaseInsensitiveForSupportedPlugins() {
		DebugModeService service = new DebugModeService(List.of("skills"));

		assertFalse(service.isEnabled("skills"));
		assertTrue(service.enable("SKILLS"));
		assertTrue(service.isEnabled("skills"));
		assertTrue(service.disable("sKiLlS"));
		assertFalse(service.isEnabled("skills"));
	}

	@Test
	void unknownPluginCannotBeToggled() {
		DebugModeService service = new DebugModeService(List.of("skills"));

		assertFalse(service.isSupported("combat"));
		assertFalse(service.enable("combat"));
		assertFalse(service.disable("combat"));
		assertFalse(service.isEnabled("combat"));
	}
}
