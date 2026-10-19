// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TranslateTextTest {
  @Test
  void sendsPostV2TranslateAndParsesTheResult() throws Exception {
    TextResult result =
        RealApiClient.createTranslator().translateText(Fixtures.EXAMPLE_TEXT, null, "de");

    assertFalse(result.getText().isEmpty());
    assertFalse(result.getDetectedSourceLanguage().isEmpty());
    assertTrue(result.getBilledCharacters() > 0);
  }
}
