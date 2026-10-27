// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.*;

/**
 * Language codes for the languages currently supported by DeepL translation. New languages may be
 * added in the future; to retrieve the currently supported languages use {@link
 * Translator#getSourceLanguages()} and {@link Translator#getTargetLanguages()}.
 */
public class LanguageCode {
  /**
   * Arabic (MSA) language code, may be used as source or target language
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Arabic = "ar";

  /**
   * Bulgarian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Bulgarian = "bg";

  /**
   * Czech language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Czech = "cs";

  /**
   * Danish language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Danish = "da";

  /**
   * German language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String German = "de";

  /**
   * Swiss German language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String GermanSwiss = "de-CH";

  /**
   * German (Germany) language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String GermanGermany = "de-DE";

  /**
   * Greek language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Greek = "el";

  /**
   * English language code, may be used as source or target language. In input texts, this language
   * code supports all English variants. As a target language it translates to American English; use
   * {@link #EnglishBritish} or {@link #EnglishAmerican} to choose the variant explicitly.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String English = "en";

  /**
   * British English language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String EnglishBritish = "en-GB";

  /**
   * American English language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String EnglishAmerican = "en-US";

  /**
   * Spanish language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Spanish = "es";

  /**
   * European Spanish language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String SpanishEuropean = "es-ES";

  /**
   * Latin American Spanish language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String SpanishLatinAmerican = "es-419";

  /**
   * Estonian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Estonian = "et";

  /**
   * Finnish language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Finnish = "fi";

  /**
   * French language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String French = "fr";

  /**
   * Canadian French language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String FrenchCanadian = "fr-CA";

  /**
   * European French language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String FrenchEuropean = "fr-FR";

  /**
   * Hungarian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Hungarian = "hu";

  /**
   * Indonesian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Indonesian = "id";

  /**
   * Italian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Italian = "it";

  /**
   * Japanese language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Japanese = "ja";

  /**
   * Korean language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Korean = "ko";

  /**
   * Lithuanian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Lithuanian = "lt";

  /**
   * Latvian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Latvian = "lv";

  /**
   * Norwegian (bokmål) language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Norwegian = "nb";

  /**
   * Dutch language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Dutch = "nl";

  /**
   * Polish language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Polish = "pl";

  /**
   * Portuguese language code, may be used as source or target language. In input texts, this
   * language code supports all Portuguese variants. As a target language it translates to European
   * Portuguese; use {@link #PortugueseBrazilian} or {@link #PortugueseEuropean} to choose the
   * variant explicitly.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Portuguese = "pt";

  /**
   * Brazilian Portuguese language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String PortugueseBrazilian = "pt-BR";

  /**
   * European Portuguese language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String PortugueseEuropean = "pt-PT";

  /**
   * Romanian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Romanian = "ro";

  /**
   * Russian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Russian = "ru";

  /**
   * Slovak language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Slovak = "sk";

  /**
   * Slovenian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Slovenian = "sl";

  /**
   * Swedish language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Swedish = "sv";

  /**
   * Turkish language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Turkish = "tr";

  /**
   * Ukrainian language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Ukrainian = "uk";

  /**
   * Chinese language code, may be used as source or target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String Chinese = "zh";

  /**
   * Simplified Chinese language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String ChineseSimplified = "zh-Hans";

  /**
   * Traditional Chinese language code, may only be used as a target language.
   *
   * @deprecated Use the language code string, e.g. {@code "de"}, or the languages API. Will be
   *     removed in the next major version.
   */
  @Deprecated public static final String ChineseTraditional = "zh-Hant";

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
