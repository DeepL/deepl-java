// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LanguagesTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String LANGUAGES =
      "[{\"lang\":\"de\",\"name\":\"German\",\"usable_as_source\":true,\"usable_as_target\":true,"
          + "\"features\":{\"formality\":{\"status\":\"stable\"}}},"
          + "{\"lang\":\"en-US\",\"name\":\"English (American)\",\"usable_as_source\":false,"
          + "\"usable_as_target\":true,\"features\":{}},"
          + "{\"lang\":\"en\",\"name\":\"English\",\"usable_as_source\":true,"
          + "\"usable_as_target\":false},"
          + "{\"lang\":\"ja\",\"name\":\"Japanese\",\"usable_as_source\":true,"
          + "\"usable_as_target\":true}]";

  private final DeepLClient client = OfflineClient.createClient(wm);

  private void expectLanguages(String query) {
    wm.stubFor(
        get(urlEqualTo("/v3/languages?" + query))
            .withHeader("Content-Type", absent())
            .willReturn(okJson(LANGUAGES)));
  }

  @Test
  void sendsTheTranslateTextResourceForSourceLanguages() throws Exception {
    expectLanguages("resource=translate_text");
    client.getSourceLanguages();
  }

  @Test
  void sendsTheTranslateTextResourceForTargetLanguages() throws Exception {
    expectLanguages("resource=translate_text");
    client.getTargetLanguages();
  }

  @Test
  void sendsTheGlossaryResourceForGlossaryLanguages() throws Exception {
    expectLanguages("resource=glossary");
    client.getGlossaryLanguages();
  }

  @Test
  void parsesOnlyLanguagesUsableAsSourceWithoutFormalitySupport() throws Exception {
    expectLanguages("resource=translate_text");
    List<Language> languages = client.getSourceLanguages();
    assertEquals(Arrays.asList("de", "en", "ja"), codes(languages));
    for (Language language : languages) {
      assertNull(language.getSupportsFormality());
    }
  }

  @Test
  void parsesOnlyLanguagesUsableAsTargetWithFormalitySupport() throws Exception {
    expectLanguages("resource=translate_text");
    List<Language> languages = client.getTargetLanguages();
    assertEquals(Arrays.asList("de", "en-US", "ja"), codes(languages));
    assertTrue(languages.get(0).getSupportsFormality());
    assertFalse(languages.get(1).getSupportsFormality());
    assertFalse(languages.get(2).getSupportsFormality());
  }

  @Test
  void parsesGlossaryLanguagePairsWithoutPairingALanguageWithItself() throws Exception {
    expectLanguages("resource=glossary");
    List<GlossaryLanguagePair> pairs = client.getGlossaryLanguages();
    // Sources: de, en, ja. Targets: de, en-US, ja.
    assertEquals(7, pairs.size());
    assertTrue(pairs.stream().noneMatch(p -> p.getSourceLanguage().equals(p.getTargetLanguage())));
    assertTrue(
        pairs.stream()
            .anyMatch(
                p -> p.getSourceLanguage().equals("en") && p.getTargetLanguage().equals("en-US")));
  }

  @Test
  void sendsTheResourceAndEveryIncludeValueOfLanguagesForAResource() throws Exception {
    expectLanguages("resource=write&include=beta&include=external");
    client.getLanguagesForResource(
        "write", Arrays.asList(LanguageSupport.INCLUDE_BETA, LanguageSupport.INCLUDE_EXTERNAL));
  }

  @Test
  void sendsOnlyTheResourceWhenNoIncludeIsGiven() throws Exception {
    expectLanguages("resource=glossary");
    client.getLanguagesForResource("glossary");
  }

  @Test
  void encodesTheResourceInTheQuery() throws Exception {
    wm.stubFor(get(urlEqualTo("/v3/languages?resource=a%20b%26c")).willReturn(okJson("[]")));
    client.getLanguagesForResource("a b&c");
  }

  @Test
  void parsesLanguageSupportWithItsFeatures() throws Exception {
    expectLanguages("resource=translate_text");
    List<LanguageSupport> languages = client.getLanguagesForResource("translate_text");
    LanguageSupport german = languages.get(0);
    assertEquals("de", german.getCode());
    assertTrue(german.isUsableAsSource());
    assertTrue(german.isUsableAsTarget());
    assertTrue(german.supports(LanguageFeature.FEATURE_FORMALITY));
    assertFalse(languages.get(1).supports(LanguageFeature.FEATURE_FORMALITY));
    assertTrue(languages.get(2).getFeatures().isEmpty());
  }

  @Test
  void throwsIllegalArgumentWhenTheResourceIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> client.getLanguagesForResource(""));
  }

  @Test
  void sendsAGetRequestForLanguageResources() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/languages/resources"))
            .withHeader("Content-Type", absent())
            .willReturn(
                okJson(
                    "[{\"name\":\"translate_text\",\"features\":["
                        + "{\"name\":\"glossary\",\"needs_source_support\":true,"
                        + "\"needs_target_support\":true},{\"name\":\"formality\"}]},"
                        + "{\"name\":\"write\"}]")));
    List<LanguageResource> resources = client.getLanguageResources();
    LanguageResource translateText = resources.get(0);
    assertEquals("translate_text", translateText.getName());
    ResourceFeature glossary = translateText.getFeatures().get(LanguageFeature.FEATURE_GLOSSARY);
    assertEquals(Boolean.TRUE, glossary.getNeedsSourceSupport());
    assertEquals(Boolean.TRUE, glossary.getNeedsTargetSupport());
    ResourceFeature formality = translateText.getFeatures().get(LanguageFeature.FEATURE_FORMALITY);
    assertNull(formality.getNeedsSourceSupport());
    assertTrue(translateText.supports(LanguageFeature.FEATURE_FORMALITY));
    assertTrue(resources.get(1).getFeatures().isEmpty());
  }

  private static List<String> codes(List<Language> languages) {
    java.util.ArrayList<String> codes = new java.util.ArrayList<>();
    for (Language language : languages) {
      codes.add(language.getCode());
    }
    return codes;
  }
}
