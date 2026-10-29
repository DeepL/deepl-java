// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options for {@link DeepLClient#updateMultilingualGlossary(String,
 * UpdateMultilingualGlossaryOptions)}. Only the fields that are set are sent; a field left <code>
 * null</code> keeps its current value.
 */
public class UpdateMultilingualGlossaryOptions extends BaseRequestOptions {
  private @Nullable String name;
  private @Nullable List<MultilingualGlossaryDictionaryEntries> dictionaries;

  /** Sets the new name of the glossary. */
  public UpdateMultilingualGlossaryOptions setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets the dictionaries to merge into the glossary. The entries of each dictionary are merged
   * into the glossary dictionary with the same language pair, which is created if missing.
   */
  public UpdateMultilingualGlossaryOptions setDictionaries(
      @Nullable List<MultilingualGlossaryDictionaryEntries> dictionaries) {
    this.dictionaries = dictionaries;
    return this;
  }

  /** Gets the current name. */
  public @Nullable String getName() {
    return name;
  }

  /** Gets the current dictionaries. */
  public @Nullable List<MultilingualGlossaryDictionaryEntries> getDictionaries() {
    return dictionaries;
  }

  /** {@inheritDoc} */
  @Override
  public UpdateMultilingualGlossaryOptions setExtraBodyParameters(
      Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
