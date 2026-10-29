// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import org.jetbrains.annotations.Nullable;

/** Base class for all exceptions thrown by this library. */
public class DeepLException extends Exception {
  @Nullable private Integer httpStatusCode;

  public DeepLException(String message) {
    super(message);
  }

  public DeepLException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Returns the HTTP status code of the failed response, or <code>null</code> if the error did not
   * come from an HTTP response, for example a connection failure.
   */
  public @Nullable Integer getHttpStatusCode() {
    return httpStatusCode;
  }

  /**
   * Returns <code>true</code> if the error is transient and a retry could succeed: HTTP 429, HTTP
   * 5xx, and connection, DNS and timeout failures. The client already retried such errors up to the
   * configured maximum number of retries.
   */
  public boolean getShouldRetry() {
    return httpStatusCode != null && (httpStatusCode == 429 || httpStatusCode >= 500);
  }

  DeepLException setHttpStatusCode(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
    return this;
  }
}
