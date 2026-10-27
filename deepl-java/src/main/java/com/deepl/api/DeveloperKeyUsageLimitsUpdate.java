// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * Usage limits to change with {@link DeepLAdminClient#setDeveloperKeyUsageLimits(String,
 * DeveloperKeyUsageLimitsUpdate)}. Only the limits you set are changed; the other limits of the key
 * keep their current value. Setting a limit to <code>null</code> removes it, making that usage
 * unlimited, and 0 blocks that usage.
 *
 * <p>All properties have corresponding setters in fluent-style, so the following is possible:
 * <code>
 *      DeveloperKeyUsageLimitsUpdate limits = new DeveloperKeyUsageLimitsUpdate()
 *          .setCharacters(500000L).setSpeechToTextMilliseconds(null);
 * </code>
 */
public class DeveloperKeyUsageLimitsUpdate {
  private boolean charactersSet;
  private @Nullable Long characters;
  private boolean speechToTextMillisecondsSet;
  private @Nullable Long speechToTextMilliseconds;
  private boolean speechToSpeechMillisecondsSet;
  private @Nullable Long speechToSpeechMilliseconds;

  /**
   * Sets the maximum number of characters per usage period across text translation, document
   * translation and text improvement, or <code>null</code> for unlimited.
   */
  public DeveloperKeyUsageLimitsUpdate setCharacters(@Nullable Long characters) {
    this.charactersSet = true;
    this.characters = characters;
    return this;
  }

  /**
   * Sets the maximum number of speech-to-text milliseconds per usage period, or <code>null</code>
   * for unlimited.
   */
  public DeveloperKeyUsageLimitsUpdate setSpeechToTextMilliseconds(
      @Nullable Long speechToTextMilliseconds) {
    this.speechToTextMillisecondsSet = true;
    this.speechToTextMilliseconds = speechToTextMilliseconds;
    return this;
  }

  /**
   * Sets the maximum number of speech-to-speech milliseconds per usage period, or <code>null</code>
   * for unlimited.
   */
  public DeveloperKeyUsageLimitsUpdate setSpeechToSpeechMilliseconds(
      @Nullable Long speechToSpeechMilliseconds) {
    this.speechToSpeechMillisecondsSet = true;
    this.speechToSpeechMilliseconds = speechToSpeechMilliseconds;
    return this;
  }

  /** Adds the limits that were set to the request; explicit nulls are kept as JSON null. */
  void addTo(JsonObject requestData) {
    if (charactersSet) {
      requestData.addProperty("characters", characters);
    }
    if (speechToTextMillisecondsSet) {
      requestData.addProperty("speech_to_text_milliseconds", speechToTextMilliseconds);
    }
    if (speechToSpeechMillisecondsSet) {
      requestData.addProperty("speech_to_speech_milliseconds", speechToSpeechMilliseconds);
    }
  }
}
