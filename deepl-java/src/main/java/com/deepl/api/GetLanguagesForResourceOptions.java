// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Options for {@link DeepLClient#getLanguagesForResource(String, GetLanguagesForResourceOptions)}.
 */
public class GetLanguagesForResourceOptions {
  private @Nullable List<String> include;

  /**
   * Sets the include values, each sent as a separate <code>include</code> query parameter.
   *
   * @param include Include values, for example "beta" or "external", or <code>null</code> to return
   *     only stable languages and features.
   * @return This object, for convenience when chaining setters.
   */
  public GetLanguagesForResourceOptions setInclude(@Nullable List<String> include) {
    this.include = include == null ? null : new ArrayList<>(include);
    return this;
  }

  /**
   * Sets the include values, each sent as a separate <code>include</code> query parameter.
   *
   * @param include Include values, for example {@link LanguageInclude#Beta}.
   * @return This object, for convenience when chaining setters.
   */
  public GetLanguagesForResourceOptions setInclude(LanguageInclude... include) {
    List<String> values = new ArrayList<>();
    for (LanguageInclude value : include) {
      values.add(value.getValue());
    }
    this.include = values;
    return this;
  }

  /** @return The include values, or <code>null</code> if none are set. */
  public @Nullable List<String> getInclude() {
    return include;
  }
}
