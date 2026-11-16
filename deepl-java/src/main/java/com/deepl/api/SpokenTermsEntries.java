// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.List;
import java.util.Objects;

/** The terms of one language in a Spoken Terms collection. */
public class SpokenTermsEntries {
  private final String lang;
  private final List<String> entries;

  /**
   * Initializes a new {@link SpokenTermsEntries} object.
   *
   * @param lang Language code (ISO 639-1) of the terms, for example "en".
   * @param entries Terms of the language, one term per element; must not be <code>null</code>.
   * @throws NullPointerException If entries is <code>null</code>.
   */
  public SpokenTermsEntries(String lang, List<String> entries) {
    this.lang = lang;
    this.entries = Objects.requireNonNull(entries, "entries");
  }

  /** @return Language code (ISO 639-1) of the terms. */
  public String getLang() {
    return lang;
  }

  /** @return Terms of the language, one term per element. */
  public List<String> getEntries() {
    return entries;
  }

  @Override
  public String toString() {
    return "SpokenTermsEntries{lang='" + lang + "', entries=" + getEntries() + '}';
  }
}
