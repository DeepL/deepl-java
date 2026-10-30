// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class MultilingualGlossariesTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String GLOSSARY =
      "{\"glossary_id\":\"g1\",\"name\":\"n\",\"creation_time\":\"2021-08-03T14:16:18.329Z\","
          + "\"dictionaries\":[{\"source_lang\":\"en\",\"target_lang\":\"de\",\"entry_count\":2},"
          + "{\"source_lang\":\"de\",\"target_lang\":\"en\",\"entry_count\":1}]}";
  private static final String DICTIONARY =
      "{\"source_lang\":\"en\",\"target_lang\":\"de\",\"entry_count\":3}";
  private static final String FORM = "application/x-www-form-urlencoded";
  private static final String JSON = "application/json";
  private static final Map<String, String> EXTRA = Collections.singletonMap("extra", "x");

  @TempDir Path tempDir;

  private final DeepLClient client = OfflineClient.createClient(wm);

  private static GlossaryEntries entries(String source, String target) {
    return new GlossaryEntries(Collections.singletonMap(source, target));
  }

  private static MultilingualGlossaryInfo glossaryObject() {
    return new MultilingualGlossaryInfo("g2", "n", new Date(), Collections.emptyList());
  }

  @Test
  void sendsEveryDictionaryWithIndexedKeysWhenCreatingAGlossary() throws Exception {
    wm.stubFor(
        post("/v3/glossaries")
            .withHeader("Content-Type", equalTo(FORM))
            .withRequestBody(
                FormBody.of(
                    "name",
                    "n",
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "Hello\tHallo",
                    "dictionaries[0].entries_format",
                    "tsv",
                    "dictionaries[1].source_lang",
                    "de",
                    "dictionaries[1].target_lang",
                    "en",
                    "dictionaries[1].entries",
                    "Hallo\tHello",
                    "dictionaries[1].entries_format",
                    "tsv"))
            .willReturn(okJson(GLOSSARY)));
    client.createMultilingualGlossary(
        "n",
        Arrays.asList(
            new MultilingualGlossaryDictionaryEntries("en", "de", entries("Hello", "Hallo")),
            new MultilingualGlossaryDictionaryEntries("de", "en", entries("Hallo", "Hello"))));
  }

  @Test
  void sendsSpecialCharactersOfNamesAndEntriesEncoded() throws Exception {
    String name = "Name with special chars: é &foo=bar \"quoted\" ~*'()!";
    wm.stubFor(
        post("/v3/glossaries")
            .withRequestBody(
                FormBody.of(
                    "name",
                    name,
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "A%=&\tB&=%",
                    "dictionaries[0].entries_format",
                    "tsv"))
            .willReturn(okJson(GLOSSARY)));
    client.createMultilingualGlossary(
        name,
        Collections.singletonList(
            new MultilingualGlossaryDictionaryEntries("en", "de", entries("A%=&", "B&=%"))));
  }

  @Test
  void sendsCsvDictionaryBeforeTheNameWhenCreatingAGlossaryFromCsv() throws Exception {
    String csv = "sourceEntry1,targetEntry1\n\"source\"\"Entry\",\"target,Entry\"";
    wm.stubFor(
        post("/v3/glossaries")
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    csv,
                    "dictionaries[0].entries_format",
                    "csv",
                    "name",
                    "n"))
            .willReturn(okJson(GLOSSARY)));
    client.createMultilingualGlossaryFromCsv("n", "en", "de", csv);
  }

  @Test
  void sendsTheContentOfACsvFileWhenCreatingAGlossaryFromCsv() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "a,b\n");
    wm.stubFor(
        post("/v3/glossaries")
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "a,b\n",
                    "dictionaries[0].entries_format",
                    "csv",
                    "name",
                    "n"))
            .willReturn(okJson(GLOSSARY)));
    client.createMultilingualGlossaryFromCsv("n", "en", "de", file);
  }

  @Test
  void parsesGlossaryWithItsDictionaries() throws Exception {
    wm.stubFor(get("/v3/glossaries/g1").willReturn(okJson(GLOSSARY)));
    MultilingualGlossaryInfo glossary = client.getMultilingualGlossary("g1");
    assertEquals("g1", glossary.getGlossaryId());
    assertEquals(1628000178329L, glossary.getCreationTime().getTime());
    assertEquals(2, glossary.getDictionaries().size());
    MultilingualGlossaryDictionaryInfo second = glossary.getDictionaries().get(1);
    assertEquals("de", second.getSourceLanguageCode());
    assertEquals("en", second.getTargetLanguageCode());
    assertEquals(1, second.getEntryCount());
  }

  @Test
  void parsesTheListOfGlossaries() throws Exception {
    wm.stubFor(
        get("/v3/glossaries")
            .withHeader("Content-Type", absent())
            .willReturn(okJson("{\"glossaries\":[" + GLOSSARY + "]}")));
    List<MultilingualGlossaryInfo> glossaries = client.listMultilingualGlossaries();
    assertEquals(1, glossaries.size());
  }

  @Test
  void sendsLanguagesAsQueryAndParsesDictionaryEntries() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/glossaries/g1/entries?source_lang=en&target_lang=de"))
            .willReturn(
                okJson(
                    "{\"dictionaries\":[{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"Apple\\tApfel\\nA%=&\\tB&=%\",\"entries_format\":\"tsv\"}]}")));
    MultilingualGlossaryDictionaryEntries dictionary =
        client.getMultilingualGlossaryDictionaryEntries("g1", "en", "de");
    assertEquals("en", dictionary.getSourceLanguageCode());
    assertEquals("de", dictionary.getTargetLanguageCode());
    assertEquals(2, dictionary.getEntries().size());
    assertEquals("Apfel", dictionary.getEntries().get("Apple"));
    assertEquals("B&=%", dictionary.getEntries().get("A%=&"));
  }

  @Test
  void encodesASpaceInTheLanguageQueryAsPercent20() throws Exception {
    wm.stubFor(
        delete(urlEqualTo("/v3/glossaries/g1/dictionaries?source_lang=en%20x&target_lang=de"))
            .willReturn(aResponse().withStatus(204)));
    client.deleteMultilingualGlossaryDictionary("g1", "en x", "de");
  }

  @Test
  void sendsIdAndLanguagesOfGlossaryAndDictionaryObjectsWhenGettingEntries() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/glossaries/g2/entries?source_lang=en&target_lang=de"))
            .willReturn(
                okJson(
                    "{\"dictionaries\":[{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"a\\tb\",\"entries_format\":\"tsv\"}]}")));
    MultilingualGlossaryDictionaryInfo dictionary =
        new MultilingualGlossaryDictionaryInfo("en", "de", 1);
    client.getMultilingualGlossaryDictionaryEntries(glossaryObject(), dictionary);
    client.getMultilingualGlossaryDictionaryEntries(glossaryObject(), "en", "de");
    client.getMultilingualGlossaryDictionaryEntries("g2", dictionary);
  }

  @Test
  void sendsTsvEntriesWhenReplacingADictionary() throws Exception {
    wm.stubFor(
        put("/v3/glossaries/g1/dictionaries")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson(
                    "{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"key1\\tvalue1\",\"entries_format\":\"tsv\"}"))
            .willReturn(okJson(DICTIONARY)));
    MultilingualGlossaryDictionaryInfo dictionary =
        client.replaceMultilingualGlossaryDictionary("g1", "en", "de", entries("key1", "value1"));
    assertEquals(3, dictionary.getEntryCount());
  }

  @Test
  void sendsTheIdOfAGlossaryObjectWhenReplacingADictionary() throws Exception {
    wm.stubFor(
        put("/v3/glossaries/g2/dictionaries")
            .withRequestBody(
                equalToJson(
                    "{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"key1\\tvalue1\",\"entries_format\":\"tsv\"}"))
            .willReturn(okJson(DICTIONARY)));
    client.replaceMultilingualGlossaryDictionary(
        glossaryObject(),
        new MultilingualGlossaryDictionaryEntries("en", "de", entries("key1", "value1")));
  }

  @Test
  void sendsCsvContentWhenReplacingADictionaryFromCsv() throws Exception {
    wm.stubFor(
        put("/v3/glossaries/g1/dictionaries")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson(
                    "{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"key1,value1\\nkey2,value2\",\"entries_format\":\"csv\"}"))
            .willReturn(okJson(DICTIONARY)));
    client.replaceMultilingualGlossaryDictionaryFromCsv(
        "g1", "en", "de", "key1,value1\nkey2,value2");
  }

  @Test
  void sendsADictionaryUpdateAsPatchWithIndexedKeys() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .withHeader("Content-Type", equalTo(FORM))
            .withHeader("Authorization", equalTo("DeepL-Auth-Key k"))
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "key1\tvalue2",
                    "dictionaries[0].entries_format",
                    "tsv"))
            .willReturn(okJson(GLOSSARY)));
    client.updateMultilingualGlossaryDictionary("g1", "en", "de", entries("key1", "value2"));
  }

  @Test
  void sendsTheContentOfACsvFileWhenReplacingADictionaryFromCsv() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "key1,value1\n");
    wm.stubFor(
        put("/v3/glossaries/g1/dictionaries")
            .withRequestBody(
                equalToJson(
                    "{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"key1,value1\\n\",\"entries_format\":\"csv\"}"))
            .willReturn(okJson(DICTIONARY)));
    client.replaceMultilingualGlossaryDictionaryFromCsv("g1", "en", "de", file);
  }

  @Test
  void sendsTheContentOfACsvFileWhenUpdatingADictionaryFromCsv() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "key1,value1\n");
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "key1,value1\n",
                    "dictionaries[0].entries_format",
                    "csv"))
            .willReturn(okJson(GLOSSARY)));
    client.updateMultilingualGlossaryDictionaryFromCsv("g1", "en", "de", file);
  }

  @Test
  void sendsIdAndLanguagesOfObjectsWhenUpdatingADictionary() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g2"))
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "key1\tvalue2",
                    "dictionaries[0].entries_format",
                    "tsv"))
            .willReturn(okJson(GLOSSARY)));
    GlossaryEntries entries = entries("key1", "value2");
    MultilingualGlossaryDictionaryEntries dictionary =
        new MultilingualGlossaryDictionaryEntries("en", "de", entries);
    client.updateMultilingualGlossaryDictionary(glossaryObject(), "en", "de", entries);
    client.updateMultilingualGlossaryDictionary("g2", dictionary);
    client.updateMultilingualGlossaryDictionary(glossaryObject(), dictionary);
  }

  @Test
  void sendsCsvContentWhenUpdatingADictionaryFromCsv() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "key1,updated",
                    "dictionaries[0].entries_format",
                    "csv"))
            .willReturn(okJson(GLOSSARY)));
    client.updateMultilingualGlossaryDictionaryFromCsv("g1", "en", "de", "key1,updated");
  }

  @Test
  void sendsTheNameEncodedWhenUpdatingTheName() throws Exception {
    String name = "Name with special chars: é &foo=bar \"quoted\"";
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .withHeader("Content-Type", equalTo(FORM))
            .withRequestBody(FormBody.of("name", name))
            .willReturn(okJson(GLOSSARY)));
    client.updateMultilingualGlossaryName("g1", name);
  }

  @Test
  void sendsADeleteRequestWithAnEmptyFormBodyWhenDeletingAGlossary() throws Exception {
    wm.stubFor(
        delete("/v3/glossaries/g1")
            .withHeader("Content-Type", equalTo(FORM))
            .withRequestBody(absent())
            .willReturn(aResponse().withStatus(204)));
    client.deleteMultilingualGlossary("g1");
  }

  @Test
  void sendsTheIdOfAGlossaryObjectWhenDeletingAGlossary() throws Exception {
    wm.stubFor(delete("/v3/glossaries/g2").willReturn(aResponse().withStatus(204)));
    client.deleteMultilingualGlossary(glossaryObject());
  }

  @Test
  void sendsLanguagesAsQueryWhenDeletingADictionary() throws Exception {
    wm.stubFor(
        delete(urlEqualTo("/v3/glossaries/g1/dictionaries?source_lang=en&target_lang=de"))
            .willReturn(aResponse().withStatus(204)));
    client.deleteMultilingualGlossaryDictionary("g1", "en", "de");
  }

  @Test
  void sendsIdAndLanguagesOfObjectsWhenDeletingADictionary() throws Exception {
    wm.stubFor(
        delete(urlEqualTo("/v3/glossaries/g2/dictionaries?source_lang=en&target_lang=de"))
            .willReturn(aResponse().withStatus(204)));
    client.deleteMultilingualGlossaryDictionary(
        glossaryObject(), new MultilingualGlossaryDictionaryInfo("en", "de", 1));
  }

  @Test
  void throwsGlossaryNotFoundWhenTheGlossaryIsUnknown() {
    wm.stubFor(get("/v3/glossaries/g1").willReturn(aResponse().withStatus(404)));
    wm.stubFor(delete("/v3/glossaries/g1").willReturn(aResponse().withStatus(404)));
    wm.stubFor(
        get(urlEqualTo("/v3/glossaries/g1/entries?source_lang=en&target_lang=de"))
            .willReturn(aResponse().withStatus(404)));
    wm.stubFor(put("/v3/glossaries/g1/dictionaries").willReturn(aResponse().withStatus(404)));
    wm.stubFor(patch(urlEqualTo("/v3/glossaries/g1")).willReturn(aResponse().withStatus(404)));
    assertThrows(GlossaryNotFoundException.class, () -> client.getMultilingualGlossary("g1"));
    assertThrows(GlossaryNotFoundException.class, () -> client.deleteMultilingualGlossary("g1"));
    assertThrows(
        GlossaryNotFoundException.class,
        () -> client.getMultilingualGlossaryDictionaryEntries("g1", "en", "de"));
    assertThrows(
        GlossaryNotFoundException.class,
        () -> client.replaceMultilingualGlossaryDictionary("g1", "en", "de", entries("a", "b")));
    assertThrows(
        GlossaryNotFoundException.class, () -> client.updateMultilingualGlossaryName("g1", "name"));
  }

  @Test
  void sendsOnlyTheSetFieldsAndExtraParametersWhenUpdatingAGlossary() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson(
                    "{\"name\":\"renamed\",\"dictionaries\":[{\"source_lang\":\"en\","
                        + "\"target_lang\":\"de\",\"entries\":\"a\\tb\",\"entries_format\":\"tsv\"}],"
                        + "\"extra\":\"x\"}"))
            .willReturn(okJson(GLOSSARY)));
    MultilingualGlossaryInfo glossary =
        client.updateMultilingualGlossary(
            "g1",
            new UpdateMultilingualGlossaryOptions()
                .setName("renamed")
                .setDictionaries(
                    Collections.singletonList(
                        new MultilingualGlossaryDictionaryEntries("en", "de", entries("a", "b"))))
                .setExtraBodyParameters(EXTRA));
    assertEquals("g1", glossary.getGlossaryId());
  }

  @Test
  void sendsAnEmptyJsonObjectWhenUpdatingAGlossaryObjectWithoutOptions() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g2"))
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(equalToJson("{}"))
            .willReturn(okJson(GLOSSARY)));
    client.updateMultilingualGlossary(glossaryObject(), null);
  }

  @Test
  void sendsExtraBodyParametersLastWhenCreatingAGlossary() throws Exception {
    wm.stubFor(
        post("/v3/glossaries")
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "Hello\tHallo",
                    "dictionaries[0].entries_format",
                    "tsv",
                    "extra",
                    "x",
                    "name",
                    "override"))
            .willReturn(okJson(GLOSSARY)));
    Map<String, String> extra = new LinkedHashMap<>();
    extra.put("extra", "x");
    extra.put("name", "override");
    client.createMultilingualGlossary(
        "n",
        Collections.singletonList(
            new MultilingualGlossaryDictionaryEntries("en", "de", entries("Hello", "Hallo"))),
        new CreateMultilingualGlossaryOptions().setExtraBodyParameters(extra));
  }

  @Test
  void sendsExtraBodyParametersLastWhenCreatingAGlossaryFromCsv() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "a,b");
    wm.stubFor(
        post("/v3/glossaries")
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "a,b",
                    "dictionaries[0].entries_format",
                    "csv",
                    "name",
                    "n",
                    "extra",
                    "x"))
            .willReturn(okJson(GLOSSARY)));
    CreateMultilingualGlossaryOptions options =
        new CreateMultilingualGlossaryOptions().setExtraBodyParameters(EXTRA);
    client.createMultilingualGlossaryFromCsv("n", "en", "de", "a,b", options);
    client.createMultilingualGlossaryFromCsv("n", "en", "de", file, options);
  }

  @Test
  void sendsExtraBodyParametersLastInTheJsonBodyWhenReplacingADictionary() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "a,b");
    wm.stubFor(
        put("/v3/glossaries/g2/dictionaries")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson(
                    "{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"a\\tb\",\"entries_format\":\"tsv\",\"extra\":\"x\"}"))
            .willReturn(okJson(DICTIONARY)));
    wm.stubFor(
        put("/v3/glossaries/g2/dictionaries")
            .withHeader("Content-Type", equalTo(JSON))
            .withRequestBody(
                equalToJson(
                    "{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"a,b\",\"entries_format\":\"csv\",\"extra\":\"x\"}"))
            .willReturn(okJson(DICTIONARY)));
    ReplaceMultilingualGlossaryDictionaryOptions options =
        new ReplaceMultilingualGlossaryDictionaryOptions().setExtraBodyParameters(EXTRA);
    MultilingualGlossaryDictionaryEntries dictionary =
        new MultilingualGlossaryDictionaryEntries("en", "de", entries("a", "b"));
    client.replaceMultilingualGlossaryDictionary("g2", dictionary, options);
    client.replaceMultilingualGlossaryDictionary(glossaryObject(), dictionary, options);
    client.replaceMultilingualGlossaryDictionaryFromCsv("g2", "en", "de", "a,b", options);
    client.replaceMultilingualGlossaryDictionaryFromCsv("g2", "en", "de", file, options);
    client.replaceMultilingualGlossaryDictionaryFromCsv(
        glossaryObject(), "en", "de", "a,b", options);
    client.replaceMultilingualGlossaryDictionaryFromCsv(
        glossaryObject(), "en", "de", file, options);
    wm.verify(6, putRequestedFor(urlEqualTo("/v3/glossaries/g2/dictionaries")));
  }

  @Test
  void sendsTheIdOfAGlossaryObjectWhenReplacingADictionaryFromCsv() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "a,b");
    wm.stubFor(
        put("/v3/glossaries/g2/dictionaries")
            .withRequestBody(
                equalToJson(
                    "{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"a,b\",\"entries_format\":\"csv\"}"))
            .willReturn(okJson(DICTIONARY)));
    client.replaceMultilingualGlossaryDictionaryFromCsv(glossaryObject(), "en", "de", "a,b");
    client.replaceMultilingualGlossaryDictionaryFromCsv(glossaryObject(), "en", "de", file);
  }

  @Test
  void sendsExtraBodyParametersLastWhenUpdatingADictionary() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "a,b");
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g2"))
            .withHeader("Content-Type", equalTo(FORM))
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "a\tb",
                    "dictionaries[0].entries_format",
                    "tsv",
                    "extra",
                    "x"))
            .willReturn(okJson(GLOSSARY)));
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g2"))
            .withHeader("Content-Type", equalTo(FORM))
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "a,b",
                    "dictionaries[0].entries_format",
                    "csv",
                    "extra",
                    "x"))
            .willReturn(okJson(GLOSSARY)));
    UpdateMultilingualGlossaryOptions options =
        new UpdateMultilingualGlossaryOptions().setExtraBodyParameters(EXTRA);
    MultilingualGlossaryDictionaryEntries dictionary =
        new MultilingualGlossaryDictionaryEntries("en", "de", entries("a", "b"));
    client.updateMultilingualGlossaryDictionary("g2", dictionary, options);
    client.updateMultilingualGlossaryDictionary(glossaryObject(), dictionary, options);
    client.updateMultilingualGlossaryDictionaryFromCsv("g2", "en", "de", "a,b", options);
    client.updateMultilingualGlossaryDictionaryFromCsv("g2", "en", "de", file, options);
    client.updateMultilingualGlossaryDictionaryFromCsv(
        glossaryObject(), "en", "de", "a,b", options);
    client.updateMultilingualGlossaryDictionaryFromCsv(glossaryObject(), "en", "de", file, options);
    wm.verify(6, patchRequestedFor(urlEqualTo("/v3/glossaries/g2")));
  }

  @Test
  void sendsTheIdOfAGlossaryObjectWhenUpdatingADictionaryFromCsv() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "a,b");
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g2"))
            .withRequestBody(
                FormBody.of(
                    "dictionaries[0].source_lang",
                    "en",
                    "dictionaries[0].target_lang",
                    "de",
                    "dictionaries[0].entries",
                    "a,b",
                    "dictionaries[0].entries_format",
                    "csv"))
            .willReturn(okJson(GLOSSARY)));
    client.updateMultilingualGlossaryDictionaryFromCsv(glossaryObject(), "en", "de", "a,b");
    client.updateMultilingualGlossaryDictionaryFromCsv(glossaryObject(), "en", "de", file);
  }

  @Test
  void sendsTheIdOfAGlossaryObjectAndExtraParametersWhenUpdatingTheName() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g2"))
            .withRequestBody(FormBody.of("name", "renamed"))
            .willReturn(okJson(GLOSSARY)));
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g2"))
            .withRequestBody(FormBody.of("name", "renamed", "extra", "x"))
            .willReturn(okJson(GLOSSARY)));
    UpdateMultilingualGlossaryOptions options =
        new UpdateMultilingualGlossaryOptions().setExtraBodyParameters(EXTRA);
    client.updateMultilingualGlossaryName(glossaryObject(), "renamed");
    client.updateMultilingualGlossaryName("g2", "renamed", options);
    client.updateMultilingualGlossaryName(glossaryObject(), "renamed", options);
  }

  @Test
  void sendsTheIdOfAGlossaryObjectWhenGettingAGlossary() throws Exception {
    wm.stubFor(get("/v3/glossaries/g2").willReturn(okJson(GLOSSARY)));
    assertEquals("g1", client.getMultilingualGlossary(glossaryObject()).getGlossaryId());
  }

  @Test
  void throwsNotFoundWhenTheEntriesResponseHasNoDictionary() {
    wm.stubFor(
        get(urlEqualTo("/v3/glossaries/g1/entries?source_lang=en&target_lang=de"))
            .willReturn(okJson("{\"dictionaries\":[]}")));
    assertThrows(
        NotFoundException.class,
        () -> client.getMultilingualGlossaryDictionaryEntries("g1", "en", "de"));
  }

  @Test
  void throwsDeepLExceptionWhenTheEntriesResponseIsMalformed() {
    wm.stubFor(
        get(urlEqualTo("/v3/glossaries/g1/entries?source_lang=en&target_lang=de"))
            .willReturn(okJson("{}")));
    wm.stubFor(
        get(urlEqualTo("/v3/glossaries/g2/entries?source_lang=en&target_lang=de"))
            .willReturn(okJson("{\"dictionaries\":[{\"source_lang\":\"en\"}]}")));
    DeepLException absent =
        assertThrows(
            DeepLException.class,
            () -> client.getMultilingualGlossaryDictionaryEntries("g1", "en", "de"));
    assertEquals(DeepLException.class, absent.getClass());
    DeepLException malformed =
        assertThrows(
            DeepLException.class,
            () -> client.getMultilingualGlossaryDictionaryEntries("g2", "en", "de"));
    assertEquals(DeepLException.class, malformed.getClass());
  }

  @Test
  void throwsGlossaryNotFoundWhenCreatingOrListingReturns404() {
    wm.stubFor(post("/v3/glossaries").willReturn(aResponse().withStatus(404)));
    wm.stubFor(get("/v3/glossaries").willReturn(aResponse().withStatus(404)));
    assertThrows(
        GlossaryNotFoundException.class,
        () ->
            client.createMultilingualGlossary(
                "n",
                Collections.singletonList(
                    new MultilingualGlossaryDictionaryEntries("en", "de", entries("a", "b")))));
    assertThrows(
        GlossaryNotFoundException.class,
        () -> client.createMultilingualGlossaryFromCsv("n", "en", "de", "a,b"));
    assertThrows(GlossaryNotFoundException.class, () -> client.listMultilingualGlossaries());
  }

  @Test
  void sendsTheGlossaryIdUrlEncodedInThePath() throws Exception {
    String id = "a/b c?d";
    String path = "/v3/glossaries/a%2Fb%20c%3Fd";
    String languages = "?source_lang=en&target_lang=de";
    wm.stubFor(get(urlEqualTo(path)).willReturn(okJson(GLOSSARY)));
    wm.stubFor(patch(urlEqualTo(path)).willReturn(okJson(GLOSSARY)));
    wm.stubFor(delete(urlEqualTo(path)).willReturn(aResponse().withStatus(204)));
    wm.stubFor(
        get(urlEqualTo(path + "/entries" + languages))
            .willReturn(
                okJson(
                    "{\"dictionaries\":[{\"source_lang\":\"en\",\"target_lang\":\"de\","
                        + "\"entries\":\"a\\tb\",\"entries_format\":\"tsv\"}]}")));
    wm.stubFor(put(urlEqualTo(path + "/dictionaries")).willReturn(okJson(DICTIONARY)));
    wm.stubFor(
        delete(urlEqualTo(path + "/dictionaries" + languages))
            .willReturn(aResponse().withStatus(204)));
    GlossaryEntries entries = entries("a", "b");
    client.getMultilingualGlossary(id);
    client.getMultilingualGlossaryDictionaryEntries(id, "en", "de");
    client.updateMultilingualGlossaryName(id, "n");
    client.updateMultilingualGlossaryDictionary(id, "en", "de", entries);
    client.updateMultilingualGlossaryDictionaryFromCsv(id, "en", "de", "a,b");
    client.updateMultilingualGlossary(id, null);
    client.replaceMultilingualGlossaryDictionary(id, "en", "de", entries);
    client.replaceMultilingualGlossaryDictionaryFromCsv(id, "en", "de", "a,b");
    client.deleteMultilingualGlossaryDictionary(id, "en", "de");
    client.deleteMultilingualGlossary(id);
  }

  @Test
  void throwsIllegalArgumentWhenCreatingAGlossaryWithoutNameOrDictionaries() {
    MultilingualGlossaryDictionaryEntries dictionary =
        new MultilingualGlossaryDictionaryEntries("en", "de", entries("a", "b"));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.createMultilingualGlossary("", Collections.singletonList(dictionary)));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.createMultilingualGlossary("n", Collections.emptyList()));
  }

  @Test
  void throwsIllegalArgumentWhenIdsOrLanguagesAreEmpty() {
    GlossaryEntries entries = entries("a", "b");
    assertThrows(
        IllegalArgumentException.class,
        () -> client.getMultilingualGlossaryDictionaryEntries("g1", "", "de"));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.getMultilingualGlossaryDictionaryEntries("", "en", "de"));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.replaceMultilingualGlossaryDictionary("", "en", "de", entries));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.updateMultilingualGlossaryDictionary("g1", "en", "", entries));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.deleteMultilingualGlossaryDictionary("g1", "", "de"));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.replaceMultilingualGlossaryDictionaryFromCsv("g1", "en", "de", ""));
  }
}
