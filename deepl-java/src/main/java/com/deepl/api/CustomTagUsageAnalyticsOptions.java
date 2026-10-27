// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import org.jetbrains.annotations.*;

/**
 * Options for {@link DeepLAdminClient#getCustomTagUsageAnalytics(java.time.LocalDate,
 * java.time.LocalDate, CustomTagUsageAnalyticsOptions)}.
 */
public class CustomTagUsageAnalyticsOptions {
  private @Nullable CustomTagUsageAggregateBy aggregateBy;
  private @Nullable Integer page;

  /**
   * Sets how the usage is aggregated.
   *
   * @param aggregateBy Aggregate the usage over the whole period or by day, or <code>null</code>
   *     for the API default (whole period).
   * @return This object, for convenience when chaining setters.
   */
  public CustomTagUsageAnalyticsOptions setAggregateBy(
      @Nullable CustomTagUsageAggregateBy aggregateBy) {
    this.aggregateBy = aggregateBy;
    return this;
  }

  /**
   * Sets the page to retrieve.
   *
   * @param page Page number, as returned by {@link CustomTagUsageReport#getNextPage()} of the
   *     previous page, or <code>null</code> for the first page.
   * @return This object, for convenience when chaining setters.
   */
  public CustomTagUsageAnalyticsOptions setPage(@Nullable Integer page) {
    this.page = page;
    return this;
  }

  /** Gets the current aggregation. */
  public @Nullable CustomTagUsageAggregateBy getAggregateBy() {
    return aggregateBy;
  }

  /** Gets the current page number. */
  public @Nullable Integer getPage() {
    return page;
  }
}
