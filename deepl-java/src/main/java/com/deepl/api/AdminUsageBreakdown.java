// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import org.jetbrains.annotations.*;

/** Usage totals in a usage analytics report, see {@link AdminUsageReport}. */
public class AdminUsageBreakdown {
  @SerializedName(value = "total_characters")
  private final long totalCharacters;

  @SerializedName(value = "text_translation_characters")
  private final long textTranslationCharacters;

  @SerializedName(value = "document_translation_characters")
  private final long documentTranslationCharacters;

  @SerializedName(value = "text_improvement_characters")
  private final long textImprovementCharacters;

  @SerializedName(value = "speech_to_text_minutes")
  private final double speechToTextMinutes;

  @SerializedName(value = "speech_to_speech_minutes")
  private final @Nullable Double speechToSpeechMinutes;

  /**
   * Initializes a new {@link AdminUsageBreakdown}.
   *
   * @param totalCharacters Total number of characters used.
   * @param textTranslationCharacters Number of characters used for text translation.
   * @param documentTranslationCharacters Number of characters used for document translation.
   * @param textImprovementCharacters Number of characters used for text improvement.
   * @param speechToTextMinutes Number of speech-to-text minutes used.
   * @param speechToSpeechMinutes Number of speech-to-speech minutes used, or <code>null</code> if
   *     the report does not include it.
   */
  public AdminUsageBreakdown(
      long totalCharacters,
      long textTranslationCharacters,
      long documentTranslationCharacters,
      long textImprovementCharacters,
      double speechToTextMinutes,
      @Nullable Double speechToSpeechMinutes) {
    this.totalCharacters = totalCharacters;
    this.textTranslationCharacters = textTranslationCharacters;
    this.documentTranslationCharacters = documentTranslationCharacters;
    this.textImprovementCharacters = textImprovementCharacters;
    this.speechToTextMinutes = speechToTextMinutes;
    this.speechToSpeechMinutes = speechToSpeechMinutes;
  }

  /** @return Total number of characters used. */
  public long getTotalCharacters() {
    return totalCharacters;
  }

  /** @return Number of characters used for text translation. */
  public long getTextTranslationCharacters() {
    return textTranslationCharacters;
  }

  /** @return Number of characters used for document translation. */
  public long getDocumentTranslationCharacters() {
    return documentTranslationCharacters;
  }

  /** @return Number of characters used for text improvement. */
  public long getTextImprovementCharacters() {
    return textImprovementCharacters;
  }

  /** @return Number of speech-to-text minutes used. */
  public double getSpeechToTextMinutes() {
    return speechToTextMinutes;
  }

  /**
   * @return Number of speech-to-speech minutes used, or <code>null</code> if the report does not
   *     include it.
   */
  public @Nullable Double getSpeechToSpeechMinutes() {
    return speechToSpeechMinutes;
  }
}
