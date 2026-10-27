// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import org.jetbrains.annotations.*;

/** Usage limits of a developer key. A <code>null</code> limit means unlimited. */
public class DeveloperKeyUsageLimits {
  @SerializedName(value = "characters")
  private final @Nullable Long characters;

  @SerializedName(value = "speech_to_text_milliseconds")
  private final @Nullable Long speechToTextMilliseconds;

  @SerializedName(value = "speech_to_speech_milliseconds")
  private final @Nullable Long speechToSpeechMilliseconds;

  /**
   * Initializes a new {@link DeveloperKeyUsageLimits}.
   *
   * @param characters Maximum number of characters per usage period, or <code>null</code> if
   *     unlimited.
   * @param speechToTextMilliseconds Maximum number of speech-to-text milliseconds per usage period,
   *     or <code>null</code> if unlimited.
   * @param speechToSpeechMilliseconds Maximum number of speech-to-speech milliseconds per usage
   *     period, or <code>null</code> if unlimited.
   */
  public DeveloperKeyUsageLimits(
      @Nullable Long characters,
      @Nullable Long speechToTextMilliseconds,
      @Nullable Long speechToSpeechMilliseconds) {
    this.characters = characters;
    this.speechToTextMilliseconds = speechToTextMilliseconds;
    this.speechToSpeechMilliseconds = speechToSpeechMilliseconds;
  }

  /** @return Maximum number of characters per usage period, or <code>null</code> if unlimited. */
  public @Nullable Long getCharacters() {
    return characters;
  }

  /**
   * @return Maximum number of speech-to-text milliseconds per usage period, or <code>null</code> if
   *     unlimited.
   */
  public @Nullable Long getSpeechToTextMilliseconds() {
    return speechToTextMilliseconds;
  }

  /**
   * @return Maximum number of speech-to-speech milliseconds per usage period, or <code>null</code>
   *     if unlimited.
   */
  public @Nullable Long getSpeechToSpeechMilliseconds() {
    return speechToSpeechMilliseconds;
  }
}
