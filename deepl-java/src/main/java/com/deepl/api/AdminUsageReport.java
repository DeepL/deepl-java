// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.util.*;
import org.jetbrains.annotations.*;

/** Usage analytics report for a date range, see {@link DeepLAdminClient#getUsageAnalytics}. */
public class AdminUsageReport {
  @SerializedName(value = "total_usage")
  private final @Nullable AdminUsageBreakdown totalUsage;

  @SerializedName(value = "start_date")
  private final @Nullable Date startDate;

  @SerializedName(value = "end_date")
  private final @Nullable Date endDate;

  @SerializedName(value = "group_by")
  private final @Nullable String groupBy;

  @SerializedName(value = "key_usages")
  private final @Nullable List<KeyUsage> keyUsages;

  @SerializedName(value = "key_and_day_usages")
  private final @Nullable List<KeyUsage> keyAndDayUsages;

  /**
   * Initializes a new {@link AdminUsageReport}.
   *
   * @param totalUsage Usage totals over the whole date range, or <code>null</code> if the report
   *     does not include them.
   * @param startDate Start day of the report period, as midnight UTC.
   * @param endDate End day of the report period, as midnight UTC.
   * @param groupBy Grouping used for the report, if any.
   * @param keyUsages Usage per key, present when grouped by key.
   * @param keyAndDayUsages Usage per key and day, present when grouped by key and day.
   */
  public AdminUsageReport(
      @Nullable AdminUsageBreakdown totalUsage,
      @Nullable Date startDate,
      @Nullable Date endDate,
      @Nullable String groupBy,
      @Nullable List<KeyUsage> keyUsages,
      @Nullable List<KeyUsage> keyAndDayUsages) {
    this.totalUsage = totalUsage;
    this.startDate = startDate;
    this.endDate = endDate;
    this.groupBy = groupBy;
    this.keyUsages = keyUsages;
    this.keyAndDayUsages = keyAndDayUsages;
  }

  /**
   * @return Usage totals over the whole date range, or <code>null</code> if the report does not
   *     include them.
   */
  public @Nullable AdminUsageBreakdown getTotalUsage() {
    return totalUsage;
  }

  /** @return Start day of the report period, as midnight UTC of that day. */
  public @Nullable Date getStartDate() {
    return startDate;
  }

  /** @return End day of the report period, as midnight UTC of that day. */
  public @Nullable Date getEndDate() {
    return endDate;
  }

  /**
   * @return Grouping used for the report as the API returns it, for example "key", or <code>null
   *     </code>.
   */
  public @Nullable String getGroupBy() {
    return groupBy;
  }

  /** @return Usage per key, or an empty list if the report is not grouped by key. */
  public List<KeyUsage> getKeyUsages() {
    return keyUsages != null ? keyUsages : Collections.emptyList();
  }

  /**
   * @return Usage per key and day, or an empty list if the report is not grouped by key and day.
   */
  public List<KeyUsage> getKeyAndDayUsages() {
    return keyAndDayUsages != null ? keyAndDayUsages : Collections.emptyList();
  }
}
