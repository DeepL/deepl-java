// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class StyleProfilesTest {
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void sendsTheStyleProfileRequestsCreateGetListUpdateAndDelete() throws Exception {
    DeepLClient client = RealApiClient.createClient();
    String styleId =
        client
            .createStyleRule(RealApiNames.getUniqueName("styleProfileRule"), "de", null, null)
            .getStyleId();
    String profileId = null;
    try {
      String name = RealApiNames.getUniqueName("styleProfile");
      StyleProfile created =
          client.createStyleProfile(
              name,
              new CreateStyleProfileOptions()
                  .setDescription("Description")
                  .setStyleRuleIds(Collections.singletonList(styleId)));
      profileId = created.getStyleProfileId();
      String id = profileId;
      assertEquals(name, created.getName());
      assertEquals("Description", created.getDescription());
      assertEquals(Collections.singletonList(styleId), created.getStyleRuleIds());
      assertTrue(created.getGlossaryIds().isEmpty());
      assertTrue(created.getTranslationMemoryIds().isEmpty());

      StyleProfile fetched = RealApiClient.retryWhileNotFound(() -> client.getStyleProfile(id));
      assertEquals(id, fetched.getStyleProfileId());
      assertEquals(name, fetched.getName());
      assertEquals("Description", fetched.getDescription());
      assertEquals(Collections.singletonList(styleId), fetched.getStyleRuleIds());

      boolean listed = false;
      for (int page = 0; !listed && page < 20; page++) {
        List<StyleProfile> profiles = client.listStyleProfiles(page, 100);
        if (profiles.isEmpty()) {
          break;
        }
        listed = profiles.stream().anyMatch(p -> p.getStyleProfileId().equals(id));
      }
      assertTrue(listed);

      StyleProfile updated =
          client.updateStyleProfile(
              fetched,
              new UpdateStyleProfileOptions()
                  .setName(name + "-renamed")
                  .setIndustry("Legal")
                  .setDescription("")
                  .setStyleRuleIds(Collections.emptyList()));
      assertEquals(name + "-renamed", updated.getName());
      assertEquals("Legal", updated.getIndustry());
      assertNull(updated.getDescription());
      assertTrue(updated.getStyleRuleIds().isEmpty());

      client.deleteStyleProfile(id);
      assertThrows(NotFoundException.class, () -> client.getStyleProfile(id));
    } finally {
      if (profileId != null) {
        try {
          client.deleteStyleProfile(profileId);
        } catch (NotFoundException ignored) {
          // Already gone
        }
      }
      try {
        client.deleteStyleRule(styleId);
      } catch (NotFoundException ignored) {
        // Already gone
      }
    }
  }
}
