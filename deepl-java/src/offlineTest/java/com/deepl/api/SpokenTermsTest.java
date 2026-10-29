// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SpokenTermsTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private final DeepLClient client = OfflineClient.createClient(wm);

  private static final String SPOKEN_TERMS =
      "{\"spoken_terms_id\":\"st1\",\"name\":\"n\","
          + "\"creation_time\":\"2026-10-01T00:00:00Z\",\"term_lists\":[]}";
  private static final String TERM_LIST = "{\"lang\":\"en\",\"entry_count\":1}";

  private static Map<String, String> extraBodyParameters() {
    Map<String, String> extra = new HashMap<>();
    extra.put("name", "override");
    extra.put("beta", "on");
    return extra;
  }

  @Test
  void createSendsTheExtraBodyParametersLast() throws Exception {
    wm.stubFor(
        post(urlEqualTo("/v3/spoken-terms"))
            .withRequestBody(
                equalToJson(
                    "{\"name\":\"override\",\"beta\":\"on\","
                        + "\"term_lists\":[{\"lang\":\"en\",\"entries\":\"a\"}]}"))
            .willReturn(okJson(SPOKEN_TERMS)));

    client.createSpokenTerms(
        "n",
        Collections.singletonList(new SpokenTermsEntries("en", Collections.singletonList("a"))),
        new CreateSpokenTermsOptions().setExtraBodyParameters(extraBodyParameters()));
  }

  @Test
  void updateSendsOnlyTheOptionsThatAreSet() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/spoken-terms/st1"))
            .withRequestBody(equalToJson("{\"name\":\"renamed\"}"))
            .willReturn(okJson(SPOKEN_TERMS)));
    wm.stubFor(
        patch(urlEqualTo("/v3/spoken-terms/st2"))
            .withRequestBody(
                equalToJson("{\"term_lists\":[{\"lang\":\"en\",\"entries\":\"a\\nb\"}]}"))
            .willReturn(okJson(SPOKEN_TERMS)));
    wm.stubFor(
        patch(urlEqualTo("/v3/spoken-terms/st3"))
            .withRequestBody(equalToJson("{\"name\":\"override\",\"beta\":\"on\"}"))
            .willReturn(okJson(SPOKEN_TERMS)));
    wm.stubFor(
        patch(urlEqualTo("/v3/spoken-terms/st4"))
            .withRequestBody(equalToJson("{}"))
            .willReturn(okJson(SPOKEN_TERMS)));

    client.updateSpokenTerms("st1", new UpdateSpokenTermsOptions().setName("renamed"));
    client.updateSpokenTerms(
        "st2",
        new UpdateSpokenTermsOptions()
            .setTermLists(
                Collections.singletonList(new SpokenTermsEntries("en", Arrays.asList("a", "b")))));
    client.updateSpokenTerms(
        "st3",
        new UpdateSpokenTermsOptions()
            .setName("renamed")
            .setExtraBodyParameters(extraBodyParameters()));
    client.updateSpokenTerms("st4", null);
  }

  @Test
  void replaceListSendsTheExtraBodyParametersLast() throws Exception {
    Map<String, String> extra = new HashMap<>();
    extra.put("lang", "de");
    wm.stubFor(
        put(urlEqualTo("/v3/spoken-terms/st1/term-lists"))
            .withRequestBody(equalToJson("{\"lang\":\"de\",\"entries\":\"a\"}"))
            .willReturn(okJson(TERM_LIST)));

    client.replaceSpokenTermsList(
        "st1",
        new SpokenTermsEntries("en", Collections.singletonList("a")),
        new ReplaceSpokenTermsListOptions().setExtraBodyParameters(extra));
  }

  @Test
  void urlEncodesTheIdInEveryPath() throws Exception {
    String id = "a/b c";
    String path = "/v3/spoken-terms/a%2Fb%20c";
    wm.stubFor(get(urlEqualTo(path)).willReturn(okJson(SPOKEN_TERMS)));
    wm.stubFor(patch(urlEqualTo(path)).willReturn(okJson(SPOKEN_TERMS)));
    wm.stubFor(delete(urlEqualTo(path)).willReturn(ok()));
    wm.stubFor(
        get(urlEqualTo(path + "/entries?lang=en"))
            .willReturn(okJson("{\"lang\":\"en\",\"entries\":\"a\"}")));
    wm.stubFor(put(urlEqualTo(path + "/term-lists")).willReturn(okJson(TERM_LIST)));
    wm.stubFor(delete(urlEqualTo(path + "/term-lists?lang=en")).willReturn(ok()));

    client.getSpokenTerms(id);
    client.updateSpokenTerms(id, new UpdateSpokenTermsOptions().setName("n"));
    client.getSpokenTermsEntries(id, "en");
    client.replaceSpokenTermsList(id, new SpokenTermsEntries("en", Collections.singletonList("a")));
    client.deleteSpokenTermsList(id, "en");
    client.deleteSpokenTerms(id);
  }

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
        IllegalArgumentException.class,
        () ->
            client.updateSpokenTerms(
                "st1", new UpdateSpokenTermsOptions().setTermLists(termLists)));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.replaceSpokenTermsList("st1", termLists.get(0)));
    wm.verify(0, anyRequestedFor(anyUrl()));
  }
}
