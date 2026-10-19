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
}
