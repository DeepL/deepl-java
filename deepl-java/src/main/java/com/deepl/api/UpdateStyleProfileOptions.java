// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options for {@link DeepLClient#updateStyleProfile(String, UpdateStyleProfileOptions)}: the fields
 * of {@link CreateStyleProfileOptions} plus the name. Only the fields that are set are sent; a
 * field left <code>null</code> keeps its current value.
 */
public class UpdateStyleProfileOptions extends CreateStyleProfileOptions {
  private @Nullable String name;

  /** Sets the new name of the style profile. */
  public UpdateStyleProfileOptions setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  /** Gets the current name. */
  public @Nullable String getName() {
    return name;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setDescription(@Nullable String description) {
    super.setDescription(description);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setIndustry(@Nullable String industry) {
    super.setIndustry(industry);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setGlossaryIds(@Nullable List<String> glossaryIds) {
    super.setGlossaryIds(glossaryIds);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setGlossaryIds(IGlossary first, IGlossary... more) {
    super.setGlossaryIds(first, more);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setStyleRuleIds(@Nullable List<String> styleRuleIds) {
    super.setStyleRuleIds(styleRuleIds);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setStyleRuleIds(StyleRuleInfo first, StyleRuleInfo... more) {
    super.setStyleRuleIds(first, more);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setTranslationMemoryIds(
      @Nullable List<String> translationMemoryIds) {
    super.setTranslationMemoryIds(translationMemoryIds);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setTranslationMemoryIds(
      TranslationMemoryInfo first, TranslationMemoryInfo... more) {
    super.setTranslationMemoryIds(first, more);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setGlossaryPriorityOrderEnabled(
      @Nullable Boolean glossaryPriorityOrderEnabled) {
    super.setGlossaryPriorityOrderEnabled(glossaryPriorityOrderEnabled);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateStyleProfileOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
