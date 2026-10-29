// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import org.jetbrains.annotations.Nullable;

/** The result of a text translation. */
public class TextResult {
  private final String text;
  private final String detectedSourceLanguage;
  private final int billedCharacters;
  private final @Nullable String modelTypeUsed;
  private final @Nullable String tagHandlingVersion;

  /** Constructs a new instance. */
  public TextResult(
      String text,
      String detectedSourceLanguage,
      int billedCharacters,
      @Nullable String modelTypeUsed) {
    this(text, detectedSourceLanguage, billedCharacters, modelTypeUsed, null);
  }

  /** Constructs a new instance. */
  public TextResult(
      String text,
      String detectedSourceLanguage,
      int billedCharacters,
      @Nullable String modelTypeUsed,
      @Nullable String tagHandlingVersion) {
    this.text = text;
    this.detectedSourceLanguage = LanguageCode.standardize(detectedSourceLanguage);
    this.billedCharacters = billedCharacters;
    this.modelTypeUsed = modelTypeUsed;
    this.tagHandlingVersion = tagHandlingVersion;
  }

  /** The translated text. */
  public String getText() {
    return text;
  }

  /** The language code of the source text detected by DeepL. */
  public String getDetectedSourceLanguage() {
    return detectedSourceLanguage;
  }

  /** Number of characters billed for this text, 0 if the response does not contain it. */
  public int getBilledCharacters() {
    return billedCharacters;
  }

  /**
   * Model type used for the translation of this text, for example <code>"quality_optimized"</code>
   * (see {@link ModelType#getValue()}), or <code>null</code> if the response does not contain it.
   */
  public @Nullable String getModelTypeUsed() {
    return modelTypeUsed;
  }

  /**
   * Tag handling version used for the translation of this text, for example <code>"v2"</code> (see
   * {@link TagHandlingVersion#getValue()}). <code>null</code> if the response does not contain it;
   * the API returns it when tag handling was requested.
   */
  public @Nullable String getTagHandlingVersion() {
    return tagHandlingVersion;
  }
}
