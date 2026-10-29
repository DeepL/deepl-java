// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options for {@link DeepLClient#updateSpokenTerms(String, UpdateSpokenTermsOptions)}. Only the
 * fields that are set are sent; a field left <code>null</code> keeps its current value.
 */
public class UpdateSpokenTermsOptions extends BaseRequestOptions {
  private @Nullable String name;
  private @Nullable List<SpokenTermsEntries> termLists;

  /** Sets the new name of the collection. */
  public UpdateSpokenTermsOptions setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets the term lists to merge into the collection, one {@link SpokenTermsEntries} per language.
   */
  public UpdateSpokenTermsOptions setTermLists(@Nullable List<SpokenTermsEntries> termLists) {
    this.termLists = termLists;
    return this;
  }

  /** Gets the current name. */
  public @Nullable String getName() {
    return name;
  }

  /** Gets the current term lists. */
  public @Nullable List<SpokenTermsEntries> getTermLists() {
    return termLists;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateSpokenTermsOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
