// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.*;

/**
 * Helpers for language code strings. To retrieve the languages currently supported by DeepL
 * translation, use {@link Translator#getSourceLanguages()} and {@link
 * Translator#getTargetLanguages()}.
 */
public class LanguageCode {
  /**
   * Removes the regional variant (if any) from the given language code.
   *
   * @param langCode Language code possibly containing a regional variant.
   * @return The language code without a regional variant.
   */
  public static String removeRegionalVariant(String langCode) {
    String[] parts = langCode.split("-", 2);
    return parts[0].toLowerCase(Locale.ENGLISH);
  }

  /**
   * Changes the upper- and lower-casing of the given language code to match ISO 639-1 with an
   * optional regional code from ISO 3166-1.
   *
   * @param langCode String containing language code to standardize.
   * @return String containing the standardized language code.
   */
  public static String standardize(String langCode) {
    String[] parts = langCode.split("-", 2);
    if (parts.length == 1) {
      return parts[0].toLowerCase(Locale.ENGLISH);
    } else {
      return parts[0].toLowerCase(Locale.ENGLISH) + "-" + parts[1].toUpperCase(Locale.ENGLISH);
    }
  }
}
