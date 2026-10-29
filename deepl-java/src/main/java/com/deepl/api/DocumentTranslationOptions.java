// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options to control document translation behaviour. These options may be provided to {@link
 * Translator#translateDocument} overloads.
 *
 * <p>All properties have corresponding setters in fluent-style, so the following is possible:
 * <code>
 *      DocumentTranslationOptions options = new DocumentTranslationOptions()
 *          .setFormality(Formality.Less).setGlossary("f63c02c5-f056-..");
 * </code>
 */
public class DocumentTranslationOptions extends BaseRequestOptions {
  private Formality formality;
  private String formalityString;
  private String glossaryId;
  private List<String> glossaryIds;
  private String styleId;
  private String styleProfileId;
  private String translationMemoryId;
  private Integer translationMemoryThreshold;
  private String outputFormat;
  private @Nullable Boolean enableWatermark;
  private @Nullable String filename;
  private @Nullable Duration timeout;

  /**
   * Sets whether translations should lean toward formal or informal language. This option is only
   * applicable for target languages that support the formality option. By default, this value is
   * <code>null</code> and translations use the default formality.
   *
   * @see Language#getSupportsFormality()
   * @see Formality
   */
  public DocumentTranslationOptions setFormality(Formality formality) {
    this.formality = formality;
    this.formalityString = null;
    return this;
  }

  /**
   * Sets the formality as a string, which is sent to the API as given, for example <code>"less"
   * </code>. Use this for values not yet in {@link Formality}. By default, this value is <code>null
   * </code> and translations use the default formality.
   *
   * @see #setFormality(Formality)
   */
  public DocumentTranslationOptions setFormality(String formality) {
    this.formalityString = formality;
    this.formality = null;
    return this;
  }

  /**
   * Sets the ID of a glossary to use with the translation. By default, this value is <code>
   * null</code> and no glossary is used.
   *
   * @deprecated Use {@link #setGlossary(String)} instead.
   */
  @Deprecated
  public DocumentTranslationOptions setGlossaryId(String glossaryId) {
    this.glossaryId = glossaryId;
    return this;
  }

  /**
   * Sets the glossary to use with the translation. By default, this value is <code>null</code> and
   * no glossary is used.
   */
  public DocumentTranslationOptions setGlossary(IGlossary glossary) {
    return setGlossary(glossary.getGlossaryId());
  }

  /**
   * Sets the glossary to use with the translation. By default, this value is <code>null</code> and
   * no glossary is used.
   */
  public DocumentTranslationOptions setGlossary(String glossaryId) {
    this.glossaryId = glossaryId;
    return this;
  }

  /**
   * Sets the list of glossary IDs to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option requires a source language to be set and cannot be
   * combined with {@link #setGlossary}.
   */
  public DocumentTranslationOptions setGlossaryIds(List<String> glossaryIds) {
    this.glossaryIds = glossaryIds;
    return this;
  }

  /**
   * Sets the list of glossary IDs to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option requires a source language to be set and cannot be
   * combined with {@link #setGlossary}.
   */
  public DocumentTranslationOptions setGlossaryIds(String... glossaryIds) {
    this.glossaryIds = Arrays.asList(glossaryIds);
    return this;
  }

  /**
   * Sets the list of glossaries to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option requires a source language to be set and cannot be
   * combined with {@link #setGlossary}.
   */
  public DocumentTranslationOptions setGlossaryIds(IGlossary first, IGlossary... more) {
    // The separate first parameter keeps the zero-argument call setGlossaryIds() on String...
    IGlossary[] glossaries = new IGlossary[more.length + 1];
    glossaries[0] = first;
    System.arraycopy(more, 0, glossaries, 1, more.length);
    this.glossaryIds = glossaryIdsOf(glossaries);
    return this;
  }

  /**
   * Sets the list of glossaries to use with the translation, up to a maximum of 5.
   *
   * @deprecated Use {@link #setGlossaryIds(IGlossary, IGlossary...)} instead.
   */
  @Deprecated
  public DocumentTranslationOptions setGlossaries(IGlossary... glossaries) {
    this.glossaryIds = glossaryIdsOf(glossaries);
    return this;
  }

  private static List<String> glossaryIdsOf(IGlossary[] glossaries) {
    List<String> ids = new ArrayList<>();
    for (IGlossary glossary : glossaries) {
      if (glossary == null) {
        throw new IllegalArgumentException("glossaries must not contain null");
      }
      ids.add(glossary.getGlossaryId());
    }
    return ids;
  }

  /**
   * Sets the ID of a style rule to use with the translation. By default, this value is <code>
   * null</code> and no style rule is used.
   *
   * @deprecated Use {@link #setStyleRule(String)} instead.
   */
  @Deprecated
  public DocumentTranslationOptions setStyleId(String styleId) {
    this.styleId = styleId;
    return this;
  }

  /**
   * Sets the ID of a style rule to use with the translation. By default, this value is <code>
   * null</code> and no style rule is used.
   */
  public DocumentTranslationOptions setStyleRule(String styleRuleId) {
    this.styleId = styleRuleId;
    return this;
  }

  /**
   * Sets the style rule to use with the translation. By default, this value is <code>null</code>
   * and no style rule is used.
   */
  public DocumentTranslationOptions setStyleRule(StyleRuleInfo styleRule) {
    if (styleRule == null) {
      throw new IllegalArgumentException("styleRule must not be null");
    }
    return setStyleRule(styleRule.getStyleId());
  }

  /**
   * Sets the ID of a style profile to use with the translation. A style profile applies its
   * glossaries, style rules and translation memories through one ID; components that do not fit the
   * language pair are left out. By default, this value is <code>null</code> and no style profile is
   * used. The API rejects combining a style profile with glossaries, a style rule, a translation
   * memory or a translation memory threshold.
   */
  public DocumentTranslationOptions setStyleProfile(String styleProfileId) {
    this.styleProfileId = styleProfileId;
    return this;
  }

  /**
   * Sets the style profile to use with the translation. By default, this value is <code>null
   * </code> and no style profile is used. See {@link #setStyleProfile(String)}.
   */
  public DocumentTranslationOptions setStyleProfile(StyleProfile styleProfile) {
    if (styleProfile == null) {
      throw new IllegalArgumentException("styleProfile must not be null");
    }
    return setStyleProfile(styleProfile.getStyleProfileId());
  }

  /**
   * Sets the ID of a translation memory to use with the translation. By default, this value is
   * <code>null</code> and no translation memory is used.
   *
   * @deprecated Use {@link #setTranslationMemory(String)} instead.
   */
  @Deprecated
  public DocumentTranslationOptions setTranslationMemoryId(String translationMemoryId) {
    this.translationMemoryId = translationMemoryId;
    return this;
  }

  /**
   * Sets the ID of a translation memory to use with the translation. By default, this value is
   * <code>null</code> and no translation memory is used.
   */
  public DocumentTranslationOptions setTranslationMemory(String translationMemoryId) {
    this.translationMemoryId = translationMemoryId;
    return this;
  }

  /**
   * Sets the translation memory to use with the translation. By default, this value is <code>null
   * </code> and no translation memory is used.
   */
  public DocumentTranslationOptions setTranslationMemory(TranslationMemoryInfo translationMemory) {
    if (translationMemory == null) {
      throw new IllegalArgumentException("translationMemory must not be null");
    }
    return setTranslationMemory(translationMemory.getTranslationMemoryId());
  }

  /**
   * Sets the threshold for translation memory matches. By default, this value is <code>null</code>
   * and the API default threshold is used. Note: a translation memory must also be set via {@link
   * #setTranslationMemory}, otherwise an error will be thrown at translation time.
   */
  public DocumentTranslationOptions setTranslationMemoryThreshold(
      Integer translationMemoryThreshold) {
    if (translationMemoryThreshold != null
        && (translationMemoryThreshold < 0 || translationMemoryThreshold > 100)) {
      throw new IllegalArgumentException("translationMemoryThreshold must be between 0 and 100");
    }
    this.translationMemoryThreshold = translationMemoryThreshold;
    return this;
  }

  /**
   * Sets the file format of the translated document, as a file extension, for example <code>
   * "docx"</code>. By default, this value is <code>null</code> and the translated document has the
   * same format as the input document.
   */
  public DocumentTranslationOptions setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
    return this;
  }

  /**
   * Sets whether a "Translated by DeepL" watermark is added to the translated document. The API
   * supports watermarks only for <code>docx</code> and <code>pdf</code> output and ignores this
   * option for other output formats. By default, this value is <code>null</code> and the option is
   * not sent, so the API default (no watermark) applies.
   */
  public DocumentTranslationOptions setEnableWatermark(@Nullable Boolean enableWatermark) {
    this.enableWatermark = enableWatermark;
    return this;
  }

  /**
   * Sets the file name sent with the upload, for example <code>"report.docx"</code>. The file
   * extension determines the document type. It is used in place of the input file's name and of the
   * <code>fileName</code> argument of the stream overloads. By default, this value is <code>null
   * </code> and the file name of the input is used.
   */
  public DocumentTranslationOptions setFilename(@Nullable String filename) {
    this.filename = filename;
    return this;
  }

  /**
   * Sets the maximum time {@link Translator#translateDocument} waits for the translation to finish
   * after the upload. When it expires, <code>translateDocument</code> throws a {@link
   * DocumentTranslationException} that carries the document handle, so the document can still be
   * retrieved. This is a client-side limit and is not sent to the API. By default, this value is
   * <code>null</code> and there is no limit.
   */
  public DocumentTranslationOptions setTimeout(@Nullable Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  /** Gets the current formality setting. */
  public Formality getFormality() {
    return formality;
  }

  /** The formality set as a string, sent in place of {@link #getFormality()}. */
  String getFormalityString() {
    return formalityString;
  }

  /** Gets the current glossary ID. */
  public String getGlossaryId() {
    return glossaryId;
  }

  /** Gets the current list of glossary IDs. */
  public List<String> getGlossaryIds() {
    return glossaryIds;
  }

  /** Gets the current style rule ID. */
  public String getStyleId() {
    return styleId;
  }

  /** Gets the current style profile ID. */
  public String getStyleProfileId() {
    return styleProfileId;
  }

  /** Gets the current translation memory ID. */
  public String getTranslationMemoryId() {
    return translationMemoryId;
  }

  /** Gets the current translation memory threshold. */
  public Integer getTranslationMemoryThreshold() {
    return translationMemoryThreshold;
  }

  /** Gets the current output format. */
  public String getOutputFormat() {
    return outputFormat;
  }

  /** Gets the current watermark setting, or <code>null</code> if it is not set. */
  public @Nullable Boolean getEnableWatermark() {
    return enableWatermark;
  }

  /** Gets the file name set with {@link #setFilename}, or <code>null</code> if it is not set. */
  public @Nullable String getFilename() {
    return filename;
  }

  /** Gets the wait limit of {@link Translator#translateDocument}, or <code>null</code> if unset. */
  public @Nullable Duration getTimeout() {
    return timeout;
  }

  /** {@inheritDoc} */
  @Override
  public DocumentTranslationOptions setExtraBodyParameters(
      Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
