// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class MultilingualGlossariesTest {
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void sendsTheV3GlossaryRequestsCreateGetListEntriesRenameReplaceAndDelete() throws Exception {
    DeepLClient client = RealApiClient.createClient();
    String name = RealApiNames.getUniqueName("glossary-v3");
    GlossaryEntries appleEnDe = new GlossaryEntries(Collections.singletonMap("Apple", "Apfel"));
    GlossaryEntries appleEnFr = new GlossaryEntries(Collections.singletonMap("Apple", "Pomme"));

    MultilingualGlossaryInfo glossary =
        client.createMultilingualGlossary(
            name,
            Arrays.asList(
                new MultilingualGlossaryDictionaryEntries("en", "de", appleEnDe),
                new MultilingualGlossaryDictionaryEntries("en", "fr", appleEnFr)));
    try {
      assertEquals(name, glossary.getName());
      assertEquals(2, glossary.getDictionaries().size());

      MultilingualGlossaryInfo fetched =
          RealApiClient.retryWhileGlossaryNotFound(
              () -> client.getMultilingualGlossary(glossary.getGlossaryId()));
      assertEquals(glossary.getGlossaryId(), fetched.getGlossaryId());
      assertEquals(name, fetched.getName());

      List<MultilingualGlossaryInfo> listed = client.listMultilingualGlossaries();
      assertTrue(
          listed.stream().anyMatch(info -> info.getGlossaryId().equals(glossary.getGlossaryId())));

      assertEquals(
          appleEnDe,
          client.getMultilingualGlossaryDictionaryEntries(glossary, "en", "de").getEntries());

      MultilingualGlossaryInfo renamed =
          client.updateMultilingualGlossaryName(glossary.getGlossaryId(), name + "-renamed");
      assertEquals(name + "-renamed", renamed.getName());

      MultilingualGlossaryDictionaryInfo replaced =
          client.replaceMultilingualGlossaryDictionary(
              glossary,
              "en",
              "de",
              new GlossaryEntries(Collections.singletonMap("Cherry", "Kirsche")));
      assertEquals("en", replaced.getSourceLanguageCode());
      assertEquals("de", replaced.getTargetLanguageCode());
      assertEquals(1, replaced.getEntryCount());

      client.deleteMultilingualGlossaryDictionary(glossary, "en", "fr");
    } finally {
      try {
        client.deleteMultilingualGlossary(glossary.getGlossaryId());
      } catch (GlossaryNotFoundException ignored) {
        // Already gone
      }
    }
  }
}
