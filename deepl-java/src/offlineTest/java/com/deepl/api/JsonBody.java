// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expected JSON request bodies. Matches the exact set of fields and their JSON types; the order of
 * object keys does not matter, the order of array elements does.
 */
final class JsonBody {
  private JsonBody() {}

  /** The exact body for the given keys and values, as {@code key1, value1, key2, value2, ...}. */
  static StringValuePattern of(Object... keysAndValues) {
    return WireMock.equalToJson(Json.write(object(keysAndValues)));
  }

  /** A JSON object for the given keys and values, for nesting into {@link #of(Object...)}. */
  static Map<String, Object> object(Object... keysAndValues) {
    Map<String, Object> fields = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      fields.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return fields;
  }

  /** A JSON array of the given values. */
  static List<Object> list(Object... values) {
    return Arrays.asList(values);
  }

  /** The JSON object of one glossary dictionary. */
  static Map<String, Object> dictionary(
      String sourceLang, String targetLang, String entries, String entriesFormat) {
    return object(
        "source_lang",
        sourceLang,
        "target_lang",
        targetLang,
        "entries",
        entries,
        "entries_format",
        entriesFormat);
  }
}
