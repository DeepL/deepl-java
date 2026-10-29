// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Exception thrown when the specified glossary could not be found.
 *
 * @deprecated Catch {@link NotFoundException} instead, which is thrown for HTTP 404 on every
 *     endpoint. This subclass will be removed in the next major version.
 */
@Deprecated
public class GlossaryNotFoundException extends NotFoundException {
  public GlossaryNotFoundException(String message) {
    super(message);
  }
}
