// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Information about a style profile: its name, optional description and industry, and the IDs of
 * the glossaries, style rules and translation memories it links.
 */
public class StyleProfile {
  @SerializedName(value = "style_profile_id")
  private final String styleProfileId;

  @SerializedName(value = "name")
  private final String name;

  @SerializedName(value = "description")
  private final @Nullable String description;

  @SerializedName(value = "industry")
  private final @Nullable String industry;

  @SerializedName(value = "glossary_ids")
  private final @Nullable List<String> glossaryIds;

  @SerializedName(value = "style_rule_ids")
  private final @Nullable List<String> styleRuleIds;

  @SerializedName(value = "translation_memory_ids")
  private final @Nullable List<String> translationMemoryIds;

  @SerializedName(value = "glossary_priority_order_enabled")
  private final boolean glossaryPriorityOrderEnabled;

  @SerializedName(value = "creation_time")
  private final @Nullable Date creationTime;

  @SerializedName(value = "updated_time")
  private final @Nullable Date updatedTime;

  /**
   * Initializes a new {@link StyleProfile} object.
   *
   * @param styleProfileId ID of the style profile.
   * @param name Name of the style profile.
   * @param description Description of the style profile, or <code>null</code> if it has none.
   * @param industry Industry of the style profile, or <code>null</code> if it has none.
   * @param glossaryIds IDs of the linked glossaries.
   * @param styleRuleIds IDs of the linked style rules.
   * @param translationMemoryIds IDs of the linked translation memories.
   * @param glossaryPriorityOrderEnabled Whether the linked glossaries are applied in list order.
   * @param creationTime Time the style profile was created, or <code>null</code>.
   * @param updatedTime Time the style profile was last updated, or <code>null</code>.
   */
  public StyleProfile(
      String styleProfileId,
      String name,
      @Nullable String description,
      @Nullable String industry,
      @Nullable List<String> glossaryIds,
      @Nullable List<String> styleRuleIds,
      @Nullable List<String> translationMemoryIds,
      boolean glossaryPriorityOrderEnabled,
      @Nullable Date creationTime,
      @Nullable Date updatedTime) {
    this.styleProfileId = styleProfileId;
    this.name = name;
    this.description = description;
    this.industry = industry;
    this.glossaryIds = glossaryIds;
    this.styleRuleIds = styleRuleIds;
    this.translationMemoryIds = translationMemoryIds;
    this.glossaryPriorityOrderEnabled = glossaryPriorityOrderEnabled;
    this.creationTime = creationTime;
    this.updatedTime = updatedTime;
  }

  /** @return ID of the style profile. */
  public String getStyleProfileId() {
    return styleProfileId;
  }

  /** @return Name of the style profile. */
  public String getName() {
    return name;
  }

  /** @return Description of the style profile, or <code>null</code> if it has none. */
  public @Nullable String getDescription() {
    return description;
  }

  /** @return Industry of the style profile, or <code>null</code> if it has none. */
  public @Nullable String getIndustry() {
    return industry;
  }

  /** @return IDs of the linked glossaries, in priority order; empty if none are linked. */
  public List<String> getGlossaryIds() {
    return glossaryIds != null ? glossaryIds : Collections.emptyList();
  }

  /** @return IDs of the linked style rules; empty if none are linked. */
  public List<String> getStyleRuleIds() {
    return styleRuleIds != null ? styleRuleIds : Collections.emptyList();
  }

  /** @return IDs of the linked translation memories; empty if none are linked. */
  public List<String> getTranslationMemoryIds() {
    return translationMemoryIds != null ? translationMemoryIds : Collections.emptyList();
  }

  /**
   * @return Whether the linked glossaries are applied in the order of {@link #getGlossaryIds()}.
   */
  public boolean isGlossaryPriorityOrderEnabled() {
    return glossaryPriorityOrderEnabled;
  }

  /** @return Time the style profile was created, or <code>null</code> if unavailable. */
  public @Nullable Date getCreationTime() {
    return creationTime;
  }

  /** @return Time the style profile was last updated, or <code>null</code> if unavailable. */
  public @Nullable Date getUpdatedTime() {
    return updatedTime;
  }

  @Override
  public String toString() {
    return "StyleProfile{styleProfileId='"
        + styleProfileId
        + "', name='"
        + name
        + "', description="
        + (description != null ? "'" + description + "'" : null)
        + ", industry="
        + (industry != null ? "'" + industry + "'" : null)
        + ", glossaryIds="
        + getGlossaryIds()
        + ", styleRuleIds="
        + getStyleRuleIds()
        + ", translationMemoryIds="
        + getTranslationMemoryIds()
        + ", glossaryPriorityOrderEnabled="
        + glossaryPriorityOrderEnabled
        + ", creationTime="
        + creationTime
        + ", updatedTime="
        + updatedTime
        + '}';
  }
}
