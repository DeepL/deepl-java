// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;

/**
 * Options for {@link DeepLAdminClient#renameDeveloperKey(String, String,
 * RenameDeveloperKeyOptions)}.
 */
public class RenameDeveloperKeyOptions extends BaseRequestOptions {
  /** {@inheritDoc} */
  @Override
  public RenameDeveloperKeyOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
