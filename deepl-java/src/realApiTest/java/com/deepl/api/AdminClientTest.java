// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AdminClientTest {
  private final DeepLAdminClient client = RealApiClient.createAdminClient();
  private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

  @Test
  void getsTheUsageAnalytics() throws Exception {
    AdminUsageReport report =
        client.getUsageAnalytics(
            today.minusDays(7),
            today,
            new AdminUsageAnalyticsOptions().setGroupBy(AdminUsageGroupBy.KeyAndDay));
    assertNotNull(report.getTotalUsage());
    assertNotNull(report.getStartDate());
    assertNotNull(report.getEndDate());
    assertEquals("keyAndDay", report.getGroupBy());
  }

  @Test
  void getsTheCustomTagUsageAnalytics() throws Exception {
    CustomTagUsageReport report =
        client.getCustomTagUsageAnalytics(
            today.minusDays(7),
            today,
            new CustomTagUsageAnalyticsOptions().setAggregateBy(CustomTagUsageAggregateBy.Day));
    assertEquals("day", report.getAggregateBy());
  }

  @Test
  void createsListsAndDeactivatesADeveloperKey() throws Exception {
    String label = RealApiNames.getUniqueName("admin-key");
    DeveloperKey key = client.createDeveloperKey(label);
    boolean deactivated = false;
    try {
      assertEquals(label, key.getLabel());
      assertTrue(isListedEventually(key), "The new developer key is not listed after 5 attempts");

      String renamedLabel = RealApiNames.getUniqueName("admin-key-renamed");
      assertEquals(renamedLabel, client.renameDeveloperKey(key, renamedLabel).getLabel());

      DeveloperKeyUsageLimits limits =
          client
              .setDeveloperKeyUsageLimits(
                  key,
                  new DeveloperKeyUsageLimitsUpdate()
                      .setCharacters(5000L)
                      .setSpeechToTextMilliseconds(null))
              .getUsageLimits();
      assertEquals(5000L, limits.getCharacters());
      assertNull(limits.getSpeechToTextMilliseconds());

      assertTrue(client.deactivateDeveloperKey(key).isDeactivated());
      deactivated = true;
    } finally {
      if (!deactivated) {
        deactivateIgnoringNotFound(key);
      }
    }
    assertTrue(
        client.getDeveloperKeys().stream()
            .anyMatch(
                listed -> listed.getKeyId().equals(key.getKeyId()) && listed.isDeactivated()));
  }

  private boolean isListedEventually(DeveloperKey key) throws Exception {
    for (int attempt = 1; attempt <= 5; attempt++) {
      if (client.getDeveloperKeys().stream()
          .anyMatch(listed -> listed.getKeyId().equals(key.getKeyId()))) {
        return true;
      }
      Thread.sleep(1000);
    }
    return false;
  }

  private void deactivateIgnoringNotFound(DeveloperKey key) {
    try {
      client.deactivateDeveloperKey(key);
    } catch (NotFoundException ignored) {
    } catch (Exception e) {
      System.err.println("Failed to deactivate developer key " + key.getKeyId() + ": " + e);
    }
  }
}
