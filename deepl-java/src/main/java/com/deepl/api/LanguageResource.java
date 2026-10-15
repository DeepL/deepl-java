// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Collections;
import java.util.Map;

/**
 * Information about a single DeepL resource and the features it supports, as returned by {@link
 * DeepLClient#getLanguageResources()}.
 */
public class LanguageResource {
  /** Resource value: text translation. */
  public static final String RESOURCE_TRANSLATE_TEXT = "translate_text";
  /** Resource value: document translation. */
  public static final String RESOURCE_TRANSLATE_DOCUMENT = "translate_document";
  /** Resource value: glossary. */
  public static final String RESOURCE_GLOSSARY = "glossary";
  /** Resource value: voice. */
  public static final String RESOURCE_VOICE = "voice";
  /** Resource value: write (rephrase). */
  public static final String RESOURCE_WRITE = "write";
  /** Resource value: style rules. */
  public static final String RESOURCE_STYLE_RULES = "style_rules";
  /** Resource value: translation memory. */
  public static final String RESOURCE_TRANSLATION_MEMORY = "translation_memory";

  private final String resource;
  private final Map<String, ResourceFeature> features;

  /**
   * Initializes a new LanguageResource object.
   *
   * @param resource Name of the resource, for example "translate_text".
   * @param features Map of feature name to {@link ResourceFeature}.
   */
  public LanguageResource(String resource, Map<String, ResourceFeature> features) {
    this.resource = resource;
    this.features = features;
  }

  /** @return Name of the resource, for example "translate_text". */
  public String getResource() {
    return resource;
  }

  /**
   * @return Map of feature name to {@link ResourceFeature} for each feature the resource supports,
   *     including whether it requires source and/or target language support.
   */
  public Map<String, ResourceFeature> getFeatures() {
    return Collections.unmodifiableMap(features);
  }

  /**
   * Checks whether this resource exposes the given feature.
   *
   * @param feature Feature name, for example {@link LanguageFeature#FEATURE_GLOSSARY}.
   * @return <code>true</code> if the feature is exposed by the resource.
   */
  public boolean supports(String feature) {
    return features.containsKey(feature);
  }
}
