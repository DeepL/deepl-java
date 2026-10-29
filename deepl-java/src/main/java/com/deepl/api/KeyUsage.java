// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.util.*;
import org.jetbrains.annotations.*;

/** Usage of a single API key in a usage analytics report, see {@link AdminUsageReport}. */
public class KeyUsage {
  @SerializedName(value = "api_key")
  private final String apiKey;

  @SerializedName(value = "api_key_label")
  private final @Nullable String apiKeyLabel;

  @SerializedName(value = "usage")
  private final AdminUsageBreakdown usage;

  @SerializedName(value = "usage_date")
  private final @Nullable Date usageDate;

  /**
   * Initializes a new {@link KeyUsage}.
   *
   * @param apiKey Masked API key.
   * @param apiKeyLabel Label of the API key, or <code>null</code> if the API sent none.
   * @param usage Usage of the API key.
   * @param usageDate Day of the usage as midnight UTC, present when grouped by key and day.
   */
  public KeyUsage(
      String apiKey,
      @Nullable String apiKeyLabel,
      AdminUsageBreakdown usage,
      @Nullable Date usageDate) {
    this.apiKey = apiKey;
    this.apiKeyLabel = apiKeyLabel;
    this.usage = usage;
    this.usageDate = usageDate;
  }

  /** @return Masked API key. */
  public String getApiKey() {
    return apiKey;
  }

  /** @return Label of the API key, or <code>null</code> if the API sent none. */
  public @Nullable String getApiKeyLabel() {
    return apiKeyLabel;
  }

  /** @return Usage of the API key. */
  public AdminUsageBreakdown getUsage() {
    return usage;
  }

  /**
   * @return Day of the usage as midnight UTC of that day, or <code>null</code> if the report is not
   *     grouped by day.
   */
  public @Nullable Date getUsageDate() {
    return usageDate;
  }
}
