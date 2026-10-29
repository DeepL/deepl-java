// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when an error occurs during {@link Translator#translateDocument}. If the error
 * occurs after the document was successfully uploaded, the {@link DocumentHandle} for the
 * associated document is included, to allow later retrieval of the document.
 */
public class DocumentTranslationException extends DeepLException {

  private final @Nullable DocumentHandle handle;

  public DocumentTranslationException(
      String message, Throwable throwable, @Nullable DocumentHandle handle) {
    super(message, throwable);
    this.handle = handle;
  }

  /**
   * Get the handle to the in-progress document translation, or <code>null</code> if an error
   * occurred before uploading the document.
   */
  public @Nullable DocumentHandle getHandle() {
    return handle;
  }

  /** Returns the HTTP status code of the underlying error, if it came from an HTTP response. */
  @Override
  public @Nullable Integer getHttpStatusCode() {
    Throwable cause = getCause();
    return cause instanceof DeepLException
        ? ((DeepLException) cause).getHttpStatusCode()
        : super.getHttpStatusCode();
  }

  /** Returns whether a retry of the underlying failed request could succeed. */
  @Override
  public boolean getShouldRetry() {
    Throwable cause = getCause();
    return cause instanceof DeepLException
        ? ((DeepLException) cause).getShouldRetry()
        : super.getShouldRetry();
  }
}
