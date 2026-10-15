// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.ResourceFeature;
import java.util.List;

/**
 * Class representing a single entry in a /v3/languages/resources response.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class V3LanguageResource {
  String name;
  List<ResourceFeature> features;
}
