// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.StyleProfile;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Class representing the v3 style profile list response by the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class StyleProfileListResponse {
  @SerializedName(value = "style_profiles")
  private List<StyleProfile> styleProfiles;

  public List<StyleProfile> getStyleProfiles() {
    return styleProfiles;
  }
}
