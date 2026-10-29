// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options for {@link DeepLClient#createTranslationMemoryImport(String, long,
 * CreateTranslationMemoryImportOptions)}.
 */
public class CreateTranslationMemoryImportOptions extends BaseRequestOptions {
  private @Nullable String contentType;
  private @Nullable String displayName;

  /**
   * Sets the MIME type of the file to import. By default, this value is <code>null</code> and the
   * API default "application/xml" is used.
   */
  public CreateTranslationMemoryImportOptions setContentType(@Nullable String contentType) {
    this.contentType = contentType;
    return this;
  }

  /** Gets the current content type. */
  public @Nullable String getContentType() {
    return contentType;
  }

  /**
   * Sets the name of the resulting translation memory. By default, this value is <code>null</code>
   * and the API names the translation memory after the file.
   */
  public CreateTranslationMemoryImportOptions setDisplayName(@Nullable String displayName) {
    this.displayName = displayName;
    return this;
  }

  /** Gets the current display name. */
  public @Nullable String getDisplayName() {
    return displayName;
  }

  /** {@inheritDoc} */
  @Override
  public CreateTranslationMemoryImportOptions setExtraBodyParameters(
      Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }
}
