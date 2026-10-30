// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("deprecation")
class GlossariesTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String GLOSSARY =
      "{\"glossary_id\":\"g1\",\"name\":\"n\",\"ready\":true,\"source_lang\":\"en\","
          + "\"target_lang\":\"de\",\"creation_time\":\"2021-08-03T14:16:18.329Z\","
          + "\"entry_count\":2}";

  @TempDir Path tempDir;

  private final Translator translator = OfflineClient.createTranslator(wm);

  @Test
  void sendsTsvEntriesWhenCreatingAGlossary() throws Exception {
    wm.stubFor(
        post("/v2/glossaries")
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(
                FormBody.of(
                    "name",
                    "my glossary & more",
                    "source_lang",
                    "en",
                    "target_lang",
                    "de",
                    "entries_format",
                    "tsv",
                    "entries",
                    "Hello\tHallo"))
            .willReturn(okJson(GLOSSARY)));
    translator.createGlossary(
        "my glossary & more",
        "en",
        "de",
        new GlossaryEntries(Collections.singletonMap("Hello", "Hallo")));
  }

  @Test
  void sendsCsvContentWhenCreatingAGlossaryFromCsv() throws Exception {
    String csv = "sourceEntry1,targetEntry1,en,de\n\"source\"\"Entry\",\"target,Entry\",en,de";
    wm.stubFor(
        post("/v2/glossaries")
            .withRequestBody(
                FormBody.of(
                    "name",
                    "n",
                    "source_lang",
                    "en",
                    "target_lang",
                    "de",
                    "entries_format",
                    "csv",
                    "entries",
                    csv))
            .willReturn(okJson(GLOSSARY)));
    translator.createGlossaryFromCsv("n", "en", "de", csv);
  }

  @Test
  void sendsTheContentOfACsvFileWhenCreatingAGlossaryFromCsv() throws Exception {
    File file = Fixtures.writeFile(tempDir.resolve("entries.csv").toFile(), "a,b\n");
    wm.stubFor(
        post("/v2/glossaries")
            .withRequestBody(
                FormBody.of(
                    "name",
                    "n",
                    "source_lang",
                    "en",
                    "target_lang",
                    "de",
                    "entries_format",
                    "csv",
                    "entries",
                    "a,b\n"))
            .willReturn(okJson(GLOSSARY)));
    translator.createGlossaryFromCsv("n", "en", "de", file);
  }

  @Test
  void parsesGlossaryInfo() throws Exception {
    wm.stubFor(post("/v2/glossaries").willReturn(okJson(GLOSSARY)));
    GlossaryInfo glossary =
        translator.createGlossary(
            "n", "en", "de", new GlossaryEntries(Collections.singletonMap("a", "b")));
    assertEquals("g1", glossary.getGlossaryId());
    assertEquals("en", glossary.getSourceLang());
    assertEquals("de", glossary.getTargetLang());
    assertEquals(2, glossary.getEntryCount());
    assertEquals(1628000178329L, glossary.getCreationTime().getTime());
  }

  @Test
  void sendsAGetRequestWhenGettingAGlossary() throws Exception {
    wm.stubFor(
        get("/v2/glossaries/g1").withHeader("Content-Type", absent()).willReturn(okJson(GLOSSARY)));
    assertEquals("g1", translator.getGlossary("g1").getGlossaryId());
  }

  @Test
  void parsesTheListOfGlossaries() throws Exception {
    wm.stubFor(
        get("/v2/glossaries")
            .willReturn(okJson("{\"glossaries\":[" + GLOSSARY + "," + GLOSSARY + "]}")));
    List<GlossaryInfo> glossaries = translator.listGlossaries();
    assertEquals(2, glossaries.size());
    assertEquals("g1", glossaries.get(1).getGlossaryId());
  }

  @Test
  void parsesTsvEntriesOfAGlossary() throws Exception {
    wm.stubFor(
        get("/v2/glossaries/g1/entries")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/tab-separated-values")
                    .withBody("Apple\tApfel\nA%=&\tB&=%\nΔぁ\t深")));
    GlossaryEntries entries = translator.getGlossaryEntries("g1");
    assertEquals(3, entries.size());
    assertEquals("Apfel", entries.get("Apple"));
    assertEquals("B&=%", entries.get("A%=&"));
    assertEquals("深", entries.get("Δぁ"));
  }

  @Test
  void sendsTheIdOfAGlossaryObject() throws Exception {
    GlossaryInfo info = new GlossaryInfo("g2", "n", true, "en", "de", new java.util.Date(), 1);
    wm.stubFor(
        get("/v2/glossaries/g2/entries").willReturn(aResponse().withStatus(200).withBody("a\tb")));
    wm.stubFor(delete("/v2/glossaries/g2").willReturn(aResponse().withStatus(204)));
    translator.getGlossaryEntries(info);
    translator.deleteGlossary(info);
  }

  @Test
  void sendsADeleteRequestWithAnEmptyFormBody() throws Exception {
    wm.stubFor(
        delete("/v2/glossaries/g1")
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(absent())
            .willReturn(aResponse().withStatus(204)));
    translator.deleteGlossary("g1");
  }

  @Test
  void throwsGlossaryNotFoundWhenTheGlossaryIsUnknown() {
    wm.stubFor(get("/v2/glossaries/g1").willReturn(aResponse().withStatus(404)));
    wm.stubFor(get("/v2/glossaries/g1/entries").willReturn(aResponse().withStatus(404)));
    wm.stubFor(delete("/v2/glossaries/g1").willReturn(aResponse().withStatus(404)));
    assertThrows(GlossaryNotFoundException.class, () -> translator.getGlossary("g1"));
    assertThrows(GlossaryNotFoundException.class, () -> translator.getGlossaryEntries("g1"));
    assertThrows(GlossaryNotFoundException.class, () -> translator.deleteGlossary("g1"));
  }

  @Test
  void throwsNotFoundWhenTheGlossaryListIsUnknown() {
    wm.stubFor(get("/v2/glossaries").willReturn(aResponse().withStatus(404)));
    NotFoundException thrown = assertThrows(NotFoundException.class, translator::listGlossaries);
    assertFalse(thrown instanceof GlossaryNotFoundException);
  }

  @Test
  void parsesTsvEntriesTrimmingWhitespace() {
    GlossaryEntries expected = new GlossaryEntries();
    expected.put("apple", "Apfel");
    expected.put("crab apple", "Holzapfel");
    assertEquals(expected, GlossaryEntries.fromTsv("apple\tApfel\n crab apple \t Holzapfel "));
  }

  @Test
  void throwsIllegalArgumentWhenTsvEntriesAreInvalid() {
    assertThrows(IllegalArgumentException.class, () -> GlossaryEntries.fromTsv(""));
    assertThrows(
        IllegalArgumentException.class,
        () -> GlossaryEntries.fromTsv("Küche\tKitchen\nKüche\tCuisine"));
    assertThrows(IllegalArgumentException.class, () -> GlossaryEntries.fromTsv("A\tB\tC"));
    assertThrows(IllegalArgumentException.class, () -> GlossaryEntries.fromTsv("A\t "));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GlossaryEntries(Collections.singletonMap("A", "B\tC")));
  }

  @Test
  void namesTheInvalidCharacterInHexadecimal() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new GlossaryEntries(Collections.singletonMap("A", "B\u001fC")));
    assertTrue(thrown.getMessage().contains("U+001F"), thrown.getMessage());
  }

  @Test
  void rejectsInvalidTargetTermsSetThroughReplaceAll() {
    GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("a", "b"));
    assertThrows(IllegalArgumentException.class, () -> entries.replaceAll((k, v) -> "x\ty"));
    assertEquals("b", entries.get("a"));
    entries.replaceAll((k, v) -> " c ");
    assertEquals("c", entries.get("a"));
  }

  @Test
  void rejectsInvalidTargetTermsSetThroughEntrySetValue() {
    GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("a", "b"));
    Map.Entry<String, String> entry = entries.entrySet().iterator().next();
    assertThrows(IllegalArgumentException.class, () -> entry.setValue("x\ty"));
    assertEquals("b", entries.get("a"));
  }
}
