// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 * Expected form-encoded request bodies. The encoder is Apache's, not the one the SDK uses, so a bug
 * in the SDK's encoding cannot hide behind itself. Key order matters.
 */
final class FormBody {
  private FormBody() {}

  /** The exact body for the given keys and values, as {@code key1, value1, key2, value2, ...}. */
  static StringValuePattern of(String... keysAndValues) {
    List<NameValuePair> pairs = new ArrayList<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      pairs.add(new BasicNameValuePair(keysAndValues[i], keysAndValues[i + 1]));
    }
    return WireMock.equalTo(URLEncodedUtils.format(pairs, StandardCharsets.UTF_8));
  }
}
