// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.time.LocalDate;
import java.util.*;
import org.jetbrains.annotations.*;

/**
 * Custom tag usage analytics report for a date range, see {@link
 * DeepLAdminClient#getCustomTagUsageAnalytics}.
 */
public class CustomTagUsageReport {
  @SerializedName(value = "aggregate_by")
  private final @Nullable String aggregateBy;

  @SerializedName(value = "start_date")
  private final @Nullable LocalDate startDate;

  @SerializedName(value = "end_date")
  private final @Nullable LocalDate endDate;

  @SerializedName(value = "next_page")
  private final @Nullable Integer nextPage;

  @SerializedName(value = "usage")
  private final @Nullable List<CustomTagUsage> usage;

  /**
   * Initializes a new {@link CustomTagUsageReport}.
   *
   * @param aggregateBy Aggregation used for the report.
   * @param startDate Start of the report period.
   * @param endDate End of the report period.
   * @param nextPage Page number of the next page, or <code>null</code> if this is the last page.
   * @param usage Usage entries per custom tag.
   */
  public CustomTagUsageReport(
      @Nullable String aggregateBy,
      @Nullable LocalDate startDate,
      @Nullable LocalDate endDate,
      @Nullable Integer nextPage,
      List<CustomTagUsage> usage) {
    this.aggregateBy = aggregateBy;
    this.startDate = startDate;
    this.endDate = endDate;
    this.nextPage = nextPage;
    this.usage = usage;
  }

  /**
   * @return Aggregation used for the report as the API returns it, for example "period", or <code>
   *     null</code>.
   */
  public @Nullable String getAggregateBy() {
    return aggregateBy;
  }

  /** @return Start of the report period. */
  public @Nullable LocalDate getStartDate() {
    return startDate;
  }

  /** @return End of the report period. */
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  /** @return Page number of the next page, or <code>null</code> if this is the last page. */
  public @Nullable Integer getNextPage() {
    return nextPage;
  }

  /** @return Usage entries per custom tag. */
  public List<CustomTagUsage> getUsage() {
    return usage != null ? usage : Collections.emptyList();
  }
}
