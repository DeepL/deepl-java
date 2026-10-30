// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TranslateTextTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String REPLY =
      "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"x\","
          + "\"billed_characters\":5,\"model_type_used\":\"quality_optimized\"}]}";
  private static final String REPLY_TWO =
      "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"x\","
          + "\"billed_characters\":5},{\"detected_source_language\":\"FR\",\"text\":\"y\","
          + "\"billed_characters\":3}]}";

  private final DeepLClient client = OfflineClient.createClient(wm);

  private void expectBody(StringValuePattern body) {
    expectBody("/v2/translate", body);
  }

  private void expectBody(String path, StringValuePattern body) {
    wm.stubFor(
        post(path)
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(body)
            .willReturn(okJson(REPLY)));
  }

  @Test
  void sendsTextAndTargetLanguage() throws Exception {
    expectBody(FormBody.of("target_lang", "de", "text", "Hello", "show_billed_characters", "1"));
    client.translateText("Hello", null, "de");
  }

  @Test
  void sendsEveryTextOfAList() throws Exception {
    wm.stubFor(
        post("/v2/translate")
            .withRequestBody(
                FormBody.of(
                    "target_lang",
                    "de",
                    "text",
                    "a b",
                    "text",
                    "c&d=e",
                    "show_billed_characters",
                    "1"))
            .willReturn(okJson(REPLY_TWO)));
    List<TextResult> results = client.translateText(Arrays.asList("a b", "c&d=e"), null, "de");
    assertEquals(2, results.size());
  }

  @Test
  void sendsLanguageCodesWithOnlyTheBaseSubtagLowerCased() throws Exception {
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "target_lang",
            "zh-Hans",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText("Hello", "EN", "ZH-Hans");
  }

  @ParameterizedTest
  @ValueSource(strings = {"en", "pt"})
  void sendsBareEnglishAndPortugueseTargetLanguages(String target) throws Exception {
    expectBody(FormBody.of("target_lang", target, "text", "Hallo", "show_billed_characters", "1"));
    client.translateText("Hallo", null, target);
  }

  @Test
  void sendsTheCodesOfLanguageObjects() throws Exception {
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText(
        "Hello", new Language("English", "en", null), new Language("German", "de", true));
  }

  @Test
  void sendsApiVersionOneInThePath() throws Exception {
    DeepLClient v1 =
        OfflineClient.createClient(
            wm, new DeepLClientOptions().setApiVersion(DeepLApiVersion.VERSION_1));
    expectBody(
        "/v1/translate",
        FormBody.of("target_lang", "de", "text", "Hello", "show_billed_characters", "1"));
    v1.translateText("Hello", null, "de");
  }

  @ParameterizedTest
  @CsvSource({
    "Default,default",
    "More,more",
    "Less,less",
    "PreferMore,prefer_more",
    "PreferLess,prefer_less"
  })
  void sendsFormality(Formality formality, String expected) throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "formality",
            expected,
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setFormality(formality));
  }

  @ParameterizedTest
  @CsvSource({"Off,0", "All,1", "NoNewlines,nonewlines"})
  void sendsSentenceSplitting(SentenceSplittingMode mode, String expected) throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "split_sentences",
            expected));
    client.translateText(
        "Hello", null, "de", new TextTranslationOptions().setSentenceSplittingMode(mode));
  }

  @Test
  void sendsPreserveFormatting() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "preserve_formatting",
            "1"));
    client.translateText(
        "Hello", null, "de", new TextTranslationOptions().setPreserveFormatting(true));
  }

  @Test
  void sendsContextAndModelType() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Das ist scharf!",
            "show_billed_characters",
            "1",
            "context",
            "Das ist ein Messer.",
            "model_type",
            "latency_optimized"));
    client.translateText(
        "Das ist scharf!",
        null,
        "de",
        new TextTranslationOptions()
            .setContext("Das ist ein Messer.")
            .setModelType("latency_optimized"));
  }

  @Test
  void sendsTagHandlingOptions() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "<p>Hi</p>",
            "show_billed_characters",
            "1",
            "tag_handling",
            "xml",
            "tag_handling_version",
            "v2",
            "outline_detection",
            "0",
            "splitting_tags",
            "title,par",
            "non_splitting_tags",
            "span",
            "ignore_tags",
            "raw,x"));
    client.translateText(
        "<p>Hi</p>",
        null,
        "de",
        new TextTranslationOptions()
            .setTagHandling("xml")
            .setTagHandlingVersion("v2")
            .setOutlineDetection(false)
            .setSplittingTags(Arrays.asList("title", "par"))
            .setNonSplittingTags(Collections.singletonList("span"))
            .setIgnoreTags(Arrays.asList("raw", "x")));
  }

  @Test
  void sendsNoOutlineDetectionByDefault() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "<p>Hi</p>",
            "show_billed_characters",
            "1",
            "tag_handling",
            "html"));
    client.translateText(
        "<p>Hi</p>", null, "de", new TextTranslationOptions().setTagHandling("html"));
  }

  @Test
  void sendsStyleId() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang", "de", "text", "Hello", "show_billed_characters", "1", "style_id", "s1"));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setStyleId("s1"));
  }

  @Test
  void sendsTheIdOfAStyleRule() throws Exception {
    StyleRuleInfo rule = new StyleRuleInfo("s2", "n", new Date(), new Date(), "de", 1, null, null);
    expectBody(
        FormBody.of(
            "target_lang", "de", "text", "Hello", "show_billed_characters", "1", "style_id", "s2"));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setStyleRule(rule));
  }

  @Test
  void sendsStyleProfileIdWithoutSourceLanguage() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "style_profile_id",
            "sp1"));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setStyleProfile("sp1"));
  }

  @Test
  void sendsTheIdOfAStyleProfile() throws Exception {
    StyleProfile profile =
        new StyleProfile("sp2", "n", null, null, null, null, null, false, null, null);
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "style_id",
            "s1",
            "style_profile_id",
            "sp2"));
    client.translateText(
        "Hello",
        "en",
        "de",
        new TextTranslationOptions().setStyleId("s1").setStyleProfile(profile));
  }

  @Test
  void omitsStyleProfileIdWhenUnset() throws Exception {
    TextTranslationOptions options = new TextTranslationOptions().setStyleId("s1");
    expectBody(
        FormBody.of(
            "target_lang", "de", "text", "Hello", "show_billed_characters", "1", "style_id", "s1"));
    client.translateText("Hello", null, "de", options);
    assertNull(options.getStyleProfileId());
  }

  @Test
  void rejectsANullStyleProfile() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TextTranslationOptions().setStyleProfile((StyleProfile) null));
  }

  @Test
  void sendsTranslationMemoryAndThreshold() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "translation_memory_id",
            "tm1",
            "translation_memory_threshold",
            "80"));
    client.translateText(
        "Hello",
        null,
        "de",
        new TextTranslationOptions()
            .setTranslationMemoryId("tm1")
            .setTranslationMemoryThreshold(80));
  }

  @Test
  void sendsTheIdOfATranslationMemory() throws Exception {
    TranslationMemoryInfo memory =
        new TranslationMemoryInfo("tm2", "n", "en", Collections.singletonList("de"), 1);
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "translation_memory_id",
            "tm2"));
    client.translateText(
        "Hello", null, "de", new TextTranslationOptions().setTranslationMemory(memory));
  }

  @Test
  void sendsEachCustomInstruction() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "custom_instructions",
            "Be brief",
            "custom_instructions",
            "Use ALL CAPS"));
    client.translateText(
        "Hello",
        null,
        "de",
        new TextTranslationOptions()
            .setCustomInstructions(Arrays.asList("Be brief", "Use ALL CAPS")));
  }

  @Test
  void sendsGlossaryId() throws Exception {
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "target_lang",
            "de",
            "glossary_id",
            "g1",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText("Hello", "en", "de", new TextTranslationOptions().setGlossary("g1"));
  }

  @Test
  void sendsTheIdOfAGlossary() throws Exception {
    GlossaryInfo glossary = new GlossaryInfo("g2", "n", true, "en", "de", new Date(), 1);
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "target_lang",
            "de",
            "glossary_id",
            "g2",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText("Hello", "en", "de", new TextTranslationOptions().setGlossary(glossary));
  }

  @Test
  void sendsGlossaryIdsJoinedByComma() throws Exception {
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "target_lang",
            "de",
            "glossary_ids",
            "g1,g2",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText(
        "Hello", "en", "de", new TextTranslationOptions().setGlossaryIds("g1", "g2"));
  }

  @Test
  void sendsFormalityStringAsGiven() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "formality",
            "prefer_less",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText(
        "Hello", null, "de", new TextTranslationOptions().setFormality("prefer_less"));
  }

  @Test
  void sendsSentenceSplittingStringAsGiven() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "split_sentences",
            "nonewlines"));
    client.translateText(
        "Hello", null, "de", new TextTranslationOptions().setSentenceSplittingMode("nonewlines"));
  }

  @Test
  void sendsTagHandlingEnums() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "tag_handling",
            "html",
            "tag_handling_version",
            "v1"));
    client.translateText(
        "Hello",
        null,
        "de",
        new TextTranslationOptions()
            .setTagHandling(TagHandlingMode.Html)
            .setTagHandlingVersion(TagHandlingVersion.V1));
  }

  @ParameterizedTest
  @CsvSource({
    "QualityOptimized,quality_optimized",
    "PreferQualityOptimized,prefer_quality_optimized",
    "LatencyOptimized,latency_optimized"
  })
  void sendsModelTypeEnum(ModelType modelType, String expected) throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "model_type",
            expected));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setModelType(modelType));
  }

  @Test
  void sendsCommaSeparatedTagStringsAsGiven() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "splitting_tags",
            "title,par",
            "non_splitting_tags",
            "span,b",
            "ignore_tags",
            "raw"));
    client.translateText(
        "Hello",
        null,
        "de",
        new TextTranslationOptions()
            .setSplittingTags("title,par")
            .setNonSplittingTags("span,b")
            .setIgnoreTags("raw"));
  }

  @Test
  void sendsStyleRuleId() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang", "de", "text", "Hello", "show_billed_characters", "1", "style_id", "s3"));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setStyleRule("s3"));
  }

  @Test
  void sendsTranslationMemoryId() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "translation_memory_id",
            "tm3"));
    client.translateText(
        "Hello", null, "de", new TextTranslationOptions().setTranslationMemory("tm3"));
  }

  @Test
  void sendsTheIdsOfGlossaryObjects() throws Exception {
    GlossaryInfo glossary = new GlossaryInfo("g1", "n", true, "en", "de", new Date(), 1);
    MultilingualGlossaryInfo multilingual =
        new MultilingualGlossaryInfo("g2", "n", new Date(), Collections.emptyList());
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "target_lang",
            "de",
            "glossary_ids",
            "g1,g2",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText(
        "Hello", "en", "de", new TextTranslationOptions().setGlossaryIds(glossary, multilingual));
  }

  @Test
  void resolvesTheZeroArgumentSetGlossaryIdsToTheIdOverloadAndSendsNoGlossaries() throws Exception {
    expectBody(FormBody.of("target_lang", "de", "text", "Hello", "show_billed_characters", "1"));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setGlossaryIds());
  }

  @Test
  void sendsExtraBodyParametersLastAndLetsThemOverrideOptions() throws Exception {
    Map<String, String> extra = new LinkedHashMap<>();
    extra.put("target_lang", "fr");
    extra.put("debug", "1");
    TextTranslationOptions options =
        new TextTranslationOptions().setExtraBodyParameters(extra).setFormality(Formality.Less);
    expectBody(
        FormBody.of(
            "source_lang",
            "en",
            "formality",
            "less",
            "text",
            "Hello",
            "show_billed_characters",
            "1",
            "target_lang",
            "fr",
            "debug",
            "1"));
    client.translateText("Hello", "en", "de", options);
  }

  @Test
  void parsesDetectedLanguageBilledCharactersAndModelType() throws Exception {
    wm.stubFor(post("/v2/translate").willReturn(okJson(REPLY)));
    TextResult result = client.translateText("Hello", null, "de");
    assertEquals("en", result.getDetectedSourceLanguage());
    assertEquals(5, result.getBilledCharacters());
    assertEquals("quality_optimized", result.getModelTypeUsed());
  }

  @Test
  void parsesAMissingModelTypeAsNull() throws Exception {
    wm.stubFor(post("/v2/translate").willReturn(okJson(REPLY_TWO)));
    List<TextResult> results = client.translateText(Arrays.asList("a", "b"), null, "de");
    assertEquals(null, results.get(0).getModelTypeUsed());
    assertEquals("fr", results.get(1).getDetectedSourceLanguage());
  }

  @Test
  void parsesAMissingBilledCharactersAndANullModelType() throws Exception {
    wm.stubFor(
        post("/v2/translate")
            .willReturn(
                okJson(
                    "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"x\","
                        + "\"model_type_used\":null}]}")));
    TextResult result = client.translateText("Hello", null, "de");
    assertEquals(0, result.getBilledCharacters());
    assertNull(result.getModelTypeUsed());
    assertEquals("x", result.getText());
  }

  @Test
  void parsesTagHandlingVersion() throws Exception {
    wm.stubFor(
        post("/v2/translate")
            .willReturn(
                okJson(
                    "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"x\","
                        + "\"billed_characters\":5,\"tag_handling_version\":\"v2\"}]}")));
    TextResult result = client.translateText("Hello", null, "de");
    assertEquals("v2", result.getTagHandlingVersion());
  }

  @Test
  void parsesAMissingOrMalformedTagHandlingVersionAsNull() throws Exception {
    wm.stubFor(
        post("/v2/translate")
            .willReturn(
                okJson(
                    "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"x\","
                        + "\"billed_characters\":5,\"tag_handling_version\":{\"v\":2}},"
                        + "{\"detected_source_language\":\"EN\",\"text\":\"y\","
                        + "\"billed_characters\":3}]}")));
    List<TextResult> results = client.translateText(Arrays.asList("a", "b"), null, "de");
    assertNull(results.get(0).getTagHandlingVersion());
    assertEquals("x", results.get(0).getText());
    assertEquals(5, results.get(0).getBilledCharacters());
    assertNull(results.get(1).getTagHandlingVersion());
  }

  @Test
  void throwsIllegalArgumentWhenTextIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> client.translateText("", null, "de"));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.translateText(Arrays.asList("a", ""), null, "de"));
  }

  @Test
  void throwsIllegalArgumentWhenLanguagesAreEmpty() {
    assertThrows(IllegalArgumentException.class, () -> client.translateText("Hello", null, ""));
    assertThrows(IllegalArgumentException.class, () -> client.translateText("Hello", "", "de"));
  }

  @Test
  void throwsIllegalArgumentWhenThresholdHasNoTranslationMemory() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                client.translateText(
                    "Hello",
                    null,
                    "de",
                    new TextTranslationOptions().setTranslationMemoryThreshold(80)));
    assertTrue(thrown.getMessage().contains("requires translationMemoryId"));
  }

  @Test
  void sendsGlossaryWithoutSourceLanguage() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "glossary_id",
            "g1",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText("Hello", null, "de", new TextTranslationOptions().setGlossary("g1"));
  }

  @Test
  void sendsGlossaryIdsWithoutSourceLanguage() throws Exception {
    expectBody(
        FormBody.of(
            "target_lang",
            "de",
            "glossary_ids",
            "g1,g2",
            "text",
            "Hello",
            "show_billed_characters",
            "1"));
    client.translateText(
        "Hello", null, "de", new TextTranslationOptions().setGlossaryIds("g1", "g2"));
  }

  @Test
  void throwsIllegalArgumentWhenGlossaryIdAndGlossaryIdsAreBothSet() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                client.translateText(
                    "Hello",
                    "en",
                    "de",
                    new TextTranslationOptions().setGlossaryId("g1").setGlossaryIds("g2")));
    assertTrue(thrown.getMessage().contains("cannot be used together with glossaryId"));
  }

  @Test
  void throwsIllegalArgumentWhenMoreThanFiveGlossaryIdsAreSet() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                client.translateText(
                    "Hello",
                    "en",
                    "de",
                    new TextTranslationOptions().setGlossaryIds("1", "2", "3", "4", "5", "6")));
    assertTrue(thrown.getMessage().contains("more than 5"));
  }
}
