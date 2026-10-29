// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/** Exception thrown when a connection error occurs while accessing the DeepL API. */
public class ConnectionException extends DeepLException {
  private final boolean shouldRetry;

  public ConnectionException(String message, boolean shouldRetry, Throwable cause) {
    super(message, cause);
    this.shouldRetry = shouldRetry;
  }

  /**
   * Returns <code>true</code> if this exception occurred due to transient condition and the request
   * should be retried, otherwise <code>false</code>.
   */
  @Override
  public boolean getShouldRetry() {
    return shouldRetry;
  }
}
