// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

/** Options for {@link DeepLAdminClient#createDeveloperKey(CreateDeveloperKeyOptions)}. */
public class CreateDeveloperKeyOptions extends BaseRequestOptions {
  private @Nullable String label;

  /**
   * Sets the label of the new key.
   *
   * @param label Label for the new key, or <code>null</code> to let the API use "DeepL API Key".
   * @return This object, for convenience when chaining setters.
   */
  public CreateDeveloperKeyOptions setLabel(@Nullable String label) {
    this.label = label;
    return this;
  }

  /** Gets the current label. */
  public @Nullable String getLabel() {
    return label;
  }

  /** {@inheritDoc} */
  @Override
  public CreateDeveloperKeyOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
