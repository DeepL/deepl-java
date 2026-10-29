// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.util.*;
import org.jetbrains.annotations.*;

/** Usage of a single custom tag, see {@link CustomTagUsageReport}. */
public class CustomTagUsage {
  @SerializedName(value = "custom_tag")
  private final String customTag;

  @SerializedName(value = "breakdown")
  private final CustomTagUsageBreakdown breakdown;

  @SerializedName(value = "usage_date")
  private final @Nullable Date usageDate;

  /**
   * Initializes a new {@link CustomTagUsage}.
   *
   * @param customTag The custom tag.
   * @param breakdown Usage of the custom tag.
   * @param usageDate Day of the usage as midnight UTC, present when aggregated by day.
   */
  public CustomTagUsage(
      String customTag, CustomTagUsageBreakdown breakdown, @Nullable Date usageDate) {
    this.customTag = customTag;
    this.breakdown = breakdown;
    this.usageDate = usageDate;
  }

  /** @return The custom tag. */
  public String getCustomTag() {
    return customTag;
  }

  /** @return Usage of the custom tag. */
  public CustomTagUsageBreakdown getBreakdown() {
    return breakdown;
  }

  /**
   * @return Day of the usage as midnight UTC of that day, or <code>null</code> if the report is not
   *     aggregated by day.
   */
  public @Nullable Date getUsageDate() {
    return usageDate;
  }
}
