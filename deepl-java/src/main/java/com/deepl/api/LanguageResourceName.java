// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Resources whose supported languages can be retrieved with {@link
 * DeepLClient#getLanguagesForResource(LanguageResourceName)}. The API may support further
 * resources; pass those as a string to {@link DeepLClient#getLanguagesForResource(String)}.
 */
public enum LanguageResourceName {
  /** Text translation. */
  TranslateText("translate_text"),

  /** Document translation. */
  TranslateDocument("translate_document"),

  /** Glossaries. */
  Glossary("glossary"),

  /** Voice. */
  Voice("voice"),

  /** Write (rephrase and correct). */
  Write("write"),

  /** Style rules. */
  StyleRules("style_rules"),

  /** Translation memories. */
  TranslationMemory("translation_memory");

  private final String value;

  LanguageResourceName(String value) {
    this.value = value;
  }

  /** @return The value sent to the API, for example "translate_text". */
  public String getValue() {
    return value;
  }
}
