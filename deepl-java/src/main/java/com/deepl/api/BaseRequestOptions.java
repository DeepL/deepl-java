// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;

/** Base class for request options providing common functionality for all endpoints. */
public abstract class BaseRequestOptions {
  private Map<String, ?> extraBodyParameters;

  /**
   * Sets additional parameters to pass in the body of the HTTP request. Can be used to access beta
   * features, override built-in parameters, or for testing purposes. Keys in this map will be added
   * to the request body and can override existing keys.
   *
   * <p>In JSON request bodies each value is sent as its JSON form, so a {@link Boolean}, {@link
   * Number}, {@link java.util.List} or {@link Map} keeps its type. In form-encoded and multipart
   * bodies a collection is sent comma-separated and any other value as its {@code toString()}. A
   * {@code null} value removes the key from the request.
   *
   * @param extraBodyParameters Map of additional parameters to include in the request.
   * @return This options object for method chaining.
   */
  public BaseRequestOptions setExtraBodyParameters(Map<String, ?> extraBodyParameters) {
    this.extraBodyParameters = extraBodyParameters;
    return this;
  }

  /** Gets the current extra body parameters. */
  public Map<String, ?> getExtraBodyParameters() {
    return extraBodyParameters;
  }
}
