// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Type of tags to parse before text translation. {@link TextTranslationOptions#setTagHandling} also
 * accepts any string, for values added to the API later.
 */
public enum TagHandlingMode {
  /** Parse the text as HTML. */
  Html("html"),

  /** Parse the text as XML. */
  Xml("xml");

  private final String value;

  TagHandlingMode(String value) {
    this.value = value;
  }

  /** The value sent to the API. */
  public String getValue() {
    return value;
  }
}
