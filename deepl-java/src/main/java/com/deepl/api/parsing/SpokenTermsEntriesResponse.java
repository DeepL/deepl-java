// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

/**
 * Class representing the v3 spoken-terms entries response by the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class SpokenTermsEntriesResponse {
  private String lang;
  private String entries;

  public String getLang() {
    return lang;
  }

  public String getEntries() {
    return entries;
  }
}
