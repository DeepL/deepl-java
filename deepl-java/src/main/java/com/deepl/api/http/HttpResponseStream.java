// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.http;

import com.deepl.api.*;
import com.deepl.api.utils.*;
import java.io.*;
import org.jetbrains.annotations.*;

public class HttpResponseStream implements AutoCloseable {

  private final int code;

  @Nullable private final InputStream body;

  public HttpResponseStream(int code, @Nullable InputStream body) {
    this.code = code;
    this.body = body;
  }

  public void close() {
    try {
      if (this.body != null) {
        this.body.close();
      }
    } catch (Exception e) {
      // ignore
    }
  }

  public HttpResponse toStringResponse() throws DeepLException {
    return toStringResponse(false);
  }

  /**
   * Reads the body into an {@link HttpResponse}.
   *
   * @param retriedOnlyIfUnsent See {@link HttpResponse#isRetriedOnlyIfUnsent()}.
   */
  public HttpResponse toStringResponse(boolean retriedOnlyIfUnsent) throws DeepLException {
    try {
      String content = this.body == null ? "" : StreamUtil.readStream(this.body);
      return new HttpResponse(getCode(), content, retriedOnlyIfUnsent);
    } catch (IOException exception) {
      throw new DeepLException("Error reading stream", exception);
    } finally {
      close();
    }
  }

  public int getCode() {
    return code;
  }

  public @Nullable InputStream getBody() {
    return body;
  }
}
