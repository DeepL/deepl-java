// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;

/**
 * Options for {@link DeepLClient#createSpokenTerms(String, java.util.List,
 * CreateSpokenTermsOptions)}.
 */
public class CreateSpokenTermsOptions extends BaseRequestOptions {
  /** {@inheritDoc} */
  @Override
  public CreateSpokenTermsOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
