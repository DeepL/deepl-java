// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;

/**
 * Information about a language pair supported for glossaries.
 *
 * @see DeepLClient#getGlossaryLanguages()
 */
public class GlossaryLanguagePair {
  @SerializedName("source_lang")
  private final String sourceLang;

  @SerializedName("target_lang")
  private final String targetLang;

  /**
   * Initializes a new GlossaryLanguagePair object.
   *
   * @param sourceLang Language code of the source terms in the glossary.
   * @param targetLang Language code of the target terms in the glossary.
   */
  public GlossaryLanguagePair(String sourceLang, String targetLang) {
    this.sourceLang = LanguageCode.standardize(sourceLang);
    this.targetLang = LanguageCode.standardize(targetLang);
  }

  /** @return Language code of the source terms in the glossary. */
  public String getSourceLanguage() {
    return sourceLang;
  }

  /** @return Language code of the target terms in the glossary. */
  public String getTargetLanguage() {
    return targetLang;
  }
}
