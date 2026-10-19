// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class TranslateDocumentTest {
  @TempDir Path tempDir;

  @Test
  @Timeout(value = 180, unit = TimeUnit.SECONDS)
  void uploadsPollsAndDownloadsATxtDocument() throws Exception {
    File input = Fixtures.writeFile(tempDir.resolve("example.txt").toFile(), Fixtures.EXAMPLE_TEXT);
    File output = tempDir.resolve("output.txt").toFile();

    DocumentStatus status =
        RealApiClient.createTranslator().translateDocument(input, output, null, "de");

    assertTrue(status.ok() && status.done());
    assertEquals(Fixtures.EXAMPLE_TEXT.length(), status.getBilledCharacters());
    assertTrue(output.length() > 0);
  }
}
