// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Type of translation model to use for text translation. {@link
 * TextTranslationOptions#setModelType} also accepts any string, for values added to the API later.
 */
public enum ModelType {
  /**
   * Use a translation model that maximizes translation quality, at the cost of response time. The
   * API responds with an error for language pairs where it is unavailable.
   */
  QualityOptimized("quality_optimized"),

  /** Use the highest-quality translation model available for the language pair. */
  PreferQualityOptimized("prefer_quality_optimized"),

  /** Use a translation model that minimizes response time, at the cost of translation quality. */
  LatencyOptimized("latency_optimized");

  private final String value;

  ModelType(String value) {
    this.value = value;
  }

  /** The value sent to the API. */
  public String getValue() {
    return value;
  }
}
