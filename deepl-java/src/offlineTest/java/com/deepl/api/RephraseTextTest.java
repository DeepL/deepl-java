// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RephraseTextTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String REPLY =
      "{\"improvements\":[{\"text\":\"x\",\"detected_source_language\":\"EN\","
          + "\"target_language\":\"EN-GB\"}]}";

  private final DeepLClient client = OfflineClient.createClient(wm);

  private void expectBody(StringValuePattern body) {
    wm.stubFor(
        post("/v2/write/rephrase")
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(body)
            .willReturn(okJson(REPLY)));
  }

  @Test
  void sendsTextAndTargetLanguageWithOnlyTheBaseSubtagLowerCased() throws Exception {
    expectBody(JsonBody.of("target_lang", "en-gb", "text", JsonBody.list("a b")));
    client.rephraseText("a b", "EN-gb", null);
  }

  @Test
  void sendsTheScriptSubtagOfTheTargetLanguageAsGiven() throws Exception {
    expectBody(JsonBody.of("target_lang", "zh-Hans", "text", JsonBody.list("Hello")));
    client.rephraseText("Hello", "ZH-Hans", null);
  }

  @Test
  void sendsNoTargetLanguageWhenItIsNull() throws Exception {
    expectBody(JsonBody.of("text", JsonBody.list("Hello")));
    client.rephraseText("Hello", null, null);
  }

  @Test
  void sendsBareEnglishTargetLanguage() throws Exception {
    expectBody(JsonBody.of("target_lang", "en", "text", JsonBody.list("Hello")));
    client.rephraseText("Hello", "en", null);
  }

  @Test
  void sendsWritingStyleAndTone() throws Exception {
    expectBody(
        JsonBody.of(
            "target_lang",
            "en-GB",
            "writing_style",
            "business",
            "tone",
            "friendly",
            "text",
            JsonBody.list("Hello")));
    client.rephraseText(
        "Hello",
        "en-GB",
        new TextRephraseOptions()
            .setWritingStyle(WritingStyle.Business.getValue())
            .setTone(WritingTone.Friendly.getValue()));
  }

  @Test
  void sendsWritingStyleAndToneEnumValues() throws Exception {
    expectBody(
        JsonBody.of(
            "writing_style", "prefer_simple", "tone", "diplomatic", "text", JsonBody.list("Hi")));
    client.rephraseText(
        "Hi",
        null,
        new TextRephraseOptions()
            .setWritingStyle(WritingStyle.PreferSimple)
            .setTone(WritingTone.Diplomatic));
  }

  @Test
  void sendsExtraBodyParametersLastReplacingSameKeyFields() throws Exception {
    expectBody(
        JsonBody.of(
            "target_lang",
            "en-GB",
            "text",
            JsonBody.list("Hi"),
            "tone",
            "friendly",
            "beta_flag",
            "x y"));
    Map<String, String> extra = new LinkedHashMap<>();
    extra.put("tone", "friendly");
    extra.put("beta_flag", "x y");
    client.rephraseText(
        "Hi",
        "en-GB",
        new TextRephraseOptions().setTone(WritingTone.Confident).setExtraBodyParameters(extra));
  }

  @Test
  void throwsDeepLExceptionWhenImprovementsAreShort() {
    wm.stubFor(post("/v2/write/rephrase").willReturn(okJson(REPLY)));
    assertThrows(
        DeepLException.class, () -> client.rephraseText(Arrays.asList("a", "b"), "en-GB", null));
  }

  @Test
  void throwsDeepLExceptionWhenImprovementsAreEmpty() {
    wm.stubFor(post("/v2/write/rephrase").willReturn(okJson("{\"improvements\":[]}")));
    assertThrows(DeepLException.class, () -> client.rephraseText("a", "en-GB", null));
  }

  @Test
  void throwsDeepLExceptionWhenAnImprovementIsNull() {
    wm.stubFor(post("/v2/write/rephrase").willReturn(okJson("{\"improvements\":[null]}")));
    assertThrows(DeepLException.class, () -> client.rephraseText("a", "en-GB", null));
  }

  @Test
  void sendsEveryTextOfAList() throws Exception {
    wm.stubFor(
        post("/v2/write/rephrase")
            .withRequestBody(JsonBody.of("target_lang", "en-GB", "text", JsonBody.list("a", "b")))
            .willReturn(
                okJson(
                    "{\"improvements\":[{\"text\":\"x\",\"detected_source_language\":\"EN\","
                        + "\"target_language\":\"EN-GB\"},{\"text\":\"y\","
                        + "\"detected_source_language\":\"DE\",\"target_language\":\"EN-GB\"}]}")));
    List<WriteResult> results = client.rephraseText(Arrays.asList("a", "b"), "en-GB", null);
    assertEquals(2, results.size());
  }

  @Test
  void parsesTheDetectedSourceLanguageInStandardForm() throws Exception {
    wm.stubFor(post("/v2/write/rephrase").willReturn(okJson(REPLY)));
    WriteResult result = client.rephraseText("Hello", "en-GB", null);
    assertEquals("en", result.getDetectedSourceLanguage());
    assertEquals("EN-GB", result.getTargetLanguage());
  }

  @Test
  void throwsIllegalArgumentWhenTextIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> client.rephraseText("", "en-GB", null));
  }
}
