// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class TranslationMemoriesTest {
  // A translation memory exists only after its import job completed, which takes about a minute.
  private static final Duration JOB_TIMEOUT = Duration.ofSeconds(120);

  @TempDir Path tempDir;

  @Test
  @Timeout(value = 300, unit = TimeUnit.SECONDS)
  void sendsTheTranslationMemoryRequestsImportGetListSegmentsExportAndDelete() throws Exception {
    DeepLClient client = RealApiClient.createClient();
    String name = RealApiNames.getUniqueName("translation-memory");
    File tmx = Fixtures.writeFile(tempDir.resolve("example.tmx").toFile(), Fixtures.EXAMPLE_TMX);
    String translationMemoryId = null;
    try {
      TranslationMemoryJob importJob =
          client.importTranslationMemoryFromFilepath(tmx, name, JOB_TIMEOUT);
      translationMemoryId = importJob.getResult().getTranslationMemoryId();
      assertEquals(TranslationMemoryJob.Operation.Import, importJob.getOperation());
      assertEquals(TranslationMemoryJobResult.Status.Completed, importJob.getStatus());
      assertNotNull(translationMemoryId);
      String id = translationMemoryId;

      TranslationMemoryInfo fetched = client.getTranslationMemory(id);
      assertEquals(id, fetched.getTranslationMemoryId());
      assertEquals(name, fetched.getName());
      assertFalse(fetched.getSourceLanguage().isEmpty());

      boolean listed = false;
      for (int page = 0; !listed && page < 20; page++) {
        List<TranslationMemoryInfo> memories = client.listTranslationMemories(page, 100);
        if (memories.isEmpty()) {
          break;
        }
        listed = memories.stream().anyMatch(memory -> memory.getTranslationMemoryId().equals(id));
      }
      assertTrue(listed);

      TranslationMemorySegments segments = client.listTranslationMemorySegments(id);
      assertFalse(segments.getSegments().isEmpty());
      assertNotNull(segments.getSegments().get(0).getSourceSegmentId());

      File exported = tempDir.resolve("export.tmx").toFile();
      TranslationMemoryJob exportJob =
          client.exportTranslationMemoryToFilepath(id, exported, JOB_TIMEOUT);
      assertEquals(TranslationMemoryJob.Operation.Export, exportJob.getOperation());
      assertTrue(Fixtures.readFile(exported).contains("<tmx"));
    } finally {
      if (translationMemoryId != null) {
        try {
          client.deleteTranslationMemory(translationMemoryId);
        } catch (NotFoundException ignored) {
          // Already gone
        }
      }
    }
  }
}
