// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SpokenTermsTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private final DeepLClient client = OfflineClient.createClient(wm);

  @Test
  void joinsEntriesWithNewlinesWhenSendingAndSplitsThemWhenReading() throws Exception {
    wm.stubFor(
        put(urlEqualTo("/v3/spoken-terms/st1/term-lists"))
            .withRequestBody(equalToJson("{\"lang\":\"en\",\"entries\":\"a\\nb\"}"))
            .willReturn(okJson("{\"lang\":\"en\",\"entry_count\":2}")));
    wm.stubFor(
        get(urlEqualTo("/v3/spoken-terms/st1/entries?lang=en"))
            .willReturn(okJson("{\"lang\":\"en\",\"entries\":\"a\\nb\\n\"}")));

    client.replaceSpokenTermsList("st1", new SpokenTermsEntries("en", Arrays.asList("a", "b")));
    SpokenTermsEntries entries = client.getSpokenTermsEntries("st1", "en");

    assertEquals("en", entries.getLang());
    assertEquals(Arrays.asList("a", "b"), entries.getEntries());
  }
}
