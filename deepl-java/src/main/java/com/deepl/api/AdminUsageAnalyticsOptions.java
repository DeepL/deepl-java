// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import org.jetbrains.annotations.*;

/**
 * Options for {@link DeepLAdminClient#getUsageAnalytics(java.time.LocalDate, java.time.LocalDate,
 * AdminUsageAnalyticsOptions)}.
 */
public class AdminUsageAnalyticsOptions {
  private @Nullable AdminUsageGroupBy groupBy;

  /**
   * Sets how the usage is grouped.
   *
   * @param groupBy Group the usage by key or by key and day, or <code>null</code> for totals only.
   * @return This object, for convenience when chaining setters.
   */
  public AdminUsageAnalyticsOptions setGroupBy(@Nullable AdminUsageGroupBy groupBy) {
    this.groupBy = groupBy;
    return this;
  }

  /** Gets the current grouping. */
  public @Nullable AdminUsageGroupBy getGroupBy() {
    return groupBy;
  }
}
