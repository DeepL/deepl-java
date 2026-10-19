// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class TranslationMemoriesTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String MEMORY =
      "{\"translation_memory_id\":\"tm1\",\"name\":\"n\",\"source_language\":\"de\","
          + "\"target_languages\":[\"en\",\"fr\"],\"segment_count\":7,"
          + "\"creation_time\":\"2021-08-03T14:16:18.329Z\","
          + "\"updated_time\":\"2021-08-03T14:16:18.329Z\"}";
  private static final String SEGMENTS =
      "{\"segment_count\":12,\"next_page_cursor\":\"c2\",\"segments\":["
          + "{\"source_segment_id\":\"a\",\"source_text\":\"Hallo\","
          + "\"creation_time\":\"2021-08-03T14:16:18.329Z\",\"targets\":["
          + "{\"target_segment_id\":\"b\",\"target_language\":\"en\",\"target_text\":\"Hello\","
          + "\"last_used_time\":\"2021-08-03T14:16:18.329Z\"}]}]}";

  private static String job(String operation, String status, String extra) {
    return "{\"job_id\":\"j1\",\"product\":\"translation_memory\",\"operation\":\""
        + operation
        + "\",\"creation_time\":\"2021-08-03T14:16:18.329Z\",\"results\":[{\"status\":\""
        + status
        + "\""
        + extra
        + "}]}";
  }

  @TempDir Path tempDir;

  private final DeepLClient client = OfflineClient.createClient(wm);

  private String storageUrl() {
    return wm.baseUrl() + "/storage";
  }

  @Test
  void sendsPageParametersWhenListingTranslationMemories() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/translation_memories?page=0&page_size=10"))
            .withHeader("Content-Type", absent())
            .willReturn(okJson("{\"translation_memories\":[" + MEMORY + "]}")));
    client.listTranslationMemories(0, 10);
  }

  @Test
  void sendsNoQueryWhenListingTranslationMemoriesWithoutParameters() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/translation_memories"))
            .willReturn(okJson("{\"translation_memories\":[]}")));
    client.listTranslationMemories();
  }

  @Test
  void parsesTranslationMemoryInfo() throws Exception {
    wm.stubFor(get("/v3/translation_memories/tm1").willReturn(okJson(MEMORY)));
    TranslationMemoryInfo memory = client.getTranslationMemory("tm1");
    assertEquals("tm1", memory.getTranslationMemoryId());
    assertEquals("de", memory.getSourceLanguage());
    assertEquals(
        Collections.unmodifiableList(java.util.Arrays.asList("en", "fr")),
        memory.getTargetLanguages());
    assertEquals(7, memory.getSegmentCount());
    assertEquals(1628000178329L, memory.getCreationTime().getTime());
    assertEquals(1628000178329L, memory.getUpdatedTime().getTime());
  }

  @Test
  void sendsTheIdOfATranslationMemoryObject() throws Exception {
    TranslationMemoryInfo memory =
        new TranslationMemoryInfo("tm2", "n", "de", Collections.singletonList("en"), 1);
    wm.stubFor(get("/v3/translation_memories/tm2").willReturn(okJson(MEMORY)));
    wm.stubFor(get("/v3/translation_memories/tm2/segments").willReturn(okJson(SEGMENTS)));
    wm.stubFor(delete("/v3/translation_memories/tm2").willReturn(aResponse().withStatus(204)));
    client.getTranslationMemory(memory);
    client.listTranslationMemorySegments(memory);
    client.listTranslationMemorySegments(memory, new TranslationMemorySegmentsOptions());
    client.deleteTranslationMemory(memory);
  }

  @Test
  void parsesTheListOfTranslationMemories() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/translation_memories"))
            .willReturn(okJson("{\"translation_memories\":[" + MEMORY + "," + MEMORY + "]}")));
    List<TranslationMemoryInfo> memories = client.listTranslationMemories();
    assertEquals(2, memories.size());
  }

  @Test
  void throwsNotFoundWhenTheTranslationMemoryIsUnknown() {
    wm.stubFor(get("/v3/translation_memories/tm1").willReturn(aResponse().withStatus(404)));
    assertThrows(NotFoundException.class, () -> client.getTranslationMemory("tm1"));
  }

  @Test
  void sendsEverySegmentsOptionAsEncodedQuery() throws Exception {
    wm.stubFor(
        get(urlEqualTo(
                "/v3/translation_memories/tm1/segments?page_size=5&page_cursor=c1"
                    + "&filter_text=Nummer%207%26&filter_case_sensitive=true"))
            .withHeader("Content-Type", absent())
            .willReturn(okJson(SEGMENTS)));
    client.listTranslationMemorySegments(
        "tm1",
        new TranslationMemorySegmentsOptions()
            .setPageSize(5)
            .setPageCursor("c1")
            .setFilterText("Nummer 7&")
            .setFilterCaseSensitive(true));
  }

  @Test
  void sendsNoQueryWhenListingSegmentsWithoutOptions() throws Exception {
    wm.stubFor(
        get(urlEqualTo("/v3/translation_memories/tm1/segments")).willReturn(okJson(SEGMENTS)));
    client.listTranslationMemorySegments("tm1");
  }

  @Test
  void parsesSegmentsWithTheirTargets() throws Exception {
    wm.stubFor(get("/v3/translation_memories/tm1/segments").willReturn(okJson(SEGMENTS)));
    TranslationMemorySegments page = client.listTranslationMemorySegments("tm1");
    assertEquals(12, page.getSegmentCount());
    assertEquals("c2", page.getNextPageCursor());
    TranslationMemorySegment segment = page.getSegments().get(0);
    assertEquals("a", segment.getSourceSegmentId());
    assertEquals("Hallo", segment.getSourceText());
    assertEquals(1628000178329L, segment.getCreationTime().getTime());
    assertNull(segment.getUpdatedTime());
    TranslationMemoryTargetSegment target = segment.getTargets().get(0);
    assertEquals("b", target.getTargetSegmentId());
    assertEquals("en", target.getTargetLanguage());
    assertEquals("Hello", target.getTargetText());
    assertEquals(1628000178329L, target.getLastUsedTime().getTime());
  }

  @Test
  void sendsADeleteRequestWhenDeletingATranslationMemory() throws Exception {
    wm.stubFor(delete("/v3/translation_memories/tm1").willReturn(aResponse().withStatus(204)));
    client.deleteTranslationMemory("tm1");
  }

  @Test
  void sendsFileNameLengthAndDisplayNameAsJsonWhenCreatingAnImport() throws Exception {
    wm.stubFor(
        post("/v3/translation_memories/import")
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(
                equalToJson(
                    "{\"source_file\":{\"file_name\":\"example.tmx\",\"content_length\":1024,"
                        + "\"content_type\":\"application/xml\"},"
                        + "\"parameters\":{\"display_name\":\"Imported TM\"}}"))
            .willReturn(
                okJson(
                    "{\"job_id\":\"j1\",\"upload_url\":\"https://storage.example/u\","
                        + "\"expires_at\":\"2021-08-03T14:16:18.329Z\"}")));
    TranslationMemoryImport created =
        client.createTranslationMemoryImport("example.tmx", 1024, "application/xml", "Imported TM");
    assertEquals("j1", created.getJobId());
    assertEquals("https://storage.example/u", created.getUploadUrl());
    assertEquals(1628000178329L, created.getExpiresAt().getTime());
  }

  @Test
  void omitsContentTypeAndParametersWhenTheyAreNull() throws Exception {
    wm.stubFor(
        post("/v3/translation_memories/import")
            .withRequestBody(
                equalToJson(
                    "{\"source_file\":{\"file_name\":\"example.tmx\",\"content_length\":1}}"))
            .willReturn(okJson("{\"job_id\":\"j1\",\"upload_url\":\"u\"}")));
    client.createTranslationMemoryImport("example.tmx", 1, null, null);
  }

  @Test
  void throwsIllegalArgumentWhenTheImportFileIsInvalid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> client.createTranslationMemoryImport("", 100, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.createTranslationMemoryImport("example.tmx", 0, null, null));
  }

  @Test
  void uploadsTheFileBytesWithoutDeepLHeaders() throws Exception {
    byte[] content = new byte[] {0x3c, (byte) 0xff, 0x00, (byte) 0x80, 0x3e};
    wm.stubFor(
        put("/storage")
            .withHeader("Content-Type", equalTo("application/xml"))
            .withHeader("Authorization", absent())
            .withHeader("User-Agent", notMatching("deepl-java.*"))
            .withRequestBody(binaryEqualTo(content))
            .willReturn(aResponse().withStatus(200)));
    client.uploadTranslationMemoryFile(storageUrl(), content, "application/xml");
  }

  @Test
  void uploadsWithTheDefaultContentTypeForAnImport() throws Exception {
    wm.stubFor(
        put("/storage")
            .withHeader("Content-Type", equalTo(DeepLClient.TRANSLATION_MEMORY_FILE_CONTENT_TYPE))
            .withHeader("Authorization", absent())
            .withRequestBody(binaryEqualTo("<tmx/>".getBytes(StandardCharsets.UTF_8)))
            .willReturn(aResponse().withStatus(200)));
    client.uploadTranslationMemoryFile(
        new TranslationMemoryImport("j1", storageUrl(), null),
        "<tmx/>".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void throwsDeepLExceptionWhenTheUploadIsRejected() {
    wm.stubFor(put("/storage").willReturn(aResponse().withStatus(403)));
    DeepLException thrown =
        assertThrows(
            DeepLException.class,
            () ->
                client.uploadTranslationMemoryFile(
                    storageUrl(), new byte[] {1}, "application/xml"));
    assertTrue(thrown.getMessage().contains("HTTP status: 403"));
  }

  @Test
  void throwsIllegalArgumentWhenAnObjectArgumentIsNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> client.getTranslationMemory((TranslationMemoryInfo) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.uploadTranslationMemoryFile((TranslationMemoryImport) null, new byte[] {1}));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.downloadTranslationMemoryExport(null, new ByteArrayOutputStream()));
  }

  @Test
  void throwsIllegalArgumentWhenTheUploadArgumentsAreInvalid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> client.uploadTranslationMemoryFile("", new byte[] {1}, "application/xml"));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.uploadTranslationMemoryFile("http://x", new byte[] {1}, ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.uploadTranslationMemoryFile("http://x", null, "application/xml"));
  }

  @Test
  void parsesAnImportJobAwaitingInput() throws Exception {
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .withHeader("Content-Type", absent())
            .willReturn(
                okJson(
                    "{\"job_id\":\"j1\",\"product\":\"translation_memory\",\"operation\":\"import\","
                        + "\"creation_time\":\"2021-08-03T14:16:18.329Z\","
                        + "\"parameters\":{\"display_name\":\"Imported TM\"},"
                        + "\"source_file\":{\"content_type\":\"application/xml\","
                        + "\"content_length\":1024},\"results\":[{\"status\":\"awaiting_input\","
                        + "\"status_metadata\":{\"required_action\":\"upload_file\"}}]}")));
    TranslationMemoryJob job = client.getTranslationMemoryJob("j1");
    assertEquals(TranslationMemoryJob.Operation.Import, job.getOperation());
    assertEquals("Imported TM", job.getDisplayName());
    assertEquals("application/xml", job.getSourceContentType());
    assertEquals(1024L, job.getSourceContentLength());
    assertEquals(TranslationMemoryJobResult.Status.AwaitingInput, job.getStatus());
    assertEquals("upload_file", job.getResult().getRequiredAction());
    assertFalse(job.done());
    assertTrue(job.ok());
  }

  @Test
  void parsesACompletedExportJob() throws Exception {
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .willReturn(
                okJson(
                    "{\"job_id\":\"j1\",\"operation\":\"export\","
                        + "\"parameters\":{\"translation_memory_id\":\"tm1\"},"
                        + "\"results\":[{\"status\":\"completed\","
                        + "\"download_url\":\"https://storage.example/d\","
                        + "\"expires_at\":\"2021-08-03T14:16:18.329Z\"}]}")));
    TranslationMemoryJob job = client.getTranslationMemoryJob("j1");
    assertEquals(TranslationMemoryJob.Operation.Export, job.getOperation());
    assertEquals("tm1", job.getTranslationMemoryId());
    assertEquals("https://storage.example/d", job.getResult().getDownloadUrl());
    assertEquals(1628000178329L, job.getResult().getExpiresAt().getTime());
    assertTrue(job.done());
  }

  @Test
  void parsesAFailedJobWithItsErrorMessage() throws Exception {
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .willReturn(
                okJson(
                    job(
                        "import",
                        "failed",
                        ",\"error\":{\"message\":\"bad tmx\"},\"skipped_segment_count\":4"))));
    TranslationMemoryJob job = client.getTranslationMemoryJob("j1");
    assertEquals("bad tmx", job.getResult().getErrorMessage());
    assertEquals(4, job.getResult().getSkippedSegmentCount());
    assertTrue(job.done());
    assertFalse(job.ok());
  }

  @Test
  void throwsNotFoundWhenTheJobIsUnknown() {
    wm.stubFor(get("/v3/translation_memories/jobs/j1").willReturn(aResponse().withStatus(404)));
    assertThrows(NotFoundException.class, () -> client.getTranslationMemoryJob("j1"));
  }

  @Test
  void pollsThroughAwaitingInputUntilTheJobCompletes() throws Exception {
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .inScenario("job")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("processing")
            .willReturn(okJson(job("import", "awaiting_input", ""))));
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .inScenario("job")
            .whenScenarioStateIs("processing")
            .willReturn(okJson(job("import", "completed", ",\"translation_memory_id\":\"tm9\""))));
    TranslationMemoryJob finished = client.waitUntilTranslationMemoryJobDone("j1");
    assertEquals("tm9", finished.getResult().getTranslationMemoryId());
  }

  @Test
  void throwsDeepLExceptionWithTheErrorMessageWhenTheJobFails() {
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .willReturn(okJson(job("import", "failed", ",\"error\":{\"message\":\"bad tmx\"}"))));
    DeepLException thrown =
        assertThrows(DeepLException.class, () -> client.waitUntilTranslationMemoryJobDone("j1"));
    assertEquals("bad tmx", thrown.getMessage());
  }

  @Test
  void throwsDeepLExceptionWhenTheJobHasNoResults() {
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .willReturn(okJson("{\"job_id\":\"j1\",\"results\":[]}")));
    DeepLException thrown =
        assertThrows(DeepLException.class, () -> client.waitUntilTranslationMemoryJobDone("j1"));
    assertTrue(thrown.getMessage().contains("returned no results"));
  }

  @Test
  void throwsDeepLExceptionWhenTheManualTimeoutIsExceeded() {
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .willReturn(okJson(job("import", "awaiting_input", ""))));
    DeepLException thrown =
        assertThrows(
            DeepLException.class,
            () -> client.waitUntilTranslationMemoryJobDone("j1", Duration.ofSeconds(1)));
    assertTrue(thrown.getMessage().contains("Manual timeout of 1s exceeded"));
  }

  @Test
  void sendsAnEmptyPostWhenCreatingAnExportAndParsesANewJob() throws Exception {
    wm.stubFor(
        post("/v3/translation_memories/tm1/export")
            .withRequestBody(absent())
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"job_id\":\"j1\",\"parameters\":{\"translation_memory_id\":\"tm1\"}}")));
    TranslationMemoryExport created = client.createTranslationMemoryExport("tm1");
    assertEquals("j1", created.getJobId());
    assertEquals("tm1", created.getTranslationMemoryId());
    assertFalse(created.isReusedExisting());
  }

  @Test
  void parsesAnExistingExportJobAsReused() throws Exception {
    wm.stubFor(
        post("/v3/translation_memories/tm1/export")
            .willReturn(
                okJson("{\"job_id\":\"j2\",\"parameters\":{\"translation_memory_id\":\"tm1\"}}")));
    TranslationMemoryExport reused = client.createTranslationMemoryExport("tm1");
    assertTrue(reused.isReusedExisting());
  }

  @Test
  void sendsTheIdOfATranslationMemoryObjectWhenCreatingAnExport() throws Exception {
    wm.stubFor(
        post("/v3/translation_memories/tm2/export")
            .willReturn(
                okJson("{\"job_id\":\"j2\",\"parameters\":{\"translation_memory_id\":\"tm2\"}}")));
    client.createTranslationMemoryExport(
        new TranslationMemoryInfo("tm2", "n", "de", Collections.singletonList("en"), 1));
  }

  @Test
  void downloadsTheExportToAFileWithoutDeepLHeaders() throws Exception {
    wm.stubFor(
        get("/storage/export")
            .withHeader("Authorization", absent())
            .withHeader("User-Agent", notMatching("deepl-java.*"))
            .willReturn(aResponse().withStatus(200).withBody("<tmx/>")));
    File output = tempDir.resolve("export.tmx").toFile();
    client.downloadTranslationMemoryExport(completedExport(), output);
    assertEquals("<tmx/>", Fixtures.readFile(output));
  }

  @Test
  void downloadsTheExportToAStream() throws Exception {
    byte[] bytes = new byte[] {0x00, (byte) 0xff, (byte) 0x80};
    wm.stubFor(get("/storage/export").willReturn(aResponse().withStatus(200).withBody(bytes)));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    client.downloadTranslationMemoryExport(completedExport(), output);
    assertEquals(3, output.size());
    assertEquals((byte) 0xff, output.toByteArray()[1]);
  }

  @Test
  void leavesNoFileBehindWhenTheDownloadFails() throws Exception {
    wm.stubFor(get("/storage/export").willReturn(aResponse().withStatus(500)));
    File output = tempDir.resolve("export.tmx").toFile();
    DeepLException thrown =
        assertThrows(
            DeepLException.class,
            () -> client.downloadTranslationMemoryExport(completedExport(), output));
    assertTrue(thrown.getMessage().contains("HTTP status: 500"));
    assertFalse(output.exists());
  }

  @Test
  void keepsAnExistingFileWhenTheOutputPathIsOccupied() throws Exception {
    File output = Fixtures.writeFile(tempDir.resolve("export.tmx").toFile(), "mine");
    assertThrows(
        java.io.IOException.class,
        () -> client.downloadTranslationMemoryExport(completedExport(), output));
    assertEquals("mine", Fixtures.readFile(output));
  }

  @Test
  void throwsDeepLExceptionWhenTheJobHasNoDownloadUrl() throws Exception {
    TranslationMemoryJob pending = pendingExport();
    DeepLException thrown =
        assertThrows(
            DeepLException.class,
            () -> client.downloadTranslationMemoryExport(pending, new ByteArrayOutputStream()));
    assertTrue(thrown.getMessage().contains("no download URL"));
  }

  @Test
  void importsAFileByCreatingUploadingAndWaiting() throws Exception {
    File tmx = Fixtures.writeFile(tempDir.resolve("example.tmx").toFile(), Fixtures.EXAMPLE_TMX);
    int length = Fixtures.EXAMPLE_TMX.getBytes(StandardCharsets.UTF_8).length;
    wm.stubFor(
        post("/v3/translation_memories/import")
            .withRequestBody(
                equalToJson(
                    "{\"source_file\":{\"file_name\":\"example.tmx\",\"content_length\":"
                        + length
                        + "},\"parameters\":{\"display_name\":\"Imported TM\"}}"))
            .willReturn(okJson("{\"job_id\":\"j1\",\"upload_url\":\"" + storageUrl() + "\"}")));
    wm.stubFor(
        put("/storage")
            .withHeader("Content-Type", equalTo("application/xml"))
            .withRequestBody(binaryEqualTo(Fixtures.EXAMPLE_TMX.getBytes(StandardCharsets.UTF_8)))
            .willReturn(aResponse().withStatus(200)));
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .willReturn(okJson(job("import", "completed", ",\"translation_memory_id\":\"tm9\""))));
    TranslationMemoryJob finished = client.importTranslationMemoryFromFilepath(tmx, "Imported TM");
    assertEquals("tm9", finished.getResult().getTranslationMemoryId());
  }

  @Test
  void throwsIllegalArgumentWhenTheImportFileDoesNotExist() {
    assertThrows(
        IllegalArgumentException.class,
        () -> client.importTranslationMemoryFromFilepath(tempDir.resolve("missing.tmx").toFile()));
  }

  @Test
  void exportsATranslationMemoryByCreatingWaitingAndDownloading() throws Exception {
    wm.stubFor(
        post("/v3/translation_memories/tm1/export")
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"job_id\":\"j1\",\"parameters\":{\"translation_memory_id\":\"tm1\"}}")));
    wm.stubFor(
        get("/v3/translation_memories/jobs/j1")
            .willReturn(
                okJson(
                    job(
                        "export",
                        "completed",
                        ",\"download_url\":\"" + wm.baseUrl() + "/storage/export\""))));
    wm.stubFor(
        get("/storage/export")
            .withHeader("Authorization", absent())
            .willReturn(aResponse().withStatus(200).withBody("<tmx/>")));
    File output = tempDir.resolve("export.tmx").toFile();
    client.exportTranslationMemoryToFilepath("tm1", output);
    assertEquals("<tmx/>", Fixtures.readFile(output));
  }

  private TranslationMemoryJob completedExport() {
    return exportJob(TranslationMemoryJobResult.Status.Completed, wm.baseUrl() + "/storage/export");
  }

  private TranslationMemoryJob pendingExport() {
    return exportJob(TranslationMemoryJobResult.Status.Processing, null);
  }

  private static TranslationMemoryJob exportJob(
      TranslationMemoryJobResult.Status status, String downloadUrl) {
    return new TranslationMemoryJob(
        "j1",
        "translation_memory",
        TranslationMemoryJob.Operation.Export,
        Collections.singletonList(
            new TranslationMemoryJobResult(status, null, downloadUrl, null, null, null, null)),
        null,
        null,
        "tm1",
        null,
        null,
        null);
  }
}
