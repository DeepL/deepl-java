// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Values for {@link GetLanguagesForResourceOptions#setInclude(LanguageInclude...)}. The API may
 * accept further values; pass those as strings to {@link
 * GetLanguagesForResourceOptions#setInclude(java.util.List)}.
 */
public enum LanguageInclude {
  /** Also return beta languages and features. */
  Beta("beta"),

  /** Also return features that rely on third-party service providers. */
  External("external");

  private final String value;

  LanguageInclude(String value) {
    this.value = value;
  }

  /** @return The value sent to the API, for example "beta". */
  public String getValue() {
    return value;
  }
}
