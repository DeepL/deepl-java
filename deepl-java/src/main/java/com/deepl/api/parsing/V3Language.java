// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Class representing a single entry in a /v3/languages response.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class V3Language {
  private String lang;
  private String name;

  @SerializedName("usable_as_source")
  private boolean usableAsSource;

  @SerializedName("usable_as_target")
  private boolean usableAsTarget;

  private Map<String, JsonElement> features;

  public String getLang() {
    return lang;
  }

  public String getName() {
    return name;
  }

  public boolean isUsableAsSource() {
    return usableAsSource;
  }

  public boolean isUsableAsTarget() {
    return usableAsTarget;
  }

  public boolean hasFeature(String featureName) {
    return features != null && features.containsKey(featureName);
  }
}
