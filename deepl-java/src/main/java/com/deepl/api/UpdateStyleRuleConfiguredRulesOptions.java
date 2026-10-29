// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;

/**
 * Options for {@link DeepLClient#updateStyleRuleConfiguredRules(String, java.util.Map,
 * UpdateStyleRuleConfiguredRulesOptions)}. The request body is the configured-rules map itself; the
 * extra body parameters are added to it as further top-level keys.
 */
public class UpdateStyleRuleConfiguredRulesOptions extends BaseRequestOptions {
  /** {@inheritDoc} */
  @Override
  public UpdateStyleRuleConfiguredRulesOptions setExtraBodyParameters(
      Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
