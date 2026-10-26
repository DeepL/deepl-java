// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Optional fields of a style profile, used by {@link DeepLClient#createStyleProfile(String,
 * StyleProfileOptions)} and {@link DeepLClient#updateStyleProfile(String, String,
 * StyleProfileOptions)}.
 *
 * <p>A field left <code>null</code> is not sent, so an update keeps its current value. An empty
 * string clears the description or industry; an empty list unlinks all components of that kind. A
 * list that is sent replaces the linked list in full.
 */
public class StyleProfileOptions {
  @SerializedName(value = "description")
  private @Nullable String description;

  @SerializedName(value = "industry")
  private @Nullable String industry;

  @SerializedName(value = "glossary_ids")
  private @Nullable List<String> glossaryIds;

  @SerializedName(value = "style_rule_ids")
  private @Nullable List<String> styleRuleIds;

  @SerializedName(value = "translation_memory_ids")
  private @Nullable List<String> translationMemoryIds;

  @SerializedName(value = "glossary_priority_order_enabled")
  private @Nullable Boolean glossaryPriorityOrderEnabled;

  /** Sets the description of the style profile; an empty string clears it. */
  public StyleProfileOptions setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  /** Sets the industry of the style profile, for example "Legal"; an empty string clears it. */
  public StyleProfileOptions setIndustry(@Nullable String industry) {
    this.industry = industry;
    return this;
  }

  /**
   * Sets the IDs of the glossaries to link, in priority order; an empty list unlinks all
   * glossaries.
   */
  public StyleProfileOptions setGlossaryIds(@Nullable List<String> glossaryIds) {
    this.glossaryIds = glossaryIds;
    return this;
  }

  /** Sets the IDs of the style rules to link; an empty list unlinks all style rules. */
  public StyleProfileOptions setStyleRuleIds(@Nullable List<String> styleRuleIds) {
    this.styleRuleIds = styleRuleIds;
    return this;
  }

  /**
   * Sets the IDs of the translation memories to link; an empty list unlinks all translation
   * memories.
   */
  public StyleProfileOptions setTranslationMemoryIds(@Nullable List<String> translationMemoryIds) {
    this.translationMemoryIds = translationMemoryIds;
    return this;
  }

  /** Sets whether the linked glossaries are applied in the order of the glossary IDs. */
  public StyleProfileOptions setGlossaryPriorityOrderEnabled(
      @Nullable Boolean glossaryPriorityOrderEnabled) {
    this.glossaryPriorityOrderEnabled = glossaryPriorityOrderEnabled;
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
}
