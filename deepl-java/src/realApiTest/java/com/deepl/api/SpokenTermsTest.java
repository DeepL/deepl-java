// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class SpokenTermsTest {
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void sendsTheSpokenTermsRequestsCreateGetListUpdateEntriesReplaceAndDelete() throws Exception {
    DeepLClient client = RealApiClient.createClient();
    String name = RealApiNames.getUniqueName("spokenTerms");

    SpokenTerms created =
        client.createSpokenTerms(
            name,
            Collections.singletonList(
                new SpokenTermsEntries("en", Arrays.asList("DeepL", "webhook"))));
    String id = created.getSpokenTermsId();
    try {
      assertEquals(name, created.getName());
      assertTermLists(created, "en", 2);

      SpokenTerms fetched = RealApiClient.retryWhileNotFound(() -> client.getSpokenTerms(id));
      assertEquals(id, fetched.getSpokenTermsId());
      assertEquals(name, fetched.getName());
      assertTrue(
          client.listSpokenTerms().stream().anyMatch(st -> st.getSpokenTermsId().equals(id)));

      SpokenTerms updated =
          client.updateSpokenTerms(
              id,
              null,
              Collections.singletonList(
                  new SpokenTermsEntries("en", Collections.singletonList("authentication"))));
      assertTermLists(updated, "en", 3);
      assertEquals(
          new HashSet<>(Arrays.asList("DeepL", "authentication", "webhook")),
          new HashSet<>(client.getSpokenTermsEntries(id, "en").getEntries()));

      SpokenTermsList replaced =
          client.replaceSpokenTermsList(
              fetched, new SpokenTermsEntries("en", Collections.singletonList("API")));
      assertEquals("en", replaced.getLang());
      assertEquals(1, replaced.getEntryCount());
      assertEquals(
          Collections.singletonList("API"), client.getSpokenTermsEntries(id, "en").getEntries());

      client.replaceSpokenTermsList(
          id, new SpokenTermsEntries("de", Collections.singletonList("Schnittstelle")));
      assertEquals(2, client.getSpokenTerms(id).getTermLists().size());
      client.deleteSpokenTermsList(id, "de");
      assertTermLists(client.getSpokenTerms(id), "en", 1);

      client.deleteSpokenTerms(id);
      assertThrows(NotFoundException.class, () -> client.getSpokenTerms(id));
    } finally {
      try {
        client.deleteSpokenTerms(id);
      } catch (NotFoundException ignored) {
        // Already gone
      }
    }
  }

  private static void assertTermLists(SpokenTerms spokenTerms, String lang, long entryCount) {
    List<SpokenTermsList> termLists = spokenTerms.getTermLists();
    assertEquals(1, termLists.size(), termLists.toString());
    assertEquals(lang, termLists.get(0).getLang());
    assertEquals(entryCount, termLists.get(0).getEntryCount());
  }
}
