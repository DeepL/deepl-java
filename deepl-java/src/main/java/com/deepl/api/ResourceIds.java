// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/** Checks the resource IDs of the translation options. */
final class ResourceIds {
  private ResourceIds() {}

  /** An ID given as a string: null leaves the option unset, an empty ID is rejected. */
  static @Nullable String optional(String name, @Nullable String id) {
    if (id != null && id.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty");
    }
    return id;
  }

  /** The ID of a resource object, which must have one. */
  static String ofObject(String name, @Nullable String id) {
    if (id == null || id.isEmpty()) {
      throw new IllegalArgumentException(name + " has no ID");
    }
    return id;
  }

  /**
   * A copy of the glossary IDs: null leaves the option unset, a null or empty entry is rejected.
   */
  static @Nullable List<String> list(@Nullable List<String> ids) {
    if (ids == null) {
      return null;
    }
    for (String id : ids) {
      Objects.requireNonNull(id, "glossaryIds");
      if (id.isEmpty()) {
        throw new IllegalArgumentException("glossaryIds must not contain an empty ID");
      }
    }
    return new ArrayList<>(ids);
  }
}
