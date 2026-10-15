// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

/**
 * A feature supported by a DeepL resource, as returned by {@link
 * DeepLClient#getLanguageResources()}.
 */
public class ResourceFeature {
  private final String name;

  @SerializedName("needs_source_support")
  @Nullable
  private final Boolean needsSourceSupport;

  @SerializedName("needs_target_support")
  @Nullable
  private final Boolean needsTargetSupport;

  /**
   * Initializes a new ResourceFeature object.
   *
   * @param name Feature name, for example "glossary" or "formality".
   * @param needsSourceSupport Whether the source language must support the feature.
   * @param needsTargetSupport Whether the target language must support the feature.
   */
  public ResourceFeature(
      String name, @Nullable Boolean needsSourceSupport, @Nullable Boolean needsTargetSupport) {
    this.name = name;
    this.needsSourceSupport = needsSourceSupport;
    this.needsTargetSupport = needsTargetSupport;
  }

  /** @return Feature name, for example "glossary" or "formality". */
  public String getName() {
    return name;
  }

  /**
   * @return <code>true</code> if the source language must support this feature, <code>false
   *     </code> if not, or <code>null</code> if the API omitted the flag (the API treats this as
   *     <code>false</code>).
   */
  @Nullable
  public Boolean getNeedsSourceSupport() {
    return needsSourceSupport;
  }

  /**
   * @return <code>true</code> if the target language must support this feature, <code>false
   *     </code> if not, or <code>null</code> if the API omitted the flag (the API treats this as
   *     <code>false</code>).
   */
  @Nullable
  public Boolean getNeedsTargetSupport() {
    return needsTargetSupport;
  }
}
