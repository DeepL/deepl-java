// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;

/**
 * Options for {@link DeepLClient#correctText} overloads. There are no correction-specific options
 * yet; use {@link #setExtraBodyParameters} to send additional request parameters.
 */
public class CorrectTextOptions extends BaseRequestOptions {
  /**
   * Sets additional parameters to pass in the body of the HTTP request. They are added last and
   * replace built-in parameters with the same name.
   *
   * @param extraBodyParameters Map of additional parameters to include in the request.
   * @return This options object for method chaining.
   */
  @Override
  public CorrectTextOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
