// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.http;

public class HttpResponse {

  private final int code;

  private final String body;

  private final boolean retriedOnlyIfUnsent;

  public HttpResponse(int code, String body) {
    this(code, body, false);
  }

  /**
   * @param retriedOnlyIfUnsent True if the request was retried only when it was not sent, as for a
   *     request that creates a resource; a server error response is then not reported as retryable.
   */
  public HttpResponse(int code, String body, boolean retriedOnlyIfUnsent) {
    this.code = code;
    this.body = body;
    this.retriedOnlyIfUnsent = retriedOnlyIfUnsent;
  }

  /** True if the request was retried only when it was not sent, as for a create request. */
  public boolean isRetriedOnlyIfUnsent() {
    return retriedOnlyIfUnsent;
  }

  public int getCode() {
    return code;
  }

  public String getBody() {
    return body;
  }
}
