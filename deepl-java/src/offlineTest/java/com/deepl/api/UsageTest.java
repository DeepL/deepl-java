// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class UsageTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private final DeepLClient client = OfflineClient.createClient(wm);

  @Test
  void sendsAGetRequestWithoutBody() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .withHeader("Content-Type", absent())
            .willReturn(okJson("{\"character_count\":1,\"character_limit\":2}")));
    client.getUsage();
  }

  @Test
  void sendsApiVersionOneInThePath() throws Exception {
    DeepLClient v1 =
        OfflineClient.createClient(
            wm, new DeepLClientOptions().setApiVersion(DeepLApiVersion.VERSION_1));
    wm.stubFor(
        get("/v1/usage").willReturn(okJson("{\"character_count\":1,\"character_limit\":2}")));
    v1.getUsage();
  }

  @Test
  void parsesCharacterAndDocumentUsage() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .willReturn(
                okJson(
                    "{\"character_count\":180118,\"character_limit\":1250000,"
                        + "\"document_count\":3,\"document_limit\":10}")));
    Usage usage = client.getUsage();
    assertEquals(180118, usage.getCharacter().getCount());
    assertEquals(1250000, usage.getCharacter().getLimit());
    assertEquals(3, usage.getDocument().getCount());
    assertEquals(10, usage.getDocument().getLimit());
    assertNull(usage.getTeamDocument());
    assertFalse(usage.anyLimitReached());
    assertTrue(usage.toString().contains("Characters: 180118 of 1250000"));
    assertTrue(usage.toString().contains("Documents: 3 of 10"));
  }

  @Test
  void parsesTeamDocumentUsageWithoutOtherDetails() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .willReturn(okJson("{\"team_document_count\":1,\"team_document_limit\":1}")));
    Usage usage = client.getUsage();
    assertNull(usage.getCharacter());
    assertNull(usage.getDocument());
    assertNotNull(usage.getTeamDocument());
    assertTrue(usage.getTeamDocument().limitReached());
    assertTrue(usage.anyLimitReached());
    assertTrue(usage.toString().contains("Team documents: 1 of 1"));
  }

  @Test
  void parsesAnExplicitNullLimitAsAbsent() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .willReturn(
                okJson(
                    "{\"character_count\":1,\"character_limit\":null,"
                        + "\"document_count\":2,\"document_limit\":3}")));
    Usage usage = client.getUsage();
    assertNull(usage.getCharacter());
    assertEquals(2, usage.getDocument().getCount());
  }

  @Test
  void parsesTheApiProFields() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .willReturn(
                okJson(
                    "{\"character_count\":180118,\"character_limit\":1250000,"
                        + "\"products\":[{\"product_type\":\"write\","
                        + "\"billing_unit\":\"characters\",\"api_key_unit_count\":0,"
                        + "\"account_unit_count\":5643,\"api_key_character_count\":0,"
                        + "\"character_count\":5643},"
                        + "{\"product_type\":\"speechToText\",\"billing_unit\":\"minutes\","
                        + "\"api_key_unit_count\":3,\"account_unit_count\":30}],"
                        + "\"api_key_character_count\":636,"
                        + "\"api_key_character_limit\":1000000000000,"
                        + "\"speech_to_text_milliseconds_count\":0,"
                        + "\"speech_to_text_milliseconds_limit\":0,"
                        + "\"speech_to_text_minutes_count\":30,"
                        + "\"speech_to_text_minutes_limit\":600,"
                        + "\"speech_to_speech_minutes_count\":12,"
                        + "\"speech_to_speech_minutes_limit\":600,"
                        + "\"start_time\":\"2025-05-13T09:18:42Z\","
                        + "\"end_time\":\"2025-06-13T09:18:42Z\"}")));
    Usage usage = client.getUsage();
    assertEquals(180118, usage.getCharacter().getCount());
    assertEquals(1250000, usage.getCharacter().getLimit());
    assertEquals(636, usage.getApiKeyCharacter().getCount());
    assertEquals(1000000000000L, usage.getApiKeyCharacter().getLimit());
    assertEquals(30, usage.getSpeechToTextMinutes().getCount());
    assertEquals(600, usage.getSpeechToTextMinutes().getLimit());
    assertEquals(12, usage.getSpeechToSpeechMinutes().getCount());
    assertEquals(600, usage.getSpeechToSpeechMinutes().getLimit());
    assertEquals(Instant.parse("2025-05-13T09:18:42Z"), usage.getStartTime().toInstant());
    assertEquals(Instant.parse("2025-06-13T09:18:42Z"), usage.getEndTime().toInstant());

    List<Usage.Product> products = usage.getProducts();
    assertEquals(2, products.size());
    assertEquals("write", products.get(0).getProductType());
    assertEquals("characters", products.get(0).getBillingUnit());
    assertEquals(0, products.get(0).getApiKeyUnitCount());
    assertEquals(5643, products.get(0).getAccountUnitCount());
    assertEquals("speechToText", products.get(1).getProductType());
    assertEquals("minutes", products.get(1).getBillingUnit());
    assertEquals(3, products.get(1).getApiKeyUnitCount());
    assertEquals(30, products.get(1).getAccountUnitCount());

    assertFalse(usage.anyLimitReached());
    assertEquals("Usage this billing period:\nCharacters: 180118 of 1250000", usage.toString());
  }

  @Test
  void leavesTheApiProFieldsNullForAFreeResponse() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .willReturn(okJson("{\"character_count\":180118,\"character_limit\":500000}")));
    Usage usage = client.getUsage();
    assertEquals(180118, usage.getCharacter().getCount());
    assertEquals(500000, usage.getCharacter().getLimit());
    assertNull(usage.getDocument());
    assertNull(usage.getTeamDocument());
    assertNull(usage.getApiKeyCharacter());
    assertNull(usage.getSpeechToTextMinutes());
    assertNull(usage.getSpeechToSpeechMinutes());
    assertNull(usage.getProducts());
    assertNull(usage.getStartTime());
    assertNull(usage.getEndTime());
    assertEquals("Usage this billing period:\nCharacters: 180118 of 500000", usage.toString());
  }

  @Test
  void keepsTheThreeArgumentConstructor() {
    Usage usage = new Usage(new Usage.Detail(1, 2), null, null);
    assertEquals(1, usage.getCharacter().getCount());
    assertNull(usage.getApiKeyCharacter());
    assertNull(usage.getProducts());
  }
}
