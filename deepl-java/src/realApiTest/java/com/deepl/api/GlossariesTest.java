// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class GlossariesTest {
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void sendsTheV2GlossaryRequestsCreateGetListEntriesAndDelete() throws Exception {
    Translator translator = RealApiClient.createTranslator();
    String name = RealApiNames.getUniqueName("glossary");
    GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("Apple", "Apfel"));

    GlossaryInfo glossary = translator.createGlossary(name, "en", "de", entries);
    try {
      assertEquals(name, glossary.getName());
      assertEquals("en", glossary.getSourceLang());
      assertEquals("de", glossary.getTargetLang());
      assertEquals(1, glossary.getEntryCount());

      GlossaryInfo fetched =
          RealApiClient.retryWhileGlossaryNotFound(
              () -> translator.getGlossary(glossary.getGlossaryId()));
      assertEquals(glossary.getGlossaryId(), fetched.getGlossaryId());
      assertEquals(name, fetched.getName());
      assertEquals(glossary.getCreationTime(), fetched.getCreationTime());

      List<GlossaryInfo> listed = translator.listGlossaries();
      assertTrue(
          listed.stream().anyMatch(info -> info.getGlossaryId().equals(glossary.getGlossaryId())));

      assertEquals(entries, translator.getGlossaryEntries(glossary.getGlossaryId()));
    } finally {
      try {
        translator.deleteGlossary(glossary.getGlossaryId());
      } catch (GlossaryNotFoundException ignored) {
        // Already gone
      }
    }
  }
}
