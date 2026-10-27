// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/** Options for grouping the usage analytics report, see {@link DeepLAdminClient}. */
public enum AdminUsageGroupBy {
  /** Group usage by API key. */
  Key("key"),

  /** Group usage by API key and usage date. */
  KeyAndDay("key_and_day");

  private final String value;

  AdminUsageGroupBy(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
