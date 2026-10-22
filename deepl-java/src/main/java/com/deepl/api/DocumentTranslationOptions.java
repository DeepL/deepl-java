// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Options to control document translation behaviour. These options may be provided to {@link
 * Translator#translateDocument} overloads.
 *
 * <p>All properties have corresponding setters in fluent-style, so the following is possible:
 * <code>
 *      DocumentTranslationOptions options = new DocumentTranslationOptions()
 *          .setFormality(Formality.Less).setGlossaryId("f63c02c5-f056-..");
 * </code>
 */
public class DocumentTranslationOptions extends BaseRequestOptions {
  private Formality formality;
  private String glossaryId;
  private List<String> glossaryIds;
  private String styleId;
  private String translationMemoryId;
  private Integer translationMemoryThreshold;
  private String outputFormat;

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
    return this;
  }

  /**
   * Sets the ID of a glossary to use with the translation. By default, this value is <code>
   * null</code> and no glossary is used.
   */
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
   * combined with {@link #setGlossaryId} or {@link #setGlossary}.
   */
  public DocumentTranslationOptions setGlossaryIds(List<String> glossaryIds) {
    this.glossaryIds = glossaryIds;
    return this;
  }

  /**
   * Sets the list of glossary IDs to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option requires a source language to be set and cannot be
   * combined with {@link #setGlossaryId} or {@link #setGlossary}.
   */
  public DocumentTranslationOptions setGlossaryIds(String... glossaryIds) {
    this.glossaryIds = Arrays.asList(glossaryIds);
    return this;
  }

  /**
   * Sets the list of glossaries to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option requires a source language to be set and cannot be
   * combined with {@link #setGlossaryId} or {@link #setGlossary}.
   */
  public DocumentTranslationOptions setGlossaries(IGlossary... glossaries) {
    List<String> ids = new ArrayList<>();
    for (IGlossary glossary : glossaries) {
      if (glossary == null) {
        throw new IllegalArgumentException("glossaries must not contain null");
      }
      ids.add(glossary.getGlossaryId());
    }
    this.glossaryIds = ids;
    return this;
  }

  /**
   * Sets the ID of a style rule to use with the translation. By default, this value is <code>
   * null</code> and no style rule is used.
   */
  public DocumentTranslationOptions setStyleId(String styleId) {
    this.styleId = styleId;
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
    return setStyleId(styleRule.getStyleId());
  }

  /**
   * Sets the ID of a translation memory to use with the translation. By default, this value is
   * <code>null</code> and no translation memory is used.
   */
  public DocumentTranslationOptions setTranslationMemoryId(String translationMemoryId) {
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
    return setTranslationMemoryId(translationMemory.getTranslationMemoryId());
  }

  /**
   * Sets the threshold for translation memory matches. By default, this value is <code>null</code>
   * and the API default threshold is used. Note: a translation memory ID must also be set via
   * {@link #setTranslationMemoryId} or {@link #setTranslationMemory}, otherwise an error will be
   * thrown at translation time.
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

  /** Gets the current formality setting. */
  public Formality getFormality() {
    return formality;
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
}
