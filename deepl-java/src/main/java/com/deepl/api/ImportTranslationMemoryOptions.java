// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.time.Duration;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options for {@link DeepLClient#importTranslationMemoryFromFilepath(java.io.File,
 * ImportTranslationMemoryOptions)}.
 */
public class ImportTranslationMemoryOptions extends BaseRequestOptions {
  private @Nullable String displayName;
  private @Nullable String contentType;
  private @Nullable Duration timeout;

  /**
   * Sets the name of the resulting translation memory. By default, this value is <code>null</code>
   * and the API names the translation memory after the file.
   */
  public ImportTranslationMemoryOptions setDisplayName(@Nullable String displayName) {
    this.displayName = displayName;
    return this;
  }

  /** Gets the current display name. */
  public @Nullable String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the MIME type of the file, declared when the import job is created and sent with the
   * upload. By default, this value is <code>null</code>: the type is not declared and the file is
   * uploaded as "application/xml".
   */
  public ImportTranslationMemoryOptions setContentType(@Nullable String contentType) {
    this.contentType = contentType;
    return this;
  }

  /** Gets the current content type. */
  public @Nullable String getContentType() {
    return contentType;
  }

  /**
   * Sets the maximum time to wait for the import to finish. By default, this value is <code>null
   * </code> and the import is awaited indefinitely. Note that this is not accurate to the
   * millisecond, as the job status is only polled every 5 seconds.
   */
  public ImportTranslationMemoryOptions setTimeout(@Nullable Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  /** Gets the current timeout. */
  public @Nullable Duration getTimeout() {
    return timeout;
  }

  /** {@inheritDoc} */
  @Override
  public ImportTranslationMemoryOptions setExtraBodyParameters(
      Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
