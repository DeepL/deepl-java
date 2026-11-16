// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
   * @throws NullPointerException If an include value is <code>null</code>.
   */
  public GetLanguagesForResourceOptions setInclude(@Nullable List<String> include) {
    if (include == null) {
      this.include = null;
      return this;
    }
    List<String> values = new ArrayList<>();
    for (String value : include) {
      values.add(Objects.requireNonNull(value, "include"));
    }
    this.include = values;
    return this;
  }

  /**
   * Sets the include values, each sent as a separate <code>include</code> query parameter.
   *
   * @param include Include values, for example {@link LanguageInclude#Beta}, or <code>null</code>
   *     to return only stable languages and features.
   * @return This object, for convenience when chaining setters.
   * @throws NullPointerException If an include value is <code>null</code>.
   */
  public GetLanguagesForResourceOptions setInclude(@Nullable LanguageInclude... include) {
    if (include == null) {
      this.include = null;
      return this;
    }
    List<String> values = new ArrayList<>();
    for (LanguageInclude value : include) {
      values.add(Objects.requireNonNull(value, "include").getValue());
    }
    this.include = values;
    return this;
  }

  /** @return The include values, or <code>null</code> if none are set. */
  public @Nullable List<String> getInclude() {
    return include;
  }
}
