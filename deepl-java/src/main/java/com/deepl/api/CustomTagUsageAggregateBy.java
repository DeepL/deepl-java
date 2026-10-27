// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/** Options for aggregating the custom tag usage analytics report, see {@link DeepLAdminClient}. */
public enum CustomTagUsageAggregateBy {
  /** Aggregate usage over the entire date range. */
  Period("period"),

  /** Aggregate usage by individual day. */
  Day("day");

  private final String value;

  CustomTagUsageAggregateBy(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
