// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import org.jetbrains.annotations.Nullable;

/**
 * A language supported by DeepL translation. The {@link DeepLClient} class provides functions to
 * retrieve the available source and target languages.
 *
 * @see DeepLClient#getSourceLanguages()
 * @see DeepLClient#getTargetLanguages()
 */
public class Language {
  private final String name;
  private final String code;
  private final @Nullable Boolean supportsFormality;

  /**
   * Initializes a new Language object.
   *
   * @param name The name of the language in English.
   * @param code The language code.
   * @param supportsFormality <code>true</code> for a target language that supports the {@link
   *     TextTranslationOptions#setFormality} option for translations, <code>false</code> for other
   *     target languages, or <code>null</code> for source languages.
   */
  public Language(String name, String code, @Nullable Boolean supportsFormality) {
    this.name = name;
    this.code = LanguageCode.standardize(code);
    this.supportsFormality = supportsFormality;
  }

  /** @return The name of the language in English, for example "Italian" or "Romanian". */
  public String getName() {
    return name;
  }

  /**
   * @return The language code, for example "it", "ro" or "en-US". Language codes follow ISO 639-1
   *     with an optional regional code from ISO 3166-1.
   */
  public String getCode() {
    return code;
  }

  /**
   * @return <code>true</code> if this language is a target language that supports the {@link
   *     TextTranslationOptions#setFormality} option for translations, <code>false</code> if this
   *     language is a target language that does not support formality, or <code>null</code> if this
   *     language is a source language.
   */
  public @Nullable Boolean getSupportsFormality() {
    return supportsFormality;
  }
}
