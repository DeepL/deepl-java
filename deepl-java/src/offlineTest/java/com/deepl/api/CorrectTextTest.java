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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CorrectTextTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String REPLY =
      "{\"improvements\":[{\"text\":\"x\",\"detected_source_language\":\"EN\","
          + "\"target_language\":\"EN-GB\"}]}";

  private final DeepLClient client = OfflineClient.createClient(wm);

  private void expectBody(StringValuePattern body) {
    wm.stubFor(
        post("/v2/write/correct")
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(body)
            .willReturn(okJson(REPLY)));
  }

  @Test
  void sendsTextAndStandardizedTargetLanguage() throws Exception {
    expectBody(FormBody.of("target_lang", "en-GB", "text", "a b"));
    client.correctText("a b", "EN-gb", null);
  }

  @Test
  void sendsNoTargetLanguageWhenItIsNull() throws Exception {
    expectBody(FormBody.of("text", "Hello"));
    client.correctText("Hello", null, null);
  }

  @Test
  void sendsExtraBodyParametersLastReplacingSameKeyFields() throws Exception {
    expectBody(FormBody.of("text", "Hi", "target_lang", "de"));
    client.correctText(
        "Hi",
        "en-GB",
        new CorrectTextOptions()
            .setExtraBodyParameters(Collections.singletonMap("target_lang", "de")));
  }

  @Test
  void throwsDeepLExceptionWhenImprovementsAreShort() {
    wm.stubFor(post("/v2/write/correct").willReturn(okJson(REPLY)));
    assertThrows(
        DeepLException.class, () -> client.correctText(Arrays.asList("a", "b"), "en-GB", null));
  }

  @Test
  void throwsDeepLExceptionWhenImprovementsAreEmpty() {
    wm.stubFor(post("/v2/write/correct").willReturn(okJson("{\"improvements\":[]}")));
    assertThrows(DeepLException.class, () -> client.correctText("a", "en-GB", null));
  }

  @Test
  void sendsEveryTextOfAList() throws Exception {
    wm.stubFor(
        post("/v2/write/correct")
            .withRequestBody(FormBody.of("target_lang", "en-GB", "text", "a", "text", "b"))
            .willReturn(
                okJson(
                    "{\"improvements\":[{\"text\":\"x\",\"detected_source_language\":\"EN\","
                        + "\"target_language\":\"EN-GB\"},{\"text\":\"y\","
                        + "\"detected_source_language\":\"DE\",\"target_language\":\"EN-GB\"}]}")));
    List<WriteResult> results = client.correctText(Arrays.asList("a", "b"), "en-GB", null);
    assertEquals(2, results.size());
  }

  @Test
  void parsesTheDetectedSourceLanguageInStandardForm() throws Exception {
    wm.stubFor(post("/v2/write/correct").willReturn(okJson(REPLY)));
    WriteResult result = client.correctText("Hello", "en-GB", null);
    assertEquals("en", result.getDetectedSourceLanguage());
  }

  @Test
  void throwsIllegalArgumentWhenTextIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> client.correctText("", null, null));
  }
}
