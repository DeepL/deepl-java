// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class RephraseTextTest extends TestBase {

  @Test
  void testSingleText() throws DeepLException, InterruptedException {
    String inputText = exampleText.get("en");
    DeepLClient client = createDeepLClient();
    WriteResult result = client.rephraseText(inputText, "EN-GB", null);
    this.checkSanityOfImprovements(inputText, result, "EN", "EN-GB", 0.2f);
  }

  @Test
  void testBareEnglishAndPortugueseTargetLanguages() {
    DeepLClient client = createDeepLClient();
    Assertions.assertDoesNotThrow(
        () -> client.rephraseText(exampleText.get("en"), LanguageCode.English, null));
    Assertions.assertDoesNotThrow(
        () -> client.rephraseText(exampleText.get("pt"), LanguageCode.Portuguese, null));
  }

  @Test
  void testNullTargetLang() throws DeepLException, InterruptedException {
    // deepl-mock requires target_lang for rephrase although the API does not.
    Assumptions.assumeFalse(isMockServer);
    DeepLClient client = createDeepLClient();
    WriteResult result = client.rephraseText(exampleText.get("en"), null, null);
    Assertions.assertNotNull(result.getText());
  }

  @Test
  void testTextArray() throws DeepLException, InterruptedException {
    DeepLClient client = createDeepLClient();
    List<String> texts = new ArrayList<>();
    texts.add(exampleText.get("en"));
    texts.add(exampleText.get("en"));
    List<WriteResult> results = client.rephraseText(texts, "EN-GB", null);
    for (int i = 0; i < texts.size(); i++) {
      this.checkSanityOfImprovements(texts.get(i), results.get(i), "EN", "EN-GB", 0.2f);
    }
  }

  @Test
  void testBusinessStyle() throws DeepLException, InterruptedException {
    String inputText =
        "As Gregor Samsa awoke one morning from uneasy dreams he found himself transformed in his bed into a gigantic insect.";
    DeepLClient client = createDeepLClient();
    TextRephraseOptions options =
        (new TextRephraseOptions()).setWritingStyle(WritingStyle.Business.getValue());
    WriteResult result = client.rephraseText(inputText, "EN-GB", options);
    if (!isMockServer) {
      this.checkSanityOfImprovements(inputText, result, "EN", "EN-GB", 0.2f);
    }
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
        nImproved / nOriginal <= (1 + epsilon), "Improved text is too long compared to original");
  }
}
