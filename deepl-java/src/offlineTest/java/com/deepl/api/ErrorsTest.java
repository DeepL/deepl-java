// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("deprecation")
class ErrorsTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private final DeepLClient translator =
      OfflineClient.createClient(wm, new DeepLClientOptions().setMaxRetries(0));

  private void respond(int status, String body) {
    wm.stubFor(get("/v2/usage").willReturn(aResponse().withStatus(status).withBody(body)));
  }

  @ParameterizedTest
  @CsvSource({
    "400,com.deepl.api.DeepLException,Bad request,false",
    "401,com.deepl.api.AuthorizationException,Authorization failure,false",
    "403,com.deepl.api.AuthorizationException,Authorization failure,false",
    "404,com.deepl.api.NotFoundException,Not found,false",
    "418,com.deepl.api.DeepLException,Unknown error,false",
    "429,com.deepl.api.TooManyRequestsException,Too many requests,true",
    "456,com.deepl.api.QuotaExceededException,Quota for this billing period has been exceeded,false",
    "500,com.deepl.api.DeepLException,Unknown error,true",
    "503,com.deepl.api.DeepLException,Service unavailable,true",
  })
  void throwsTheExceptionOfTheStatusCode(
      int status,
      Class<? extends DeepLException> expected,
      String messagePrefix,
      boolean shouldRetry) {
    respond(status, "");
    DeepLException thrown = assertThrows(expected, translator::getUsage);
    assertEquals(expected, thrown.getClass());
    assertEquals(true, thrown.getMessage().startsWith(messagePrefix), thrown.getMessage());
    assertEquals(status, thrown.getHttpStatusCode());
    assertEquals(shouldRetry, thrown.getShouldRetry());
  }

  @Test
  void throwsAuthorizationExceptionForHttp401OfTheAdminClient() {
    wm.stubFor(get("/v2/admin/developer-keys").willReturn(aResponse().withStatus(401)));
    DeepLAdminClient admin =
        OfflineClient.createAdminClient(wm, new DeepLAdminClientOptions().setMaxRetries(0));
    AuthorizationException thrown =
        assertThrows(AuthorizationException.class, admin::listDeveloperKeys);
    assertEquals(401, thrown.getHttpStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not json", "{\"character_count\":", "[]"})
  void throwsDeepLExceptionForAMalformedResponse(String body) {
    respond(200, body);
    DeepLException thrown = assertThrows(DeepLException.class, translator::getUsage);
    assertEquals(DeepLException.class, thrown.getClass());
    assertTrue(thrown.getMessage().startsWith("Failed to parse"), thrown.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{}", "{\"translations\":[{\"text\":"})
  void throwsDeepLExceptionForAMalformedTranslation(String body) {
    wm.stubFor(post("/v2/translate").willReturn(aResponse().withStatus(200).withBody(body)));
    DeepLException thrown =
        assertThrows(DeepLException.class, () -> translator.translateText("Hello", null, "de"));
    assertTrue(thrown.getMessage().startsWith("Failed to parse"), thrown.getMessage());
  }

  @Test
  void throwsWithTheMessageAndDetailOfAJsonErrorBody() {
    respond(400, "{\"message\":\"Value for 'target_lang' not supported.\",\"detail\":\"Try de\"}");
    DeepLException thrown = assertThrows(DeepLException.class, translator::getUsage);
    assertEquals(
        "Bad request, error message: message: Value for 'target_lang' not supported., detail: Try de",
        thrown.getMessage());
  }

  @Test
  void throwsWithTheMessageOfAnInfrastructureErrorBody() {
    respond(400, "{\"error\":{\"message\":\"Request blocked\"}}");
    DeepLException thrown = assertThrows(DeepLException.class, translator::getUsage);
    assertEquals("Bad request, error message: message: Request blocked", thrown.getMessage());
  }

  @Test
  void throwsWithTheRawBodyWhenTheErrorBodyIsNotJson() {
    respond(500, "<html>Bad gateway</html>");
    DeepLException thrown = assertThrows(DeepLException.class, translator::getUsage);
    assertEquals("Unknown error, response: <html>Bad gateway</html>", thrown.getMessage());
  }

  @Test
  void throwsWithoutSuffixWhenTheErrorBodyIsEmpty() {
    respond(456, "");
    QuotaExceededException thrown =
        assertThrows(QuotaExceededException.class, translator::getUsage);
    assertEquals("Quota for this billing period has been exceeded", thrown.getMessage());
  }

  @Test
  void throwsNotFoundForGlossaryRequests() {
    wm.stubFor(get("/v2/glossaries/g1").willReturn(aResponse().withStatus(404)));
    NotFoundException thrown =
        assertThrows(NotFoundException.class, () -> translator.getGlossary("g1"));
    assertEquals(NotFoundException.class, thrown.getClass());
    assertEquals("Not found", thrown.getMessage());
  }

  private static Arguments nullRow(String name, ThrowingConsumer<DeepLClient> call) {
    return Arguments.of(name, call);
  }

  static Stream<Arguments> nullIdsNamesAndObjects() {
    return Stream.of(
        nullRow("glossaryId", c -> c.getMultilingualGlossary((String) null)),
        nullRow("name", c -> c.updateMultilingualGlossaryName("g1", null)),
        nullRow(
            "sourceLanguageCode", c -> c.deleteMultilingualGlossaryDictionary("g1", null, "de")),
        nullRow("lang", c -> c.getSpokenTermsEntries("st1", null)),
        nullRow("resource", c -> c.getLanguagesForResource((String) null)),
        nullRow("resource", c -> c.getLanguagesForResource((LanguageResourceName) null)),
        nullRow("glossary", c -> c.getMultilingualGlossary((MultilingualGlossaryInfo) null)),
        nullRow("spokenTerms", c -> c.getSpokenTerms((SpokenTerms) null)),
        nullRow("styleProfile", c -> c.getStyleProfile((StyleProfile) null)),
        nullRow("styleRule", c -> c.getStyleRule((StyleRuleInfo) null)),
        nullRow(
            "instruction", c -> c.getStyleRuleCustomInstruction("s1", (CustomInstruction) null)),
        nullRow("translationMemory", c -> c.getTranslationMemory((TranslationMemoryInfo) null)),
        nullRow("job", c -> c.getTranslationMemoryJob((TranslationMemoryJob) null)),
        nullRow("glossaryId", c -> c.getGlossary(null)),
        nullRow("glossary", c -> c.deleteGlossary((GlossaryInfo) null)));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("nullIdsNamesAndObjects")
  void throwsNullPointerExceptionNamingANullIdNameOrObject(
      String name, ThrowingConsumer<DeepLClient> call) {
    assertEquals(
        name, assertThrows(NullPointerException.class, () -> call.accept(translator)).getMessage());
    assertTrue(wm.getAllServeEvents().isEmpty());
  }

  static Stream<Arguments> nullResourceContents() {
    GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("a", "b"));
    MultilingualGlossaryDictionaryEntries dict =
        new MultilingualGlossaryDictionaryEntries("en", "de", entries);
    SpokenTermsEntries terms = new SpokenTermsEntries("en", Collections.singletonList("a"));
    return Stream.of(
        nullRow("sourceLang", c -> c.createGlossary("n", null, "de", entries)),
        nullRow("entries", c -> c.createGlossary("n", "en", "de", (GlossaryEntries) null)),
        nullRow("csvContent", c -> c.createGlossaryFromCsv("n", "en", "de", (String) null)),
        nullRow("csvFile", c -> c.createGlossaryFromCsv("n", "en", "de", (File) null)),
        nullRow("glossaryDicts", c -> c.createMultilingualGlossary("n", null)),
        nullRow("glossaryDicts", c -> c.createMultilingualGlossary("n", Arrays.asList(dict, null))),
        nullRow(
            "entries",
            c -> c.replaceMultilingualGlossaryDictionary("g1", "en", "de", (GlossaryEntries) null)),
        nullRow(
            "glossaryDict",
            c ->
                c.getMultilingualGlossaryDictionaryEntries(
                    "g1", (MultilingualGlossaryDictionaryInfo) null)),
        nullRow(
            "dictionaries",
            c ->
                c.updateMultilingualGlossary(
                    "g1",
                    new UpdateMultilingualGlossaryOptions()
                        .setDictionaries(Arrays.asList(dict, null)))),
        nullRow(
            "csvContent", c -> c.createMultilingualGlossaryFromCsv("n", "en", "de", (String) null)),
        nullRow("name", c -> c.createSpokenTerms(null, Collections.singletonList(terms))),
        nullRow("termLists", c -> c.createSpokenTerms("n", null)),
        nullRow("entries", c -> c.replaceSpokenTermsList("st1", (SpokenTermsEntries) null)),
        nullRow(
            "entries",
            c ->
                c.replaceSpokenTermsList(
                    "st1", new SpokenTermsEntries("en", Arrays.asList("a", null)))),
        nullRow(
            "entries", c -> c.replaceSpokenTermsList("st1", new SpokenTermsEntries("en", null))),
        nullRow(
            "customInstructions",
            c -> c.createStyleRule("n", "en", null, Arrays.asList((CustomInstruction) null), null)),
        nullRow(
            "label",
            c ->
                c.createStyleRule(
                    "n", "en", null, Arrays.asList(new CustomInstruction(null, "P", null)), null)),
        nullRow("configuredRules", c -> c.updateStyleRuleConfiguredRules("s1", null, null)),
        nullRow("name", c -> c.createStyleProfile(null)));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("nullResourceContents")
  void throwsNullPointerExceptionNamingANullResourceContent(
      String name, ThrowingConsumer<DeepLClient> call) {
    assertEquals(
        name, assertThrows(NullPointerException.class, () -> call.accept(translator)).getMessage());
    assertTrue(wm.getAllServeEvents().isEmpty());
  }

  static Stream<Arguments> objectsWithoutId() {
    Date now = new Date();
    return Stream.<ThrowingConsumer<DeepLClient>>of(
            c ->
                c.getMultilingualGlossary(
                    new MultilingualGlossaryInfo(null, "n", now, Collections.emptyList())),
            c -> c.getSpokenTerms(new SpokenTerms(null, "n", now, Collections.emptyList())),
            c ->
                c.getStyleProfile(
                    new StyleProfile(null, "n", null, null, null, null, null, false, now, now)),
            c -> c.getStyleRule(new StyleRuleInfo(null, "n", now, now, "de", 1, null, null)),
            c ->
                c.getTranslationMemory(
                    new TranslationMemoryInfo(null, "n", "en", Collections.singletonList("de"), 1)))
        .map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("objectsWithoutId")
  void throwsIllegalArgumentForAResourceObjectWithoutId(ThrowingConsumer<DeepLClient> call) {
    assertThrows(IllegalArgumentException.class, () -> call.accept(translator));
    assertTrue(wm.getAllServeEvents().isEmpty());
  }
}
