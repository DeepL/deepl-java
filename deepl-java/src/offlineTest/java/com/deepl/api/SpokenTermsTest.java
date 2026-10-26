// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

  @Test
  void rejectsATermContainingANewlineBeforeSendingAnyRequest() {
    List<SpokenTermsEntries> termLists =
        Collections.singletonList(new SpokenTermsEntries("en", Arrays.asList("DeepL", "a\nb")));

    assertThrows(IllegalArgumentException.class, () -> client.createSpokenTerms("n", termLists));
    assertThrows(
        IllegalArgumentException.class, () -> client.updateSpokenTerms("st1", null, termLists));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.replaceSpokenTermsList("st1", termLists.get(0)));
    wm.verify(0, anyRequestedFor(anyUrl()));
  }
}
