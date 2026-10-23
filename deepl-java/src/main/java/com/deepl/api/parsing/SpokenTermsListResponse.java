// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.SpokenTerms;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Class representing the v3 spoken-terms list response by the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class SpokenTermsListResponse {
  @SerializedName(value = "spoken_terms")
  private List<SpokenTerms> spokenTerms;

  public List<SpokenTerms> getSpokenTerms() {
    return spokenTerms;
  }
}
