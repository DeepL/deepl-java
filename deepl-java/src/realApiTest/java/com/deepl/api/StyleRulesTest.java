// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class StyleRulesTest {
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void sendsTheStyleRuleRequestsCreateGetListRenameConfigureInstructionsAndDelete()
      throws Exception {
    DeepLClient client = RealApiClient.createClient();
    String name = RealApiNames.getUniqueName("style-rule");
    ConfiguredRules rules =
        new ConfiguredRules(
            Collections.singletonMap("calendar_era", "use_bc_and_ad"),
            null,
            null,
            null,
            null,
            null,
            null);

    StyleRuleInfo styleRule = client.createStyleRule(name, "en", rules, null);
    String styleId = styleRule.getStyleId();
    try {
      assertNotNull(styleId);
      assertEquals(name, styleRule.getName());
      assertEquals("en", styleRule.getLanguage());
      assertNotNull(styleRule.getCreationTime());

      StyleRuleInfo fetched = client.getStyleRule(styleId);
      assertEquals(styleId, fetched.getStyleId());
      assertEquals(name, fetched.getName());

      boolean listed = false;
      for (int page = 0; !listed && page < 20; page++) {
        List<StyleRuleInfo> styleRules = client.getAllStyleRules(page, 25, true);
        if (styleRules.isEmpty()) {
          break;
        }
        listed = styleRules.stream().anyMatch(rule -> rule.getStyleId().equals(styleId));
      }
      assertTrue(listed);

      StyleRuleInfo renamed = client.updateStyleRuleName(styleId, name + "-renamed");
      assertEquals(name + "-renamed", renamed.getName());

      StyleRuleInfo configured = client.updateStyleRuleConfiguredRules(styleId, rules);
      assertEquals(styleId, configured.getStyleId());

      CustomInstruction instruction =
          client.createStyleRuleCustomInstruction(styleId, "Label", "Prompt", "de");
      String instructionId = instruction.getId();
      assertFalse(instructionId == null || instructionId.isEmpty());
      assertEquals("Label", instruction.getLabel());

      CustomInstruction fetchedInstruction =
          client.getStyleRuleCustomInstruction(styleId, instructionId);
      assertEquals("Prompt", fetchedInstruction.getPrompt());

      CustomInstruction updatedInstruction =
          client.updateStyleRuleCustomInstruction(
              styleId, instructionId, "New Label", "New Prompt", "en");
      assertEquals("New Label", updatedInstruction.getLabel());

      client.deleteStyleRuleCustomInstruction(styleId, instructionId);
    } finally {
      try {
        client.deleteStyleRule(styleId);
      } catch (NotFoundException ignored) {
        // Already gone
      }
    }
  }
}
