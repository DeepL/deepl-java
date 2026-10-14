// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class TranslateTextTest extends TestBase {

  @Test
  void testSingleText() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    TextResult result = translator.translateText(exampleText.get("en"), null, LanguageCode.German);
    Assertions.assertEquals(exampleText.get("de"), result.getText());
    Assertions.assertEquals("en", result.getDetectedSourceLanguage());
    Assertions.assertEquals(exampleText.get("en").length(), result.getBilledCharacters());
  }

  @Test
  void testTextArray() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    List<String> texts = new ArrayList<>();
    texts.add(exampleText.get("fr"));
    texts.add(exampleText.get("en"));
    List<TextResult> result = translator.translateText(texts, null, LanguageCode.German);
    Assertions.assertEquals(exampleText.get("de"), result.get(0).getText());
    Assertions.assertEquals(exampleText.get("de"), result.get(1).getText());
  }

  @Test
  void testSourceLang() throws DeepLException, InterruptedException {
    Consumer<TextResult> checkResult =
        (result) -> {
          Assertions.assertEquals(exampleText.get("de"), result.getText());
          Assertions.assertEquals("en", result.getDetectedSourceLanguage());
        };

    Translator translator = createTranslator();
    checkResult.accept(translator.translateText(exampleText.get("en"), null, "DE"));
    checkResult.accept(translator.translateText(exampleText.get("en"), "En", "DE"));
    checkResult.accept(translator.translateText(exampleText.get("en"), "en", "DE"));

    List<Language> sourceLanguages = translator.getSourceLanguages();
    Language sourceLanguageEn =
        sourceLanguages.stream()
            .filter((language -> Objects.equals(language.getCode(), "en")))
            .findFirst()
            .orElse(null);
    Language sourceLanguageDe =
        sourceLanguages.stream()
            .filter((language -> Objects.equals(language.getCode(), "de")))
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(sourceLanguageEn);
    Assertions.assertNotNull(sourceLanguageDe);
    checkResult.accept(
        translator.translateText(exampleText.get("en"), sourceLanguageEn, sourceLanguageDe));
  }

  @Test
  void testTargetLang() throws DeepLException, InterruptedException {
    Consumer<TextResult> checkResult =
        (result) -> {
          Assertions.assertEquals(exampleText.get("de"), result.getText());
          Assertions.assertEquals("en", result.getDetectedSourceLanguage());
        };

    Translator translator = createTranslator();
    checkResult.accept(translator.translateText(exampleText.get("en"), null, "De"));
    checkResult.accept(translator.translateText(exampleText.get("en"), null, "de"));
    checkResult.accept(translator.translateText(exampleText.get("en"), null, "DE"));

    List<Language> targetLanguages = translator.getTargetLanguages();
    Language targetLanguageDe =
        targetLanguages.stream()
            .filter((language -> Objects.equals(language.getCode(), "de")))
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(targetLanguageDe);
    checkResult.accept(translator.translateText(exampleText.get("en"), null, targetLanguageDe));
  }

  @Test
  void testBareEnglishAndPortugueseTargetLanguages() {
    Translator translator = createTranslator();
    Assertions.assertDoesNotThrow(
        () -> translator.translateText(exampleText.get("de"), null, LanguageCode.English));
    Assertions.assertDoesNotThrow(
        () -> translator.translateText(exampleText.get("de"), null, LanguageCode.Portuguese));
  }

  @Test
  void testInvalidLanguage() {
    Translator translator = createTranslator();
    DeepLException thrown;
    thrown =
        Assertions.assertThrows(
            DeepLException.class,
            () -> {
              translator.translateText(exampleText.get("en"), null, "XX");
            });
    Assertions.assertTrue(thrown.getMessage().contains("target_lang"));

    thrown =
        Assertions.assertThrows(
            DeepLException.class,
            () -> {
              translator.translateText(exampleText.get("en"), "XX", "de");
            });
    Assertions.assertTrue(thrown.getMessage().contains("source_lang"));
  }

  @Test
  void testTranslateWithRetries() throws DeepLException, InterruptedException {
    Assumptions.assumeTrue(isMockServer);
    Translator translator = createTranslator(new SessionOptions().setRespondWith429(2));

    long timeBefore = new Date().getTime();
    List<String> texts = new ArrayList<>();
    texts.add(exampleText.get("en"));
    texts.add(exampleText.get("ja"));
    List<TextResult> result = translator.translateText(texts, null, "de");
    long timeAfter = new Date().getTime();

    Assertions.assertEquals(2, result.size());
    Assertions.assertEquals(exampleText.get("de"), result.get(0).getText());
    Assertions.assertEquals("en", result.get(0).getDetectedSourceLanguage());
    Assertions.assertEquals(exampleText.get("de"), result.get(1).getText());
    Assertions.assertEquals("ja", result.get(1).getDetectedSourceLanguage());
    Assertions.assertTrue(timeAfter - timeBefore > 1000);
  }

  @Test
  void testFormality() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    TextResult result;

    result =
        translator.translateText(
            "How are you?", null, "de", new TextTranslationOptions().setFormality(Formality.Less));
    if (!isMockServer) {
      Assertions.assertTrue(result.getText().contains("dir"));
    }

    // Default formality is automatic, so the output may be either formal or informal
    result =
        translator.translateText(
            "How are you?",
            null,
            "de",
            new TextTranslationOptions().setFormality(Formality.Default));
    if (!isMockServer) {
      Assertions.assertTrue(result.getText().contains("Ihnen") || result.getText().contains("dir"));
    }

    result =
        translator.translateText(
            "How are you?", null, "de", new TextTranslationOptions().setFormality(Formality.More));
    if (!isMockServer) {
      Assertions.assertTrue(result.getText().contains("Ihnen"));
    }

    result =
        translator.translateText(
            "How are you?",
            null,
            "de",
            new TextTranslationOptions().setFormality(Formality.PreferLess));
    if (!isMockServer) {
      Assertions.assertTrue(result.getText().contains("dir"));
    }

    result =
        translator.translateText(
            "How are you?",
            null,
            "de",
            new TextTranslationOptions().setFormality(Formality.PreferMore));
    if (!isMockServer) {
      Assertions.assertTrue(result.getText().contains("Ihnen"));
    }
  }

  @Test
  void testContext() throws DeepLException, InterruptedException {
    // In German, "scharf" can mean:
    //  - spicy/hot when referring to food, or
    //  - sharp when referring to other objects such as a knife (Messer).
    Translator translator = createTranslator();
    String text = "Das ist scharf!";

    translator.translateText(text, null, "de");
    // Result: "That is hot!"

    translator.translateText(
        text, null, "de", new TextTranslationOptions().setContext("Das ist ein Messer."));
    // Result: "That is sharp!"
  }

  @Test
  void testSplitSentences() throws DeepLException, InterruptedException {
    Assumptions.assumeTrue(isMockServer);

    Translator translator = createTranslator();
    String text =
        "If the implementation is hard to explain, it's a bad idea.\nIf the implementation is easy to explain, it may be a good idea.";

    translator.translateText(
        text,
        null,
        "de",
        new TextTranslationOptions().setSentenceSplittingMode(SentenceSplittingMode.Off));
    translator.translateText(
        text,
        null,
        "de",
        new TextTranslationOptions().setSentenceSplittingMode(SentenceSplittingMode.All));
    translator.translateText(
        text,
        null,
        "de",
        new TextTranslationOptions().setSentenceSplittingMode(SentenceSplittingMode.NoNewlines));
  }

  @Test
  void testPreserveFormatting() throws DeepLException, InterruptedException {
    Assumptions.assumeTrue(isMockServer);

    Translator translator = createTranslator();
    translator.translateText(
        exampleText.get("en"),
        null,
        "de",
        new TextTranslationOptions().setPreserveFormatting(true));
    translator.translateText(
        exampleText.get("en"),
        null,
        "de",
        new TextTranslationOptions().setPreserveFormatting(false));
  }

  @Test
  void testTagHandlingXML() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    String text =
        "<document><meta><title>A document's title</title></meta>"
            + "<content><par>"
            + "<span>This is a sentence split</span>"
            + "<span>across two &lt;span&gt; tags that should be treated as one."
            + "</span>"
            + "</par>"
            + "<par>Here is a sentence. Followed by a second one.</par>"
            + "<raw>This sentence will not be translated.</raw>"
            + "</content>"
            + "</document>";
    TextResult result =
        translator.translateText(
            text,
            null,
            "de",
            new TextTranslationOptions()
                .setTagHandling("xml")
                .setOutlineDetection(false)
                .setNonSplittingTags(Arrays.asList("span"))
                .setSplittingTags(Arrays.asList("title", "par"))
                .setIgnoreTags(Arrays.asList("raw")));
    if (!isMockServer) {
      Assertions.assertTrue(
          result.getText().contains("<raw>This sentence will not be translated.</raw>"));
      Assertions.assertTrue(result.getText().matches(".*<title>.*Der Titel.*</title>.*"));
    }
  }

  @Test
  void testTagHandlingHTML() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    String text =
        "<!DOCTYPE html>"
            + "<html>"
            + "<body>"
            + "<h1>My First Heading</h1>"
            + "<p translate=\"no\">My first paragraph.</p>"
            + "</body>"
            + "</html>";

    TextResult result =
        translator.translateText(
            text, null, "de", new TextTranslationOptions().setTagHandling("html"));
    if (!isMockServer) {
      Assertions.assertTrue(result.getText().contains("<h1>Meine erste Überschrift</h1>"));
      Assertions.assertTrue(
          result.getText().contains("<p translate=\"no\">My first paragraph.</p>"));
    }
  }

  @Test
  void testTagHandlingVersionV1() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    String text = "<p>Hello world</p>";

    TextResult result =
        translator.translateText(
            text,
            null,
            "de",
            new TextTranslationOptions().setTagHandling("html").setTagHandlingVersion("v1"));
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getText());
    Assertions.assertFalse(result.getText().isEmpty());
  }

  @Test
  void testTagHandlingVersionV2() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    String text = "<p>Hello world</p>";

    TextResult result =
        translator.translateText(
            text,
            null,
            "de",
            new TextTranslationOptions().setTagHandling("html").setTagHandlingVersion("v2"));
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getText());
    Assertions.assertFalse(result.getText().isEmpty());
  }

  @Test
  void testEmptyText() {
    Translator translator = createTranslator();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          translator.translateText("", null, "de");
        });
  }

  @Test
  void testMixedCaseLanguages() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    TextResult result;

    result = translator.translateText(exampleText.get("de"), null, "en-us");
    Assertions.assertEquals(exampleText.get("en-US"), result.getText().toLowerCase(Locale.ENGLISH));
    Assertions.assertEquals("de", result.getDetectedSourceLanguage());

    result = translator.translateText(exampleText.get("de"), null, "EN-us");
    Assertions.assertEquals(exampleText.get("en-US"), result.getText().toLowerCase(Locale.ENGLISH));
    Assertions.assertEquals("de", result.getDetectedSourceLanguage());

    result = translator.translateText(exampleText.get("de"), "de", "EN-US");
    Assertions.assertEquals(exampleText.get("en-US"), result.getText().toLowerCase(Locale.ENGLISH));
    Assertions.assertEquals("de", result.getDetectedSourceLanguage());

    result = translator.translateText(exampleText.get("de"), "dE", "EN-US");
    Assertions.assertEquals(exampleText.get("en-US"), result.getText().toLowerCase(Locale.ENGLISH));
    Assertions.assertEquals("de", result.getDetectedSourceLanguage());
  }

  @Test
  void testExtraBodyParams() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();

    // Verifies that extra_body_parameters can override standard parameters like target_lang
    Map<String, String> extraParams = new HashMap<>();
    extraParams.put("target_lang", "FR");
    extraParams.put("debug", "1");

    TextTranslationOptions options = new TextTranslationOptions();
    options.setExtraBodyParameters(extraParams);

    TextResult result = translator.translateText(exampleText.get("en"), null, "DE", options);

    Assertions.assertEquals(exampleText.get("fr"), result.getText());
    Assertions.assertEquals("en", result.getDetectedSourceLanguage());
    Assertions.assertEquals(exampleText.get("en").length(), result.getBilledCharacters());
  }

  @Test
  void testCustomInstructions() throws DeepLException, InterruptedException {
    Translator translator = createTranslator();
    String text = "Hello world. I am testing if custom instructions are working correctly.";

    TextResult resultWithCustomInstructions =
        translator.translateText(
            text,
            null,
            "de",
            new TextTranslationOptions()
                .setCustomInstructions(
                    Collections.singletonList("Render the whole text in ALL CAPS")));

    Assertions.assertNotNull(resultWithCustomInstructions.getText());
    Assertions.assertEquals("en", resultWithCustomInstructions.getDetectedSourceLanguage());
    if (!isMockServer) {
      // Assert the instruction was actually applied, rather than that the output merely differs
      // from an unconstrained translation. The previous version compared against a translation
      // made without instructions and used "Use informal language" / "Be concise", but the
      // default translation of this sentence is already informal and concise, so the API
      // legitimately returned identical text and the comparison drifted with the model.
      Assertions.assertEquals(
          resultWithCustomInstructions.getText().toUpperCase(Locale.ROOT),
          resultWithCustomInstructions.getText());
    }
  }
}
