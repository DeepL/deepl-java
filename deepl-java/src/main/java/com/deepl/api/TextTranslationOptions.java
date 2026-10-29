// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Options to control text translation behaviour. These options may be provided to {@link
 * Translator#translateText} overloads.
 *
 * <p>All properties have corresponding setters in fluent-style, so the following is possible:
 * <code>
 *      TextTranslationOptions options = new TextTranslationOptions()
 *          .setFormality(Formality.Less).setGlossary("f63c02c5-f056-..");
 * </code>
 */
public class TextTranslationOptions extends BaseRequestOptions {
  private Formality formality;
  private String formalityString;
  private String glossaryId;
  private List<String> glossaryIds;
  private String styleId;
  private String styleProfileId;
  private String translationMemoryId;
  private Integer translationMemoryThreshold;
  private SentenceSplittingMode sentenceSplittingMode;
  private String sentenceSplittingModeString;
  private boolean preserveFormatting = false;
  private String context;
  private String tagHandling;
  private String tagHandlingVersion;
  private String modelType;
  private boolean outlineDetection = true;
  private Iterable<String> ignoreTags;
  private Iterable<String> nonSplittingTags;
  private Iterable<String> splittingTags;
  private Iterable<String> customInstructions;

  /**
   * Sets whether translations should lean toward formal or informal language. This option is only
   * applicable for target languages that support the formality option. By default, this value is
   * <code>null</code> and<code>null</code> translations use the default formality.
   *
   * @see Language#getSupportsFormality()
   * @see Formality
   */
  public TextTranslationOptions setFormality(Formality formality) {
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
  public TextTranslationOptions setFormality(String formality) {
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
  public TextTranslationOptions setGlossaryId(String glossaryId) {
    this.glossaryId = glossaryId;
    return this;
  }

  /**
   * Sets the glossary to use with the translation. By default, this value is <code>null</code> and
   * no glossary is used.
   */
  public TextTranslationOptions setGlossary(IGlossary glossary) {
    return setGlossary(glossary.getGlossaryId());
  }

  /**
   * Sets the glossary to use with the translation. By default, this value is <code>null</code> and
   * no glossary is used.
   */
  public TextTranslationOptions setGlossary(String glossaryId) {
    this.glossaryId = glossaryId;
    return this;
  }

  /**
   * Sets the list of glossary IDs to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option cannot be combined with {@link #setGlossary}.
   */
  public TextTranslationOptions setGlossaryIds(List<String> glossaryIds) {
    this.glossaryIds = glossaryIds;
    return this;
  }

  /**
   * Sets the list of glossary IDs to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option cannot be combined with {@link #setGlossary}.
   */
  public TextTranslationOptions setGlossaryIds(String... glossaryIds) {
    this.glossaryIds = Arrays.asList(glossaryIds);
    return this;
  }

  /**
   * Sets the list of glossaries to use with the translation, up to a maximum of 5. Glossaries are
   * applied in the order provided (first match wins). By default, this value is <code>null</code>
   * and no glossaries are used. This option cannot be combined with {@link #setGlossary}. To pass a
   * list, pass the glossary IDs to {@link #setGlossaryIds(List)}.
   */
  public TextTranslationOptions setGlossaryIds(IGlossary first, IGlossary... more) {
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
  public TextTranslationOptions setGlossaries(IGlossary... glossaries) {
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
  public TextTranslationOptions setStyleId(String styleId) {
    this.styleId = styleId;
    return this;
  }

  /**
   * Sets the ID of a style rule to use with the translation. By default, this value is <code>
   * null</code> and no style rule is used.
   */
  public TextTranslationOptions setStyleRule(String styleRuleId) {
    this.styleId = styleRuleId;
    return this;
  }

  /**
   * Sets the style rule to use with the translation. By default, this value is <code>null</code>
   * and no style rule is used.
   */
  public TextTranslationOptions setStyleRule(StyleRuleInfo styleRule) {
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
   * memory, a translation memory threshold or custom instructions.
   */
  public TextTranslationOptions setStyleProfile(String styleProfileId) {
    this.styleProfileId = styleProfileId;
    return this;
  }

  /**
   * Sets the style profile to use with the translation. By default, this value is <code>null
   * </code> and no style profile is used. See {@link #setStyleProfile(String)}.
   */
  public TextTranslationOptions setStyleProfile(StyleProfile styleProfile) {
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
  public TextTranslationOptions setTranslationMemoryId(String translationMemoryId) {
    this.translationMemoryId = translationMemoryId;
    return this;
  }

  /**
   * Sets the ID of a translation memory to use with the translation. By default, this value is
   * <code>null</code> and no translation memory is used.
   */
  public TextTranslationOptions setTranslationMemory(String translationMemoryId) {
    this.translationMemoryId = translationMemoryId;
    return this;
  }

  /**
   * Sets the translation memory to use with the translation. By default, this value is <code>null
   * </code> and no translation memory is used.
   */
  public TextTranslationOptions setTranslationMemory(TranslationMemoryInfo translationMemory) {
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
  public TextTranslationOptions setTranslationMemoryThreshold(Integer translationMemoryThreshold) {
    if (translationMemoryThreshold != null
        && (translationMemoryThreshold < 0 || translationMemoryThreshold > 100)) {
      throw new IllegalArgumentException("translationMemoryThreshold must be between 0 and 100");
    }
    this.translationMemoryThreshold = translationMemoryThreshold;
    return this;
  }

  /**
   * Specifies additional context to influence translations, that is not translated itself.
   * Characters in the `context` parameter are not counted toward billing. See the API documentation
   * for more information and example usage.
   */
  public TextTranslationOptions setContext(String context) {
    this.context = context;
    return this;
  }

  /**
   * Specifies how input translation text should be split into sentences. By default, this value is
   * <code>null</code> and the default sentence splitting mode is used.
   *
   * @see SentenceSplittingMode
   */
  public TextTranslationOptions setSentenceSplittingMode(
      SentenceSplittingMode sentenceSplittingMode) {
    this.sentenceSplittingMode = sentenceSplittingMode;
    this.sentenceSplittingModeString = null;
    return this;
  }

  /**
   * Sets the sentence splitting mode as a string, which is sent to the API as given in <code>
   * split_sentences</code>, for example <code>"nonewlines"</code>. Use this for values not yet in
   * {@link SentenceSplittingMode}.
   *
   * @see #setSentenceSplittingMode(SentenceSplittingMode)
   */
  public TextTranslationOptions setSentenceSplittingMode(String sentenceSplittingMode) {
    this.sentenceSplittingModeString = sentenceSplittingMode;
    this.sentenceSplittingMode = null;
    return this;
  }

  /**
   * Sets whether formatting should be preserved in translations. Set to <code>true</code> to
   * prevent the translation engine from correcting some formatting aspects, and instead leave the
   * formatting unchanged, default is <code>false</code>.
   */
  public TextTranslationOptions setPreserveFormatting(boolean preserveFormatting) {
    this.preserveFormatting = preserveFormatting;
    return this;
  }

  /**
   * Set the type of tags to parse before translation, only <code>"xml"</code> and <code>"html"
   * </code> are currently available. By default, this value is <code>null</code> and no
   * tag-handling is used.
   */
  public TextTranslationOptions setTagHandling(String tagHandling) {
    this.tagHandling = tagHandling;
    return this;
  }

  /**
   * Set the type of tags to parse before translation. By default, this value is <code>null</code>
   * and no tag-handling is used.
   *
   * @see TagHandlingMode
   */
  public TextTranslationOptions setTagHandling(TagHandlingMode tagHandling) {
    this.tagHandling = tagHandling != null ? tagHandling.getValue() : null;
    return this;
  }

  /**
   * Sets the version of tag handling algorithm to use. By default, this value is <code>null</code>
   * and the API default is used.
   */
  public TextTranslationOptions setTagHandlingVersion(String tagHandlingVersion) {
    this.tagHandlingVersion = tagHandlingVersion;
    return this;
  }

  /**
   * Sets the version of tag handling algorithm to use. By default, this value is <code>null</code>
   * and the API default is used.
   *
   * @see TagHandlingVersion
   */
  public TextTranslationOptions setTagHandlingVersion(TagHandlingVersion tagHandlingVersion) {
    this.tagHandlingVersion = tagHandlingVersion != null ? tagHandlingVersion.getValue() : null;
    return this;
  }

  /**
   * Set the type of model to use for a text translation. Currently supported values: <code>
   * "quality_optimized"</code> use a translation model that maximizes translation quality, at the
   * cost of response time. This option may be unavailable for some language pairs and the API would
   * respond with an error in this case; <code>"prefer_quality_optimized"</code> use the
   * highest-quality translation model for the given language pair; <code>"latency_optimized"
   * </code> use a translation model that minimizes response time, at the cost of translation
   * quality.
   */
  public TextTranslationOptions setModelType(String modelType) {
    this.modelType = modelType;
    return this;
  }

  /**
   * Set the type of model to use for a text translation. By default, this value is <code>null
   * </code> and the API default model type is used.
   *
   * @see ModelType
   */
  public TextTranslationOptions setModelType(ModelType modelType) {
    this.modelType = modelType != null ? modelType.getValue() : null;
    return this;
  }

  /**
   * Sets whether outline detection is used; set to <code>false</code> to disable automatic tag
   * detection, default is <code>true</code>.
   */
  public TextTranslationOptions setOutlineDetection(boolean outlineDetection) {
    this.outlineDetection = outlineDetection;
    return this;
  }

  /**
   * Sets the list of XML tags containing content that should not be translated. By default, this
   * value is <code>null</code> and no tags are specified.
   */
  public TextTranslationOptions setIgnoreTags(Iterable<String> ignoreTags) {
    this.ignoreTags = ignoreTags;
    return this;
  }

  /**
   * Sets the XML tags containing content that should not be translated, as a comma-separated string
   * that is sent as given, for example <code>
   * "a,b"</code>. By default, this value is <code>null</code> and no tags are specified.
   */
  public TextTranslationOptions setIgnoreTags(String ignoreTags) {
    this.ignoreTags = ignoreTags != null ? Collections.singletonList(ignoreTags) : null;
    return this;
  }

  /**
   * Sets the list of XML tags that should not be used to split text into sentences. By default,
   * this value is <code>null</code> and no tags are specified.
   */
  public TextTranslationOptions setNonSplittingTags(Iterable<String> nonSplittingTags) {
    this.nonSplittingTags = nonSplittingTags;
    return this;
  }

  /**
   * Sets the XML tags that should not be used to split text into sentences, as a comma-separated
   * string that is sent as given, for example <code>
   * "a,b"</code>. By default, this value is <code>null</code> and no tags are specified.
   */
  public TextTranslationOptions setNonSplittingTags(String nonSplittingTags) {
    this.nonSplittingTags =
        nonSplittingTags != null ? Collections.singletonList(nonSplittingTags) : null;
    return this;
  }

  /**
   * Set the list of XML tags that should be used to split text into sentences. By default, this
   * value is <code>null</code> and no tags are specified.
   */
  public TextTranslationOptions setSplittingTags(Iterable<String> splittingTags) {
    this.splittingTags = splittingTags;
    return this;
  }

  /**
   * Sets the XML tags that should be used to split text into sentences, as a comma-separated string
   * that is sent as given, for example <code>
   * "a,b"</code>. By default, this value is <code>null</code> and no tags are specified.
   */
  public TextTranslationOptions setSplittingTags(String splittingTags) {
    this.splittingTags = splittingTags != null ? Collections.singletonList(splittingTags) : null;
    return this;
  }

  /**
   * Sets custom instructions to influence translations. By default, this value is <code>null</code>
   * and no custom instructions are used.
   */
  public TextTranslationOptions setCustomInstructions(Iterable<String> customInstructions) {
    this.customInstructions = customInstructions;
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

  /** The sentence splitting mode set as a string, sent in place of the enum value. */
  String getSentenceSplittingModeString() {
    return sentenceSplittingModeString;
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

  /** Gets the current sentence splitting mode. */
  public SentenceSplittingMode getSentenceSplittingMode() {
    return sentenceSplittingMode;
  }

  /** Gets the current preserve formatting setting. */
  public boolean isPreserveFormatting() {
    return preserveFormatting;
  }

  /** Gets the current context. */
  public String getContext() {
    return context;
  }

  /** Gets the current model type. */
  public String getModelType() {
    return modelType;
  }

  /** Gets the current tag handling setting. */
  public String getTagHandling() {
    return tagHandling;
  }

  /** Gets the current tag handling version. */
  public String getTagHandlingVersion() {
    return tagHandlingVersion;
  }

  /** Gets the current outline detection setting. */
  public boolean isOutlineDetection() {
    return outlineDetection;
  }

  /** Gets the current ignore tags list. */
  public Iterable<String> getIgnoreTags() {
    return ignoreTags;
  }

  /** Gets the current non-splitting tags list. */
  public Iterable<String> getNonSplittingTags() {
    return nonSplittingTags;
  }

  /** Gets the current splitting tags list. */
  public Iterable<String> getSplittingTags() {
    return splittingTags;
  }

  /** Gets the current custom instructions list. */
  public Iterable<String> getCustomInstructions() {
    return customInstructions;
  }

  /** {@inheritDoc} */
  @Override
  public TextTranslationOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
