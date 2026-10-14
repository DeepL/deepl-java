// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CorrectTextTest extends TestBase {

  @Test
  void testSingleText() throws DeepLException, InterruptedException {
    String inputText = exampleText.get("en");
    DeepLClient client = createDeepLClient();
    WriteResult result = client.correctText(inputText, "EN-GB");
    this.checkSanityOfImprovements(inputText, result, "EN", "EN-GB", 0.2f);
  }

  @Test
  void testTextArray() throws DeepLException, InterruptedException {
    DeepLClient client = createDeepLClient();
    List<String> texts = new ArrayList<>();
    texts.add(exampleText.get("en"));
    texts.add(exampleText.get("en"));
    List<WriteResult> results = client.correctText(texts, "EN-GB");
    for (int i = 0; i < texts.size(); i++) {
      this.checkSanityOfImprovements(texts.get(i), results.get(i), "EN", "EN-GB", 0.2f);
    }
  }

  @Test
  void testEmptyText() {
    DeepLClient client = createDeepLClient();
    Assertions.assertThrows(IllegalArgumentException.class, () -> client.correctText("", null));
  }

  protected void checkSanityOfImprovements(
      String inputText,
      WriteResult result,
      String expectedSourceLanguageUppercase,
      String expectedTargetLanguageUppercase,
      float epsilon) {
    Assertions.assertEquals(
        expectedSourceLanguageUppercase, result.getDetectedSourceLanguage().toUpperCase());
    Assertions.assertEquals(
        expectedTargetLanguageUppercase, result.getTargetLanguage().toUpperCase());
    int nImproved = result.getText().length();
    int nOriginal = inputText.length();
    Assertions.assertTrue(
        1 / (1 + epsilon) <= ((float) nImproved) / nOriginal,
        "Improved text is too short compared to original, improved:\n"
            + result.getText()
            + "\n, original:\n"
            + inputText);
    Assertions.assertTrue(
        ((float) nImproved) / nOriginal <= (1 + epsilon),
        "Improved text is too long compared to original");
  }
}
