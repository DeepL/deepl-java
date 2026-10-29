// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Support level of a single feature for a language, as returned by {@link
 * DeepLClient#getLanguagesForResource(String, GetLanguagesForResourceOptions)}.
 */
public class LanguageFeature {
  /** Feature: automatic source-language detection. */
  public static final String FEATURE_AUTO_DETECTION = "auto_detection";
  /** Feature: formality control. */
  public static final String FEATURE_FORMALITY = "formality";
  /** Feature: glossary support. */
  public static final String FEATURE_GLOSSARY = "glossary";
  /** Feature: style rules. */
  public static final String FEATURE_STYLE_RULES = "style_rules";
  /** Feature: tag handling. */
  public static final String FEATURE_TAG_HANDLING = "tag_handling";
  /** Feature: translation memory. */
  public static final String FEATURE_TRANSLATION_MEMORY = "translation_memory";
  /** Feature: tone control (write). */
  public static final String FEATURE_TONE = "tone";
  /** Feature: writing style (write). */
  public static final String FEATURE_WRITING_STYLE = "writing_style";
  /** Feature: transcription (voice). */
  public static final String FEATURE_TRANSCRIPTION = "transcription";
  /** Feature: translated speech (voice). */
  public static final String FEATURE_TRANSLATED_SPEECH = "translated_speech";
  /** Feature: spoken terms (voice). */
  public static final String FEATURE_SPOKEN_TERMS = "spoken_terms";

  private final String status;

  /**
   * Initializes a new LanguageFeature object.
   *
   * @param status Support status of the feature, for example "stable" or "beta".
   */
  public LanguageFeature(String status) {
    this.status = status;
  }

  /** @return Support status of the feature, for example "stable" or "beta". */
  public String getStatus() {
    return status;
  }
}
