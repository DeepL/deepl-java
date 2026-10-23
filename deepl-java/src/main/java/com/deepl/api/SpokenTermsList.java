// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;

/** Information about the term list of one language in a Spoken Terms collection. */
public class SpokenTermsList {
  @SerializedName(value = "lang")
  private final String lang;

  @SerializedName(value = "entry_count")
  private final long entryCount;

  /**
   * Initializes a new {@link SpokenTermsList}.
   *
   * @param lang Language code (ISO 639-1) of the term list.
   * @param entryCount Number of terms in the term list.
   */
  public SpokenTermsList(String lang, long entryCount) {
    this.lang = lang;
    this.entryCount = entryCount;
  }

  /** @return Language code (ISO 639-1) of the term list. */
  public String getLang() {
    return lang;
  }

  /** @return Number of terms in the term list. */
  public long getEntryCount() {
    return entryCount;
  }

  @Override
  public String toString() {
    return "SpokenTermsList{lang='" + lang + "', entryCount=" + entryCount + '}';
  }
}
