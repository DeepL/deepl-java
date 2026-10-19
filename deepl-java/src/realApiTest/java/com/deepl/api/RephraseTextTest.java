// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class RephraseTextTest {
  @Test
  void sendsPostV2WriteRephraseAndParsesTheResult() throws Exception {
    WriteResult result =
        RealApiClient.createClient().rephraseText(Fixtures.EXAMPLE_TEXT, "EN-GB", null);

    assertFalse(result.getText().isEmpty());
    assertFalse(result.getDetectedSourceLanguage().isEmpty());
    assertFalse(result.getTargetLanguage().isEmpty());
  }
}
