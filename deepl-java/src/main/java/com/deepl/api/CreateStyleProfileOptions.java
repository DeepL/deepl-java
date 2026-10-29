// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * Options for {@link DeepLClient#createStyleProfile(String, CreateStyleProfileOptions)}, also the
 * base of {@link UpdateStyleProfileOptions}.
 *
 * <p>A field left <code>null</code> is not sent, so an update keeps its current value. An empty
 * string clears the description or industry; an empty list unlinks all components of that kind. A
 * list that is sent replaces the linked list in full.
 */
public class CreateStyleProfileOptions extends BaseRequestOptions {
  private @Nullable String description;
  private @Nullable String industry;
  private @Nullable List<String> glossaryIds;
  private @Nullable List<String> styleRuleIds;
  private @Nullable List<String> translationMemoryIds;
  private @Nullable Boolean glossaryPriorityOrderEnabled;

  /** Sets the description of the style profile; an empty string clears it. */
  public CreateStyleProfileOptions setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  /** Sets the industry of the style profile, for example "Legal"; an empty string clears it. */
  public CreateStyleProfileOptions setIndustry(@Nullable String industry) {
    this.industry = industry;
    return this;
  }

  /**
   * Sets the IDs of the glossaries to link, in priority order; an empty list unlinks all
   * glossaries.
   */
  public CreateStyleProfileOptions setGlossaryIds(@Nullable List<String> glossaryIds) {
    this.glossaryIds = glossaryIds;
    return this;
  }

  /**
   * Sets the glossaries to link, in priority order, as {@link GlossaryInfo} or {@link
   * MultilingualGlossaryInfo} objects; their IDs are sent.
   */
  public CreateStyleProfileOptions setGlossaryIds(IGlossary first, IGlossary... more) {
    this.glossaryIds = idsOf(first, more, IGlossary::getGlossaryId, "glossaries");
    return this;
  }

  /** Sets the IDs of the style rules to link; an empty list unlinks all style rules. */
  public CreateStyleProfileOptions setStyleRuleIds(@Nullable List<String> styleRuleIds) {
    this.styleRuleIds = styleRuleIds;
    return this;
  }

  /** Sets the style rules to link as {@link StyleRuleInfo} objects; their IDs are sent. */
  public CreateStyleProfileOptions setStyleRuleIds(StyleRuleInfo first, StyleRuleInfo... more) {
    this.styleRuleIds = idsOf(first, more, StyleRuleInfo::getStyleId, "style rules");
    return this;
  }

  /**
   * Sets the IDs of the translation memories to link; an empty list unlinks all translation
   * memories.
   */
  public CreateStyleProfileOptions setTranslationMemoryIds(
      @Nullable List<String> translationMemoryIds) {
    this.translationMemoryIds = translationMemoryIds;
    return this;
  }

  /**
   * Sets the translation memories to link as {@link TranslationMemoryInfo} objects; their IDs are
   * sent.
   */
  public CreateStyleProfileOptions setTranslationMemoryIds(
      TranslationMemoryInfo first, TranslationMemoryInfo... more) {
    this.translationMemoryIds =
        idsOf(first, more, TranslationMemoryInfo::getTranslationMemoryId, "translation memories");
    return this;
  }

  /** Sets whether the linked glossaries are applied in the order of the glossary IDs. */
  public CreateStyleProfileOptions setGlossaryPriorityOrderEnabled(
      @Nullable Boolean glossaryPriorityOrderEnabled) {
    this.glossaryPriorityOrderEnabled = glossaryPriorityOrderEnabled;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public CreateStyleProfileOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }

  /** Gets the current description. */
  public @Nullable String getDescription() {
    return description;
  }

  /** Gets the current industry. */
  public @Nullable String getIndustry() {
    return industry;
  }

  /** Gets the current glossary IDs. */
  public @Nullable List<String> getGlossaryIds() {
    return glossaryIds;
  }

  /** Gets the current style rule IDs. */
  public @Nullable List<String> getStyleRuleIds() {
    return styleRuleIds;
  }

  /** Gets the current translation memory IDs. */
  public @Nullable List<String> getTranslationMemoryIds() {
    return translationMemoryIds;
  }

  /** Gets the current glossary priority order setting. */
  public @Nullable Boolean getGlossaryPriorityOrderEnabled() {
    return glossaryPriorityOrderEnabled;
  }

  // The separate first parameter keeps a zero-argument call off the object overloads.
  private static <T> List<String> idsOf(
      T first, T[] more, Function<T, String> getId, String description) {
    List<String> ids = new ArrayList<>();
    ids.add(idOf(first, getId, description));
    for (T item : more) {
      ids.add(idOf(item, getId, description));
    }
    return ids;
  }

  private static <T> String idOf(@Nullable T item, Function<T, String> getId, String description) {
    if (item == null) {
      throw new IllegalArgumentException(description + " must not contain null");
    }
    return getId.apply(item);
  }
}
