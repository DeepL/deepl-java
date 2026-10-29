// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Version of the tag handling algorithm. {@link TextTranslationOptions#setTagHandlingVersion} also
 * accepts any string, for values added to the API later.
 */
public enum TagHandlingVersion {
  /** Version 1 of the tag handling algorithm. */
  V1("v1"),

  /** Version 2 of the tag handling algorithm. */
  V2("v2");

  private final String value;

  TagHandlingVersion(String value) {
    this.value = value;
  }

  /** The value sent to the API. */
  public String getValue() {
    return value;
  }
}
