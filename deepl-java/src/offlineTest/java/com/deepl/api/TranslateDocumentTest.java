// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class TranslateDocumentTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String HANDLE = "{\"document_id\":\"D1\",\"document_key\":\"K1\"}";
  private static final String DONE =
      "{\"document_id\":\"D1\",\"status\":\"done\",\"billed_characters\":11}";
  private static final DocumentHandle HANDLE_OBJECT = new DocumentHandle("D1", "K1");

  @TempDir Path tempDir;

  private final DeepLClient client = OfflineClient.createClient(wm);

  /** Stubs a document upload that expects exactly the given file and form fields. */
  private void expectUpload(
      String path, String fileName, byte[] content, Map<String, String> fields) {
    MappingBuilder stub =
        post(path)
            .withHeader("Content-Type", matching("multipart/form-data; boundary=.+"))
            .withMultipartRequestBody(
                aMultipart()
                    .withName("file")
                    .withHeader("Content-Disposition", containing("filename=\"" + fileName + "\""))
                    .withBody(binaryEqualTo(content)));
    for (Map.Entry<String, String> field : fields.entrySet()) {
      stub.withMultipartRequestBody(
          aMultipart().withName(field.getKey()).withBody(equalTo(field.getValue())));
    }
    wm.stubFor(stub.willReturn(okJson(HANDLE)));
  }

  private static Map<String, String> fields(String... keysAndValues) {
    Map<String, String> fields = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      fields.put(keysAndValues[i], keysAndValues[i + 1]);
    }
    return fields;
  }

  /** The parts of every request to {@code path}, in order: the stub only checks presence. */
  private void assertPartNames(String path, String... expected) {
    int uploads = 0;
    for (ServeEvent event : wm.getAllServeEvents()) {
      if (!event.getRequest().getUrl().equals(path)) {
        continue;
      }
      uploads++;
      List<String> names = new ArrayList<>();
      for (Request.Part part : event.getRequest().getParts()) {
        names.add(part.getName());
      }
      assertEquals(Arrays.asList(expected), names);
    }
    assertTrue(uploads > 0, "No request to " + path);
  }

  private File writeInput(String name, String content) throws IOException {
    return Fixtures.writeFile(tempDir.resolve(name).toFile(), content);
  }

  @Test
  void sendsTextFileAndLanguagesAsMultipartParts() throws Exception {
    File input = writeInput("example.txt", "proton beam");
    expectUpload(
        "/v2/document",
        "example.txt",
        "proton beam".getBytes(StandardCharsets.UTF_8),
        fields("source_lang", "en", "target_lang", "de"));
    client.translateDocumentUpload(input, "EN", "DE");
    assertPartNames("/v2/document", "file", "source_lang", "target_lang");
  }

  @Test
  void sendsBinaryFileBytesUnchanged() throws Exception {
    byte[] bytes = new byte[] {0x50, 0x4b, 0x03, 0x04, (byte) 0xff, 0x00, (byte) 0x80, 0x0d, 0x0a};
    File input = tempDir.resolve("example.docx").toFile();
    java.nio.file.Files.write(input.toPath(), bytes);
    expectUpload("/v2/document", "example.docx", bytes, fields("target_lang", "de"));
    client.translateDocumentUpload(input, null, "de");
    assertPartNames("/v2/document", "file", "target_lang");
  }

  @Test
  void sendsAllDocumentOptionsInOrder() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields(
            "source_lang", "en",
            "target_lang", "de",
            "formality", "prefer_more",
            "glossary_id", "g1",
            "style_id", "s1",
            "style_profile_id", "sp1",
            "translation_memory_id", "tm1",
            "translation_memory_threshold", "80",
            "output_format", "pdf"));
    client.translateDocumentUpload(
        input,
        "en",
        "de",
        new DocumentTranslationOptions()
            .setFormality(Formality.PreferMore)
            .setGlossaryId("g1")
            .setStyleId("s1")
            .setStyleProfile("sp1")
            .setTranslationMemoryId("tm1")
            .setTranslationMemoryThreshold(80)
            .setOutputFormat("pdf"));
    assertPartNames(
        "/v2/document",
        "file",
        "source_lang",
        "target_lang",
        "formality",
        "glossary_id",
        "style_id",
        "style_profile_id",
        "translation_memory_id",
        "translation_memory_threshold",
        "output_format");
  }

  @Test
  void sendsEnableWatermarkWithOutputFormat() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "output_format", "docx", "enable_watermark", "true"));
    client.translateDocumentUpload(
        input,
        null,
        "de",
        new DocumentTranslationOptions().setOutputFormat("docx").setEnableWatermark(true));
    assertPartNames("/v2/document", "file", "target_lang", "output_format", "enable_watermark");
  }

  @Test
  void sendsEnableWatermarkFalseWhenExplicitlyDisabled() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "enable_watermark", "false"));
    client.translateDocumentUpload(
        input, null, "de", new DocumentTranslationOptions().setEnableWatermark(false));
    assertPartNames("/v2/document", "file", "target_lang", "enable_watermark");
  }

  @Test
  void omitsEnableWatermarkWhenUnset() throws Exception {
    File input = writeInput("example.txt", "a");
    DocumentTranslationOptions options = new DocumentTranslationOptions().setOutputFormat("docx");
    assertNull(options.getEnableWatermark());
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "output_format", "docx"));
    client.translateDocumentUpload(input, null, "de", options);
    assertPartNames("/v2/document", "file", "target_lang", "output_format");
  }

  @Test
  void sendsGlossaryIdsJoinedByCommaAndExtraBodyParametersLast() throws Exception {
    File input = writeInput("example.txt", "a");
    Map<String, String> extra = new LinkedHashMap<>();
    extra.put("target_lang", "fr");
    extra.put("debug", "1");
    DocumentTranslationOptions options =
        new DocumentTranslationOptions().setExtraBodyParameters(extra).setGlossaryIds("g1", "g2");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("source_lang", "en", "glossary_ids", "g1,g2", "target_lang", "fr", "debug", "1"));
    client.translateDocumentUpload(input, "en", "de", options);
    assertPartNames("/v2/document", "file", "source_lang", "glossary_ids", "target_lang", "debug");
  }

  @Test
  void sendsTheIdsOfAStyleRuleAndATranslationMemory() throws Exception {
    File input = writeInput("example.txt", "a");
    StyleRuleInfo rule =
        new StyleRuleInfo(
            "s2", "n", new java.util.Date(), new java.util.Date(), "de", 1, null, null);
    TranslationMemoryInfo memory =
        new TranslationMemoryInfo("tm2", "n", "en", Arrays.asList("de"), 1);
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "style_id", "s2", "translation_memory_id", "tm2"));
    client.translateDocumentUpload(
        input,
        null,
        "de",
        new DocumentTranslationOptions().setStyleRule(rule).setTranslationMemory(memory));
    assertPartNames("/v2/document", "file", "target_lang", "style_id", "translation_memory_id");
  }

  @Test
  void sendsStyleProfileIdAsOneField() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "style_profile_id", "sp1"));
    client.translateDocumentUpload(
        input, null, "de", new DocumentTranslationOptions().setStyleProfile("sp1"));
    assertPartNames("/v2/document", "file", "target_lang", "style_profile_id");
  }

  @Test
  void sendsTheIdOfAStyleProfile() throws Exception {
    File input = writeInput("example.txt", "a");
    StyleProfile profile =
        new StyleProfile("sp2", "n", null, null, null, null, null, false, null, null);
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "style_profile_id", "sp2"));
    client.translateDocumentUpload(
        input, null, "de", new DocumentTranslationOptions().setStyleProfile(profile));
    assertPartNames("/v2/document", "file", "target_lang", "style_profile_id");
  }

  @Test
  void omitsStyleProfileIdWhenUnset() throws Exception {
    File input = writeInput("example.txt", "a");
    DocumentTranslationOptions options = new DocumentTranslationOptions().setStyleId("s1");
    assertNull(options.getStyleProfileId());
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "style_id", "s1"));
    client.translateDocumentUpload(input, null, "de", options);
    assertPartNames("/v2/document", "file", "target_lang", "style_id");
  }

  @Test
  void rejectsANullStyleProfile() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DocumentTranslationOptions().setStyleProfile((StyleProfile) null));
  }

  @Test
  void sendsStreamUploadsToThePathWithoutATrailingSlash() throws Exception {
    expectUpload(
        "/v2/document",
        "stream.txt",
        "abc".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de"));
    DocumentHandle handle =
        client.translateDocumentUpload(
            new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)),
            "stream.txt",
            null,
            "de");
    assertPartNames("/v2/document", "file", "target_lang");
    assertEquals("D1", handle.getDocumentId());
    assertEquals("K1", handle.getDocumentKey());
  }

  @Test
  void sendsTheDocumentKeyWhenAskingForTheStatus() throws Exception {
    wm.stubFor(
        post("/v2/document/D1")
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(FormBody.of("document_key", "K1"))
            .willReturn(
                okJson(
                    "{\"document_id\":\"D1\",\"status\":\"translating\","
                        + "\"seconds_remaining\":7,\"billed_characters\":3}")));
    DocumentStatus status = client.translateDocumentGetStatus(HANDLE_OBJECT);
    assertEquals(DocumentStatus.StatusCode.Translating, status.getStatus());
    assertTrue(status.ok());
    assertFalse(status.done());
    assertEquals(7L, status.getSecondsRemaining());
    assertEquals(3L, status.getBilledCharacters());
  }

  @Test
  void parsesErrorStatusAsNotOk() throws Exception {
    wm.stubFor(
        post("/v2/document/D1")
            .willReturn(
                okJson(
                    "{\"document_id\":\"D1\",\"status\":\"error\",\"error_message\":\"boom\"}")));
    DocumentStatus status = client.translateDocumentGetStatus(HANDLE_OBJECT);
    assertFalse(status.ok());
    assertFalse(status.done());
    assertEquals("boom", status.getErrorMessage());
  }

  @Test
  void pollsTheStatusUntilTheDocumentIsDone() throws Exception {
    wm.stubFor(
        post("/v2/document/D1")
            .inScenario("poll")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("translating")
            .willReturn(okJson("{\"document_id\":\"D1\",\"status\":\"queued\"}")));
    wm.stubFor(
        post("/v2/document/D1")
            .inScenario("poll")
            .whenScenarioStateIs("translating")
            .willReturn(okJson(DONE)));
    DocumentStatus status = client.translateDocumentWaitUntilDone(HANDLE_OBJECT);
    assertTrue(status.done());
    assertEquals(11L, status.getBilledCharacters());
  }

  @Test
  void throwsDeepLExceptionWithTheErrorMessageWhenWaitingForAFailedDocument() {
    wm.stubFor(
        post("/v2/document/D1")
            .willReturn(
                okJson(
                    "{\"document_id\":\"D1\",\"status\":\"error\",\"error_message\":\"boom\"}")));
    DeepLException thrown =
        assertThrows(
            DeepLException.class, () -> client.translateDocumentWaitUntilDone(HANDLE_OBJECT));
    assertEquals("boom", thrown.getMessage());
  }

  @Test
  void throwsDeepLExceptionWithAGenericMessageWhenAFailedDocumentHasNoErrorMessage() {
    wm.stubFor(
        post("/v2/document/D1")
            .willReturn(okJson("{\"document_id\":\"D1\",\"status\":\"error\"}")));
    DeepLException thrown =
        assertThrows(
            DeepLException.class, () -> client.translateDocumentWaitUntilDone(HANDLE_OBJECT));
    assertEquals("Unknown error", thrown.getMessage());
  }

  @Test
  void sendsTheDocumentKeyWhenDownloadingTheResultToAStream() throws Exception {
    byte[] bytes = new byte[] {0x00, (byte) 0xff, 0x7f, (byte) 0x80, 0x0a};
    wm.stubFor(
        post("/v2/document/D1/result")
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(FormBody.of("document_key", "K1"))
            .willReturn(aResponse().withStatus(200).withBody(bytes)));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    client.translateDocumentDownload(HANDLE_OBJECT, output);
    assertArrayEquals(bytes, output.toByteArray());
  }

  @Test
  void sendsTheDocumentKeyWhenDownloadingTheResultToAFile() throws Exception {
    wm.stubFor(
        post("/v2/document/D1/result")
            .withRequestBody(FormBody.of("document_key", "K1"))
            .willReturn(aResponse().withStatus(200).withBody("Protonenstrahl")));
    File output = tempDir.resolve("out.txt").toFile();
    client.translateDocumentDownload(HANDLE_OBJECT, output);
    assertEquals("Protonenstrahl", Fixtures.readFile(output));
  }

  @Test
  void throwsDocumentNotReadyWhenTheResultIsNotAvailableYet() {
    wm.stubFor(post("/v2/document/D1/result").willReturn(aResponse().withStatus(503)));
    assertThrows(
        DocumentNotReadyException.class,
        () -> client.translateDocumentDownload(HANDLE_OBJECT, new ByteArrayOutputStream()));
  }

  @Test
  void throwsNotFoundWhenTheHandleIsUnknown() {
    wm.stubFor(post("/v2/document/D1").willReturn(aResponse().withStatus(404)));
    assertThrows(NotFoundException.class, () -> client.translateDocumentStatus(HANDLE_OBJECT));
  }

  @Test
  void uploadsPollsAndDownloadsWhenTranslatingAFile() throws Exception {
    File input = writeInput("example.txt", "proton beam");
    File output = tempDir.resolve("out.txt").toFile();
    expectUpload(
        "/v2/document",
        "example.txt",
        "proton beam".getBytes(StandardCharsets.UTF_8),
        fields("source_lang", "en", "target_lang", "de"));
    wm.stubFor(
        post("/v2/document/D1")
            .withRequestBody(FormBody.of("document_key", "K1"))
            .willReturn(okJson(DONE)));
    wm.stubFor(
        post("/v2/document/D1/result")
            .withRequestBody(FormBody.of("document_key", "K1"))
            .willReturn(aResponse().withStatus(200).withBody("Protonenstrahl")));
    DocumentStatus status = client.translateDocument(input, output, "en", "de");
    assertPartNames("/v2/document", "file", "source_lang", "target_lang");
    assertTrue(status.done());
    assertEquals("Protonenstrahl", Fixtures.readFile(output));
  }

  @Test
  void sendsEnableWatermarkWhenTranslatingAFile() throws Exception {
    File input = writeInput("example.txt", "a");
    File output = tempDir.resolve("out.txt").toFile();
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "enable_watermark", "true"));
    wm.stubFor(post("/v2/document/D1").willReturn(okJson(DONE)));
    wm.stubFor(
        post("/v2/document/D1/result").willReturn(aResponse().withStatus(200).withBody("b")));
    client.translateDocument(
        input, output, null, "de", new DocumentTranslationOptions().setEnableWatermark(true));
    assertPartNames("/v2/document", "file", "target_lang", "enable_watermark");
  }

  @Test
  void uploadsStreamsToThePathWithoutATrailingSlashWhenTranslatingStreams() throws Exception {
    expectUpload(
        "/v2/document",
        "stream.txt",
        "abc".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de"));
    wm.stubFor(post("/v2/document/D1").willReturn(okJson(DONE)));
    wm.stubFor(
        post("/v2/document/D1/result").willReturn(aResponse().withStatus(200).withBody("xyz")));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    client.translateDocument(
        new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)),
        "stream.txt",
        output,
        null,
        "de",
        null);
    assertPartNames("/v2/document", "file", "target_lang");
    assertEquals("xyz", new String(output.toByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  void throwsDocumentTranslationExceptionWithTheHandleWhenTheTranslationFails() throws Exception {
    File input = writeInput("example.txt", "a");
    File output = tempDir.resolve("out.txt").toFile();
    wm.stubFor(post("/v2/document").willReturn(okJson(HANDLE)));
    wm.stubFor(
        post("/v2/document/D1")
            .willReturn(
                okJson(
                    "{\"document_id\":\"D1\",\"status\":\"error\","
                        + "\"error_message\":\"Source and target language are equal\"}")));
    DocumentTranslationException thrown =
        assertThrows(
            DocumentTranslationException.class,
            () -> client.translateDocument(input, output, null, "de"));
    assertTrue(thrown.getMessage().contains("Source and target language are equal"));
    assertEquals("D1", thrown.getHandle().getDocumentId());
    assertFalse(output.exists());
  }

  @Test
  void throwsDocumentTranslationExceptionWithoutHandleWhenTheUploadFails() throws Exception {
    File input = writeInput("example.xyz", "a");
    File output = tempDir.resolve("out.xyz").toFile();
    wm.stubFor(
        post("/v2/document")
            .willReturn(aResponse().withStatus(400).withBody("{\"message\":\"Invalid file\"}")));
    DocumentTranslationException thrown =
        assertThrows(
            DocumentTranslationException.class,
            () -> client.translateDocument(input, output, "en", "de"));
    assertNull(thrown.getHandle());
    assertTrue(thrown.getMessage().contains("Invalid file"));
    assertEquals(400, thrown.getHttpStatusCode());
    assertFalse(thrown.getShouldRetry());
  }

  @Test
  void keepsAnExistingOutputFileWhenTranslatingAFile() throws Exception {
    File input = writeInput("example.txt", "a");
    File output = writeInput("out.txt", "already here");
    assertThrows(IOException.class, () -> client.translateDocument(input, output, "en", "de"));
    assertEquals("already here", Fixtures.readFile(output));
  }

  @Test
  void keepsAnExistingOutputFileWhenDownloadingAResultToAFile() throws Exception {
    File output = writeInput("out.txt", "already here");
    assertThrows(IOException.class, () -> client.translateDocumentDownload(HANDLE_OBJECT, output));
    assertEquals("already here", Fixtures.readFile(output));
  }

  @Test
  void retriesTheUploadWithTheSameBodyAfterTooManyRequests() throws Exception {
    File input = writeInput("example.txt", "retry me");
    wm.stubFor(
        post("/v2/document")
            .inScenario("upload")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("second")
            .willReturn(aResponse().withStatus(429)));
    wm.stubFor(
        post("/v2/document")
            .inScenario("upload")
            .whenScenarioStateIs("second")
            .withMultipartRequestBody(
                aMultipart()
                    .withName("file")
                    .withBody(binaryEqualTo("retry me".getBytes(StandardCharsets.UTF_8))))
            .withMultipartRequestBody(aMultipart().withName("target_lang").withBody(equalTo("de")))
            .willReturn(okJson(HANDLE)));
    DocumentHandle handle = client.translateDocumentUpload(input, null, "de");
    assertNotNull(handle);
    assertEquals(2, wm.getAllServeEvents().size());
    assertPartNames("/v2/document", "file", "target_lang");
  }

  @Test
  void sendsAFormalityStringAsGiven() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "formality", "prefer_less_new"));
    client.translateDocumentUpload(
        input, null, "de", new DocumentTranslationOptions().setFormality("prefer_less_new"));
    assertPartNames("/v2/document", "file", "target_lang", "formality");
  }

  @Test
  void sendsTheIdsGivenToSetStyleRuleAndSetTranslationMemory() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de", "style_id", "s3", "translation_memory_id", "tm3"));
    client.translateDocumentUpload(
        input,
        null,
        "de",
        new DocumentTranslationOptions().setStyleRule("s3").setTranslationMemory("tm3"));
    assertPartNames("/v2/document", "file", "target_lang", "style_id", "translation_memory_id");
  }

  @Test
  void sendsTheIdsOfGlossaryObjectsGivenToSetGlossaryIds() throws Exception {
    File input = writeInput("example.txt", "a");
    GlossaryInfo glossary = new GlossaryInfo("g1", "n", true, "en", "de", new java.util.Date(), 1);
    MultilingualGlossaryInfo multilingual =
        new MultilingualGlossaryInfo("g2", "n", new java.util.Date(), new ArrayList<>());
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("source_lang", "en", "target_lang", "de", "glossary_ids", "g1,g2"));
    client.translateDocumentUpload(
        input, "en", "de", new DocumentTranslationOptions().setGlossaryIds(glossary, multilingual));
    assertPartNames("/v2/document", "file", "source_lang", "target_lang", "glossary_ids");
  }

  @Test
  void sendsLanguageCodesWithOnlyTheBaseSubtagLowerCased() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "example.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("source_lang", "en", "target_lang", "zh-Hans"));
    client.translateDocumentUpload(input, "EN", "ZH-Hans");
    assertPartNames("/v2/document", "file", "source_lang", "target_lang");
  }

  @Test
  void sendsTheFilenameOptionInPlaceOfTheFileName() throws Exception {
    File input = writeInput("example.txt", "a");
    expectUpload(
        "/v2/document",
        "renamed.txt",
        "a".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de"));
    client.translateDocumentUpload(
        input, null, "de", new DocumentTranslationOptions().setFilename("renamed.txt"));
    assertPartNames("/v2/document", "file", "target_lang");
  }

  @Test
  void sendsTheFilenameOptionInPlaceOfTheFileNameArgumentOfAStreamUpload() throws Exception {
    expectUpload(
        "/v2/document",
        "renamed.txt",
        "abc".getBytes(StandardCharsets.UTF_8),
        fields("target_lang", "de"));
    client.translateDocumentUpload(
        new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)),
        "stream.txt",
        null,
        "de",
        new DocumentTranslationOptions().setFilename("renamed.txt"));
    assertPartNames("/v2/document", "file", "target_lang");
  }

  @Test
  void urlEncodesTheDocumentIdInTheStatusPath() throws Exception {
    wm.stubFor(
        post("/v2/document/D%201%2Fx")
            .withRequestBody(FormBody.of("document_key", "K1"))
            .willReturn(okJson(DONE)));
    DocumentStatus status = client.translateDocumentGetStatus(new DocumentHandle("D 1/x", "K1"));
    assertTrue(status.done());
  }

  @Test
  void urlEncodesTheDocumentIdInTheDownloadPath() throws Exception {
    wm.stubFor(
        post("/v2/document/D%201%2Fx/result")
            .withRequestBody(FormBody.of("document_key", "K1"))
            .willReturn(aResponse().withStatus(200).withBody("b")));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    client.translateDocumentDownload(new DocumentHandle("D 1/x", "K1"), output);
    assertEquals("b", new String(output.toByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  void keepsAnUnknownStatusAsOkAndNotDone() throws Exception {
    wm.stubFor(
        post("/v2/document/D1")
            .willReturn(okJson("{\"document_id\":\"D1\",\"status\":\"paused\"}")));
    DocumentStatus status = client.translateDocumentGetStatus(HANDLE_OBJECT);
    assertEquals("paused", status.getStatusString());
    assertNull(status.getStatus());
    assertTrue(status.ok());
    assertFalse(status.done());
  }

  @Test
  void keepsPollingAfterAnUnknownStatus() throws Exception {
    wm.stubFor(
        post("/v2/document/D1")
            .inScenario("unknown")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("done")
            .willReturn(okJson("{\"document_id\":\"D1\",\"status\":\"paused\"}")));
    wm.stubFor(
        post("/v2/document/D1")
            .inScenario("unknown")
            .whenScenarioStateIs("done")
            .willReturn(okJson(DONE)));
    DocumentStatus status = client.translateDocumentWaitUntilDone(HANDLE_OBJECT);
    assertTrue(status.done());
    assertEquals("done", status.getStatusString());
  }

  @Test
  void throwsDeepLExceptionWhenTheWaitTimeoutExpires() {
    wm.stubFor(
        post("/v2/document/D1")
            .willReturn(okJson("{\"document_id\":\"D1\",\"status\":\"queued\"}")));
    DeepLException thrown =
        assertThrows(
            DeepLException.class,
            () -> client.translateDocumentWaitUntilDone(HANDLE_OBJECT, Duration.ofMillis(50)));
    assertTrue(thrown.getMessage().contains("Timed out"));
  }

  @Test
  void throwsDocumentTranslationExceptionWithTheHandleWhenTheTimeoutOptionExpires()
      throws Exception {
    File input = writeInput("example.txt", "a");
    File output = tempDir.resolve("out.txt").toFile();
    wm.stubFor(post("/v2/document").willReturn(okJson(HANDLE)));
    wm.stubFor(
        post("/v2/document/D1")
            .willReturn(okJson("{\"document_id\":\"D1\",\"status\":\"queued\"}")));
    DocumentTranslationException thrown =
        assertThrows(
            DocumentTranslationException.class,
            () ->
                client.translateDocument(
                    input,
                    output,
                    null,
                    "de",
                    new DocumentTranslationOptions().setTimeout(Duration.ofMillis(50))));
    assertTrue(thrown.getMessage().contains("Timed out"));
    assertEquals("D1", thrown.getHandle().getDocumentId());
    assertFalse(output.exists());
  }
}
