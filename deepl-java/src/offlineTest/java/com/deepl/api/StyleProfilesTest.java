// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StyleProfilesTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String ID = "3f1c9d0e-7a24-4b58-9c31-8f0e5d2a1b47";
  private static final String PROFILE =
      "{\"style_profile_id\":\"3f1c9d0e-7a24-4b58-9c31-8f0e5d2a1b47\",\"name\":\"Legal, German\","
          + "\"description\":\"Contract wording for the German market.\",\"industry\":\"Legal\","
          + "\"glossary_ids\":[\"4d0a2e1c-6b93-4f7a-8d15-2c6e9a3b7f81\"],"
          + "\"style_rule_ids\":[\"bd0a38f3-1831-440b-a8dd-2c702e2325ab\"],"
          + "\"translation_memory_ids\":[\"a74d88fb-ed2a-4943-a664-a4512398b994\"],"
          + "\"glossary_priority_order_enabled\":true,"
          + "\"creation_time\":\"2026-04-01T16:34:25.223Z\","
          + "\"updated_time\":\"2026-08-06T09:12:44.108Z\"}";
  private static final String MINIMAL_PROFILE =
      "{\"style_profile_id\":\"3f1c9d0e-7a24-4b58-9c31-8f0e5d2a1b47\",\"name\":\"n\","
          + "\"glossary_ids\":[],\"style_rule_ids\":[],\"translation_memory_ids\":[],"
          + "\"glossary_priority_order_enabled\":false,"
          + "\"creation_time\":\"2026-10-26T16:27:10.2982869Z\","
          + "\"updated_time\":\"2026-10-26T16:27:10.2982869Z\","
          + "\"example_document_ids\":[\"x\"],\"spoken_terms_ids\":[\"y\"]}";

  private final DeepLClient client = OfflineClient.createClient(wm);

  @Test
  void sendsPageParametersOnlyWhenSetWhenListing() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/style_profiles"))
            .willReturn(okJson("{\"style_profiles\":[" + PROFILE + "],\"total_count\":1}")));
    wm.stubFor(
        get(urlEqualTo("/v3/style_profiles?page=2&page_size=100"))
            .willReturn(okJson("{\"style_profiles\":[],\"total_count\":1}")));

    List<StyleProfile> profiles = client.listStyleProfiles();
    assertEquals(1, profiles.size());
    assertEquals(ID, profiles.get(0).getStyleProfileId());
    assertTrue(client.listStyleProfiles(2, 100).isEmpty());
  }

  @Test
  void returnsAnEmptyListWhenTheListResponseHasNoStyleProfiles() throws Exception {
    wm.stubFor(get(urlEqualTo("/v3/style_profiles")).willReturn(okJson("{\"total_count\":0}")));

    assertTrue(client.listStyleProfiles().isEmpty());
  }

  @Test
  void sendsOnlyTheNameWhenCreatingWithoutOptions() throws Exception {
    wm.stubFor(
        post(urlEqualTo("/v3/style_profiles"))
            .withRequestBody(equalToJson("{\"name\":\"n\"}"))
            .willReturn(aResponse().withStatus(201).withBody(MINIMAL_PROFILE)));

    client.createStyleProfile("n");
  }

  @Test
  void sendsEveryOptionWhenCreating() throws Exception {
    wm.stubFor(
        post(urlEqualTo("/v3/style_profiles"))
            .withRequestBody(
                equalToJson(
                    "{\"name\":\"n\",\"description\":\"d\",\"industry\":\"Legal\","
                        + "\"glossary_ids\":[\"g2\",\"g1\"],\"style_rule_ids\":[\"s1\"],"
                        + "\"translation_memory_ids\":[\"t1\"],"
                        + "\"glossary_priority_order_enabled\":true}"))
            .willReturn(aResponse().withStatus(201).withBody(PROFILE)));

    client.createStyleProfile(
        "n",
        new CreateStyleProfileOptions()
            .setDescription("d")
            .setIndustry("Legal")
            .setGlossaryIds(Arrays.asList("g2", "g1"))
            .setStyleRuleIds(Collections.singletonList("s1"))
            .setTranslationMemoryIds(Collections.singletonList("t1"))
            .setGlossaryPriorityOrderEnabled(true));
  }

  @Test
  void sendsOnlyTheSetFieldsWhenUpdatingIncludingEmptyValues() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(equalToJson("{\"description\":\"\",\"style_rule_ids\":[]}"))
            .willReturn(okJson(MINIMAL_PROFILE)));

    client.updateStyleProfile(
        ID,
        new UpdateStyleProfileOptions()
            .setDescription("")
            .setStyleRuleIds(Collections.emptyList()));
  }

  @Test
  void sendsTheNameFromTheUpdateOptions() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(equalToJson("{\"name\":\"new\",\"industry\":\"Legal\"}"))
            .willReturn(okJson(MINIMAL_PROFILE)));

    client.updateStyleProfile(
        ID, new UpdateStyleProfileOptions().setName("new").setIndustry("Legal"));
  }

  @Test
  void addsExtraBodyParametersLastWhenCreatingAndUpdating() throws Exception {
    Map<String, String> extra = new HashMap<>();
    extra.put("description", "replaced");
    extra.put("beta", "on");
    wm.stubFor(
        post(urlEqualTo("/v3/style_profiles"))
            .withRequestBody(
                equalToJson("{\"name\":\"n\",\"description\":\"replaced\",\"beta\":\"on\"}"))
            .willReturn(aResponse().withStatus(201).withBody(MINIMAL_PROFILE)));
    wm.stubFor(
        patch(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(
                equalToJson("{\"name\":\"m\",\"description\":\"replaced\",\"beta\":\"on\"}"))
            .willReturn(okJson(MINIMAL_PROFILE)));

    client.createStyleProfile(
        "n", new CreateStyleProfileOptions().setDescription("d").setExtraBodyParameters(extra));
    client.updateStyleProfile(
        ID,
        new UpdateStyleProfileOptions()
            .setName("m")
            .setDescription("d")
            .setExtraBodyParameters(extra));
  }

  @Test
  void sendsTheIdsOfComponentObjects() throws Exception {
    wm.stubFor(
        post(urlEqualTo("/v3/style_profiles"))
            .withRequestBody(
                equalToJson(
                    "{\"name\":\"n\",\"glossary_ids\":[\"g1\",\"mg1\"],"
                        + "\"style_rule_ids\":[\"s1\"],\"translation_memory_ids\":[\"t1\",\"t2\"]}"))
            .willReturn(aResponse().withStatus(201).withBody(MINIMAL_PROFILE)));
    wm.stubFor(
        patch(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(equalToJson("{\"style_rule_ids\":[\"s1\"]}"))
            .willReturn(okJson(MINIMAL_PROFILE)));
    GlossaryInfo glossary = new GlossaryInfo("g1", "n", true, "en", "de", new Date(), 1);
    MultilingualGlossaryInfo multilingual =
        new MultilingualGlossaryInfo("mg1", "n", new Date(), Collections.emptyList());
    StyleRuleInfo rule = new StyleRuleInfo("s1", "n", new Date(), new Date(), "de", 1, null, null);
    TranslationMemoryInfo tm1 =
        new TranslationMemoryInfo("t1", "n", "de", Collections.singletonList("en"), 1);
    TranslationMemoryInfo tm2 =
        new TranslationMemoryInfo("t2", "n", "de", Collections.singletonList("en"), 1);

    client.createStyleProfile(
        "n",
        new CreateStyleProfileOptions()
            .setGlossaryIds(glossary, multilingual)
            .setStyleRuleIds(rule)
            .setTranslationMemoryIds(tm1, tm2));
    client.updateStyleProfile(ID, new UpdateStyleProfileOptions().setStyleRuleIds(rule));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CreateStyleProfileOptions().setStyleRuleIds(rule, (StyleRuleInfo) null));
  }

  @Test
  void sendsAnEmptyObjectWhenUpdatingWithNothingSet() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(equalToJson("{}"))
            .willReturn(okJson(MINIMAL_PROFILE)));

    client.updateStyleProfile(ID);
    client.updateStyleProfile(ID, null);
    client.updateStyleProfile(ID, new UpdateStyleProfileOptions());
  }

  @Test
  void sendsAnEmptyObjectWhenUpdatingAStyleProfileObjectWithoutOptions() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(equalToJson("{}"))
            .willReturn(okJson(MINIMAL_PROFILE)));
    StyleProfile profile =
        new StyleProfile(ID, "n", null, null, null, null, null, false, new Date(), new Date());

    client.updateStyleProfile(profile);

    wm.verify(
        1,
        patchRequestedFor(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(equalToJson("{}")));
  }

  @Test
  void acceptsAStyleProfileObjectInsteadOfAnId() throws Exception {
    wm.stubFor(get(urlEqualTo("/v3/style_profiles/" + ID)).willReturn(okJson(PROFILE)));
    wm.stubFor(
        patch(urlEqualTo("/v3/style_profiles/" + ID))
            .withRequestBody(equalToJson("{\"name\":\"new\"}"))
            .willReturn(okJson(PROFILE)));
    wm.stubFor(
        delete(urlEqualTo("/v3/style_profiles/" + ID)).willReturn(aResponse().withStatus(204)));

    StyleProfile profile = client.getStyleProfile(ID);
    client.getStyleProfile(profile);
    client.updateStyleProfile(profile, new UpdateStyleProfileOptions().setName("new"));
    client.deleteStyleProfile(ID);
    client.deleteStyleProfile(profile);
  }

  @Test
  void escapesTheIdInThePath() throws Exception {
    wm.stubFor(get(urlEqualTo("/v3/style_profiles/a%20b%2Fc%3F")).willReturn(okJson(PROFILE)));

    client.getStyleProfile("a b/c?");
  }

  @Test
  void rejectsAnEmptyIdBeforeSendingAnyRequest() {
    assertThrows(IllegalArgumentException.class, () -> client.getStyleProfile(""));
    assertThrows(IllegalArgumentException.class, () -> client.updateStyleProfile("", null));
    assertThrows(IllegalArgumentException.class, () -> client.deleteStyleProfile(""));
    wm.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void parsesEveryFieldOfTheSpecExample() throws Exception {
    wm.stubFor(get(urlEqualTo("/v3/style_profiles/" + ID)).willReturn(okJson(PROFILE)));

    StyleProfile profile = client.getStyleProfile(ID);

    assertEquals(ID, profile.getStyleProfileId());
    assertEquals("Legal, German", profile.getName());
    assertEquals("Contract wording for the German market.", profile.getDescription());
    assertEquals("Legal", profile.getIndustry());
    assertEquals(
        Collections.singletonList("4d0a2e1c-6b93-4f7a-8d15-2c6e9a3b7f81"),
        profile.getGlossaryIds());
    assertEquals(
        Collections.singletonList("bd0a38f3-1831-440b-a8dd-2c702e2325ab"),
        profile.getStyleRuleIds());
    assertEquals(
        Collections.singletonList("a74d88fb-ed2a-4943-a664-a4512398b994"),
        profile.getTranslationMemoryIds());
    assertTrue(profile.isGlossaryPriorityOrderEnabled());
    assertEquals(Instant.parse("2026-04-01T16:34:25.223Z"), profile.getCreationTime().toInstant());
    assertEquals(Instant.parse("2026-08-06T09:12:44.108Z"), profile.getUpdatedTime().toInstant());
  }

  @Test
  void parsesAProfileWithoutOptionalFieldsAndIgnoresUnknownFields() throws Exception {
    wm.stubFor(get(urlEqualTo("/v3/style_profiles/" + ID)).willReturn(okJson(MINIMAL_PROFILE)));

    StyleProfile profile = client.getStyleProfile(ID);

    assertNull(profile.getDescription());
    assertNull(profile.getIndustry());
    assertTrue(profile.getGlossaryIds().isEmpty());
    assertFalse(profile.isGlossaryPriorityOrderEnabled());
    assertEquals(Instant.parse("2026-10-26T16:27:10.298Z"), profile.getCreationTime().toInstant());
  }

  @Test
  void toleratesNonStringIds() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/style_profiles/" + ID))
            .willReturn(
                okJson(
                    "{\"style_profile_id\":\""
                        + ID
                        + "\",\"name\":\"n\","
                        + "\"creation_time\":\"2026-04-01T16:34:25.223Z\","
                        + "\"updated_time\":\"2026-08-06T09:12:44.108Z\","
                        + "\"glossary_ids\":[\"g1\",42,null,{}],\"style_rule_ids\":null}")));

    StyleProfile profile = client.getStyleProfile(ID);

    assertEquals(ID, profile.getStyleProfileId());
    assertEquals("n", profile.getName());
    assertEquals(Collections.singletonList("g1"), profile.getGlossaryIds());
    assertTrue(profile.getStyleRuleIds().isEmpty());
    assertTrue(profile.getTranslationMemoryIds().isEmpty());
    assertFalse(profile.isGlossaryPriorityOrderEnabled());
  }

  @Test
  void throwsDeepLExceptionWhenATimestampIsMissingOrMalformed() {
    String head = "{\"style_profile_id\":\"" + ID + "\",\"name\":\"n\",";
    wm.stubFor(
        get(urlEqualTo("/v3/style_profiles/missing"))
            .willReturn(okJson(head + "\"updated_time\":\"2026-08-06T09:12:44.108Z\"}")));
    wm.stubFor(
        get(urlEqualTo("/v3/style_profiles/malformed"))
            .willReturn(
                okJson(
                    head
                        + "\"creation_time\":\"2026-04-01T16:34:25.223Z\","
                        + "\"updated_time\":\"not a timestamp\"}")));

    DeepLException missing =
        assertThrows(DeepLException.class, () -> client.getStyleProfile("missing"));
    assertTrue(missing.getMessage().contains("creation_time"));
    DeepLException malformed =
        assertThrows(DeepLException.class, () -> client.getStyleProfile("malformed"));
    assertTrue(malformed.getMessage().contains("updated_time"));
  }

  @Test
  void throwsNotFoundExceptionOn404() {
    wm.stubFor(
        get(urlEqualTo("/v3/style_profiles/" + ID))
            .willReturn(
                aResponse().withStatus(404).withBody("{\"message\":\"Style profile not found\"}")));

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> client.getStyleProfile(ID));
    assertTrue(exception.getMessage().contains("Style profile not found"));
  }
}
