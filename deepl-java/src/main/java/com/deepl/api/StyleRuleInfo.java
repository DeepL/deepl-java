// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.util.*;
import org.jetbrains.annotations.*;

/** Information about a style rule list. */
public class StyleRuleInfo {
  @SerializedName(value = "style_id")
  private final String styleId;

  @SerializedName(value = "name")
  private final String name;

  @SerializedName(value = "creation_time")
  private final Date creationTime;

  @SerializedName(value = "updated_time")
  private final Date updatedTime;

  @SerializedName(value = "language")
  private final String language;

  @SerializedName(value = "version")
  private final int version;

  @SerializedName(value = "configured_rules")
  @Nullable
  private final Map<String, Map<String, String>> configuredRules;

  @SerializedName(value = "custom_instructions")
  @Nullable
  private final List<CustomInstruction> customInstructions;

  /**
   * Initializes a new {@link StyleRuleInfo} containing information about a style rule list.
   *
   * @param styleId Unique ID assigned to the style rule list.
   * @param name User-defined name assigned to the style rule list.
   * @param creationTime Timestamp when the style rule list was created.
   * @param updatedTime Timestamp when the style rule list was last updated.
   * @param language Language code for the style rule list.
   * @param version Version number of the style rule list.
   * @param configuredRules The predefined rules that have been enabled, as a map from category to
   *     rule to value.
   * @param customInstructions Optional list of custom instructions.
   */
  public StyleRuleInfo(
      String styleId,
      String name,
      Date creationTime,
      Date updatedTime,
      String language,
      int version,
      @Nullable Map<String, Map<String, String>> configuredRules,
      @Nullable List<CustomInstruction> customInstructions) {
    this.styleId = styleId;
    this.name = name;
    this.creationTime = creationTime;
    this.updatedTime = updatedTime;
    this.language = language;
    this.version = version;
    this.configuredRules = configuredRules;
    this.customInstructions = customInstructions;
  }

  /** @return Unique ID assigned to the style rule list. */
  public String getStyleId() {
    return styleId;
  }

  /** @return User-defined name assigned to the style rule list. */
  public String getName() {
    return name;
  }

  /** @return Timestamp when the style rule list was created. */
  public Date getCreationTime() {
    return creationTime;
  }

  /** @return Timestamp when the style rule list was last updated. */
  public Date getUpdatedTime() {
    return updatedTime;
  }

  /** @return Language code for the style rule list. */
  public String getLanguage() {
    return language;
  }

  /** @return Version number of the style rule list. */
  public int getVersion() {
    return version;
  }

  /**
   * @return The predefined rules that have been enabled, as a map from category to rule to value in
   *     the API's snake_case form (e.g. {@code "dates_and_times" -> {"calendar_era" ->
   *     "use_bc_and_ad"}}), as sent by the API.
   */
  @Nullable
  public Map<String, Map<String, String>> getConfiguredRules() {
    return configuredRules;
  }

  /** @return Optional list of custom instructions. */
  @Nullable
  public List<CustomInstruction> getCustomInstructions() {
    return customInstructions;
  }
}
