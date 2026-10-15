// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Information about a language supported by the DeepL API for a given resource, as returned by
 * {@link DeepLClient#getLanguagesForResource(String, java.util.List)}.
 */
public class LanguageSupport {
  /** Include value: also return beta languages and features. */
  public static final String INCLUDE_BETA = "beta";
  /** Include value: also return features that rely on third-party service providers. */
  public static final String INCLUDE_EXTERNAL = "external";

  @SerializedName("lang")
  private final String code;

  private final String name;

  private final String status;

  @SerializedName("usable_as_source")
  private final boolean usableAsSource;

  @SerializedName("usable_as_target")
  private final boolean usableAsTarget;

  @Nullable private final Map<String, LanguageFeature> features;

  /**
   * Initializes a new LanguageSupport object.
   *
   * @param code BCP-47 language code, for example "de", "en-US" or "zh-Hans".
   * @param name Name of the language in English.
   * @param status Support status of the language, for example "stable" or "beta".
   * @param usableAsSource Whether the language can be used as a source language.
   * @param usableAsTarget Whether the language can be used as a target language.
   * @param features Map of feature name to {@link LanguageFeature}.
   */
  public LanguageSupport(
      String code,
      String name,
      String status,
      boolean usableAsSource,
      boolean usableAsTarget,
      @Nullable Map<String, LanguageFeature> features) {
    this.code = code;
    this.name = name;
    this.status = status;
    this.usableAsSource = usableAsSource;
    this.usableAsTarget = usableAsTarget;
    this.features = features;
  }

  /** @return BCP-47 language code, for example "de", "en-US", "pt-BR" or "zh-Hans". */
  public String getCode() {
    return code;
  }

  /** @return Name of the language in English. */
  public String getName() {
    return name;
  }

  /** @return Support status of the language, for example "stable" or "beta". */
  public String getStatus() {
    return status;
  }

  /** @return <code>true</code> if the language can be used as a source language. */
  public boolean isUsableAsSource() {
    return usableAsSource;
  }

  /** @return <code>true</code> if the language can be used as a target language. */
  public boolean isUsableAsTarget() {
    return usableAsTarget;
  }

  /**
   * @return Map of feature name to {@link LanguageFeature} for the features this language supports
   *     with the requested resource; empty if it supports no optional features.
   */
  public Map<String, LanguageFeature> getFeatures() {
    return features == null ? Collections.emptyMap() : Collections.unmodifiableMap(features);
  }

  /**
   * Checks whether this language supports the given feature for the requested resource.
   *
   * @param feature Feature name, for example {@link LanguageFeature#FEATURE_FORMALITY}.
   * @return <code>true</code> if the feature is supported.
   */
  public boolean supports(String feature) {
    return features != null && features.containsKey(feature);
  }

  @Override
  public String toString() {
    return name + " (" + code + ")";
  }
}
