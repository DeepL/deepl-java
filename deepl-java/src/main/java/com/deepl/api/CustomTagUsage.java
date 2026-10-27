// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.time.LocalDate;
import java.util.*;
import org.jetbrains.annotations.*;

/** Usage of a single custom tag, see {@link CustomTagUsageReport}. */
public class CustomTagUsage {
  @SerializedName(value = "custom_tag")
  private final String customTag;

  @SerializedName(value = "breakdown")
  private final CustomTagUsageBreakdown breakdown;

  @SerializedName(value = "usage_date")
  private final @Nullable LocalDate usageDate;

  /**
   * Initializes a new {@link CustomTagUsage}.
   *
   * @param customTag The custom tag.
   * @param breakdown Usage of the custom tag.
   * @param usageDate Day of the usage, present when aggregated by day.
   */
  public CustomTagUsage(
      String customTag, CustomTagUsageBreakdown breakdown, @Nullable LocalDate usageDate) {
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

  /** @return Day of the usage, or <code>null</code> if the report is not aggregated by day. */
  public @Nullable LocalDate getUsageDate() {
    return usageDate;
  }
}
