// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StyleRulesTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String RULE =
      "{\"style_id\":\"s1\",\"name\":\"n\",\"creation_time\":\"2021-08-03T14:16:18.329Z\","
          + "\"updated_time\":\"2021-08-03T14:16:18.329Z\",\"language\":\"en\",\"version\":3}";
  private static final String DETAILED_RULE =
      "{\"style_id\":\"s1\",\"name\":\"n\",\"creation_time\":\"2021-08-03T14:16:18.329Z\","
          + "\"updated_time\":\"2021-08-03T14:16:18.329Z\",\"language\":\"en\",\"version\":3,"
          + "\"configured_rules\":{\"dates_and_times\":{\"calendar_era\":\"use_bc_and_ad\"},"
          + "\"numbers\":{\"x\":\"y\"}},"
          + "\"custom_instructions\":[{\"id\":\"i1\",\"label\":\"L\",\"prompt\":\"P\","
          + "\"source_language\":\"de\"}]}";
  private static final String INSTRUCTION =
      "{\"id\":\"i1\",\"label\":\"L\",\"prompt\":\"P\",\"source_language\":\"de\"}";
  private static final String JSON = "application/json";

  private final DeepLClient client = OfflineClient.createClient(wm);

  private static ConfiguredRules calendarRule() {
    return new ConfiguredRules(
        Collections.singletonMap("calendar_era", "use_bc_and_ad"),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void sendsPageParametersWhenListingStyleRules() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/style_rules?page=0&page_size=10&detailed=true"))
            .withHeader("Content-Type", absent())
            .willReturn(okJson("{\"style_rules\":[" + DETAILED_RULE + "]}")));
    client.getAllStyleRules(0, 10, true);
  }

  @Test
  void sendsNoQueryWhenListingStyleRulesWithoutParameters() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/style_rules")).willReturn(okJson("{\"style_rules\":[" + RULE + "]}")));
    client.getAllStyleRules();
  }

  @Test
  void sendsOnlyTheGivenParametersWhenListingStyleRules() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/style_rules?detailed=false"))
            .willReturn(okJson("{\"style_rules\":[]}")));
    client.getAllStyleRules(null, null, false);
  }

  @Test
  void parsesDetailedStyleRules() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/style_rules?detailed=true"))
            .willReturn(okJson("{\"style_rules\":[" + DETAILED_RULE + "]}")));
    List<StyleRuleInfo> rules = client.getAllStyleRules(null, null, true);
    StyleRuleInfo rule = rules.get(0);
    assertEquals("s1", rule.getStyleId());
    assertEquals(1628000178329L, rule.getCreationTime().getTime());
    assertEquals(1628000178329L, rule.getUpdatedTime().getTime());
    Map<String, String> dates = rule.getConfiguredRules().getDatesAndTimes();
    assertEquals("use_bc_and_ad", dates.get("calendar_era"));
    assertNull(rule.getConfiguredRules().getVocabulary());
    CustomInstruction instruction = rule.getCustomInstructions().get(0);
    assertEquals("de", instruction.getSourceLanguage());
  }

  @Test
  void parsesStyleRulesWithoutDetailsAsNull() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/style_rules")).willReturn(okJson("{\"style_rules\":[" + RULE + "]}")));
    StyleRuleInfo rule = client.getAllStyleRules().get(0);
    assertNull(rule.getConfiguredRules());
    assertNull(rule.getCustomInstructions());
  }

  @Test
  void sendsOnlyNameAndLanguageWhenCreatingAStyleRule() throws Exception {
    wm.stubFor(
        post("/v3/style_rules")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(equalToJson("{\"name\":\"Rule\",\"language\":\"en\"}"))
            .willReturn(okJson(RULE)));
    client.createStyleRule("Rule", "en", null, null);
  }

  @Test
  void sendsConfiguredRulesAndCustomInstructionsWhenCreatingAStyleRule() throws Exception {
    wm.stubFor(
        post("/v3/style_rules")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson(
                    "{\"name\":\"Rule\",\"language\":\"en\","
                        + "\"configured_rules\":{\"dates_and_times\":{\"calendar_era\":\"use_bc_and_ad\"}},"
                        + "\"custom_instructions\":[{\"label\":\"L\",\"prompt\":\"P\"},"
                        + "{\"label\":\"L2\",\"prompt\":\"P2\",\"source_language\":\"de\"}]}"))
            .willReturn(okJson(RULE)));
    client.createStyleRule(
        "Rule",
        "en",
        calendarRule(),
        Arrays.asList(
            new CustomInstruction("L", "P", null), new CustomInstruction("L2", "P2", "de")));
  }

  @Test
  void parsesTheCreatedStyleRule() throws Exception {
    wm.stubFor(post("/v3/style_rules").willReturn(okJson(RULE)));
    StyleRuleInfo rule = client.createStyleRule("Rule", "en", null, null);
    assertEquals("s1", rule.getStyleId());
  }

  @Test
  void sendsAGetRequestWhenGettingAStyleRule() throws Exception {
    wm.stubFor(
        get("/v3/style_rules/s1").withHeader("Content-Type", absent()).willReturn(okJson(RULE)));
    client.getStyleRule("s1");
  }

  @Test
  void sendsTheNameAsJsonPatchWhenRenamingAStyleRule() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/style_rules/s1"))
            .withHeader("Content-Type", equalTo(JSON))
            .withHeader("Authorization", equalTo("DeepL-Auth-Key k"))
            .withRequestBody(equalToJson("{\"name\":\"New \\\"name\\\" é\"}"))
            .willReturn(okJson(RULE)));
    client.updateStyleRuleName("s1", "New \"name\" é");
  }

  @Test
  void sendsConfiguredRulesAsJsonPutWhenUpdatingConfiguredRules() throws Exception {
    wm.stubFor(
        put("/v3/style_rules/s1/configured_rules")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson("{\"dates_and_times\":{\"calendar_era\":\"use_bc_and_ad\"}}"))
            .willReturn(okJson(RULE)));
    client.updateStyleRuleConfiguredRules("s1", calendarRule());
  }

  @Test
  void sendsADeleteRequestWhenDeletingAStyleRule() throws Exception {
    wm.stubFor(delete("/v3/style_rules/s1").willReturn(aResponse().withStatus(204)));
    client.deleteStyleRule("s1");
  }

  @Test
  void sendsLabelPromptAndSourceLanguageWhenCreatingACustomInstruction() throws Exception {
    wm.stubFor(
        post("/v3/style_rules/s1/custom_instructions")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson("{\"label\":\"L\",\"prompt\":\"P\",\"source_language\":\"de\"}"))
            .willReturn(okJson(INSTRUCTION)));
    client.createStyleRuleCustomInstruction("s1", "L", "P", "de");
  }

  @Test
  void omitsTheSourceLanguageWhenItIsNull() throws Exception {
    wm.stubFor(
        post("/v3/style_rules/s1/custom_instructions")
            .withRequestBody(equalToJson("{\"label\":\"L\",\"prompt\":\"P\"}"))
            .willReturn(okJson(INSTRUCTION)));
    client.createStyleRuleCustomInstruction("s1", "L", "P", null);
  }

  @Test
  void sendsAGetRequestWhenGettingACustomInstruction() throws Exception {
    wm.stubFor(
        get("/v3/style_rules/s1/custom_instructions/i1")
            .withHeader("Content-Type", absent())
            .willReturn(okJson(INSTRUCTION)));
    client.getStyleRuleCustomInstruction("s1", "i1");
  }

  @Test
  void sendsJsonPutWhenUpdatingACustomInstruction() throws Exception {
    wm.stubFor(
        put("/v3/style_rules/s1/custom_instructions/i1")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(equalToJson("{\"label\":\"L2\",\"prompt\":\"P2\"}"))
            .willReturn(okJson(INSTRUCTION)));
    client.updateStyleRuleCustomInstruction("s1", "i1", "L2", "P2", null);
  }

  @Test
  void sendsADeleteRequestWhenDeletingACustomInstruction() throws Exception {
    wm.stubFor(
        delete("/v3/style_rules/s1/custom_instructions/i1")
            .willReturn(aResponse().withStatus(204)));
    client.deleteStyleRuleCustomInstruction("s1", "i1");
  }

  @Test
  void encodesTheIdsInStyleRulePaths() throws Exception {
    String rulePath = "/v3/style_rules/a%2Fb%20c%3F";
    String instructionPath = rulePath + "/custom_instructions/i%2F1%23";
    wm.stubFor(get(urlEqualTo(rulePath)).willReturn(okJson(RULE)));
    wm.stubFor(patch(urlEqualTo(rulePath)).willReturn(okJson(RULE)));
    wm.stubFor(delete(urlEqualTo(rulePath)).willReturn(aResponse().withStatus(204)));
    wm.stubFor(put(urlEqualTo(rulePath + "/configured_rules")).willReturn(okJson(RULE)));
    wm.stubFor(post(urlEqualTo(rulePath + "/custom_instructions")).willReturn(okJson(INSTRUCTION)));
    wm.stubFor(get(urlEqualTo(instructionPath)).willReturn(okJson(INSTRUCTION)));
    wm.stubFor(put(urlEqualTo(instructionPath)).willReturn(okJson(INSTRUCTION)));
    wm.stubFor(delete(urlEqualTo(instructionPath)).willReturn(aResponse().withStatus(204)));
    String id = "a/b c?";
    client.getStyleRule(id);
    client.updateStyleRuleName(id, "n");
    client.deleteStyleRule(id);
    client.updateStyleRuleConfiguredRules(id, calendarRule());
    client.createStyleRuleCustomInstruction(id, "L", "P", null);
    client.getStyleRuleCustomInstruction(id, "i/1#");
    client.updateStyleRuleCustomInstruction(id, "i/1#", "L", "P", null);
    client.deleteStyleRuleCustomInstruction(id, "i/1#");
  }

  @Test
  void throwsNotFoundWhenTheStyleRuleIsUnknown() {
    wm.stubFor(get("/v3/style_rules/s1").willReturn(aResponse().withStatus(404)));
    assertThrows(NotFoundException.class, () -> client.getStyleRule("s1"));
  }

  @Test
  void throwsIllegalArgumentWhenAnArgumentIsEmpty() {
    ConfiguredRules rules = new ConfiguredRules(null, null, null, null, null, null, null);
    assertThrows(
        IllegalArgumentException.class, () -> client.createStyleRule("", "en", null, null));
    assertThrows(
        IllegalArgumentException.class, () -> client.createStyleRule("Test", "", null, null));
    assertThrows(IllegalArgumentException.class, () -> client.getStyleRule(""));
    assertThrows(IllegalArgumentException.class, () -> client.updateStyleRuleName("", "New"));
    assertThrows(IllegalArgumentException.class, () -> client.updateStyleRuleName("s1", ""));
    assertThrows(IllegalArgumentException.class, () -> client.deleteStyleRule(""));
    assertThrows(
        IllegalArgumentException.class, () -> client.updateStyleRuleConfiguredRules("", rules));
    assertThrows(
        IllegalArgumentException.class, () -> client.updateStyleRuleConfiguredRules("s1", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.createStyleRuleCustomInstruction("", "L", "P", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.createStyleRuleCustomInstruction("s1", "", "P", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.createStyleRuleCustomInstruction("s1", "L", "", null));
    assertThrows(
        IllegalArgumentException.class, () -> client.getStyleRuleCustomInstruction("", "i1"));
    assertThrows(
        IllegalArgumentException.class, () -> client.getStyleRuleCustomInstruction("s1", ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.updateStyleRuleCustomInstruction("", "i1", "L", "P", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.updateStyleRuleCustomInstruction("s1", "", "L", "P", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.updateStyleRuleCustomInstruction("s1", "i1", "", "P", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.updateStyleRuleCustomInstruction("s1", "i1", "L", "", null));
    assertThrows(
        IllegalArgumentException.class, () -> client.deleteStyleRuleCustomInstruction("", "i1"));
    assertThrows(
        IllegalArgumentException.class, () -> client.deleteStyleRuleCustomInstruction("s1", ""));
  }
}
