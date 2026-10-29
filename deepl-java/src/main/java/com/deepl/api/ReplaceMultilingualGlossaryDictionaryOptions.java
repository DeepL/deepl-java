// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;

/**
 * Options for {@link DeepLClient#replaceMultilingualGlossaryDictionary(String,
 * MultilingualGlossaryDictionaryEntries, ReplaceMultilingualGlossaryDictionaryOptions)} and {@code
 * replaceMultilingualGlossaryDictionaryFromCsv}.
 */
public class ReplaceMultilingualGlossaryDictionaryOptions extends BaseRequestOptions {
  /** {@inheritDoc} */
  @Override
  public ReplaceMultilingualGlossaryDictionaryOptions setExtraBodyParameters(
      Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
