// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/** Exception thrown when a connection error occurs while accessing the DeepL API. */
public class ConnectionException extends DeepLException {
  private final boolean shouldRetry;
  private final boolean requestNotSent;

  public ConnectionException(String message, boolean shouldRetry, Throwable cause) {
    this(message, shouldRetry, cause, false);
  }

  ConnectionException(
      String message, boolean shouldRetry, Throwable cause, boolean requestNotSent) {
    super(message, cause);
    this.shouldRetry = shouldRetry;
    this.requestNotSent = requestNotSent;
  }

  /** True if the failure happened before the request was sent, so a retry cannot duplicate it. */
  boolean getRequestNotSent() {
    return requestNotSent;
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
