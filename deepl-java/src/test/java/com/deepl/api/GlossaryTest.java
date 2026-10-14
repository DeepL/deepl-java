// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.utils.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.*;

public class GlossaryTest extends TestBase {
  private final String invalidGlossaryId = "invalid_glossary_id";
  private final String nonexistentGlossaryId = "96ab91fd-e715-41a1-adeb-5d701f84a483";
  private final String sourceLang = "en";
  private final String targetLang = "de";

  private final GlossaryEntries testEntries = GlossaryEntries.fromTsv("Hello\tHallo");

  @Test
  void TestGlossaryEntries() {
    GlossaryEntries testEntries = new GlossaryEntries();
    testEntries.put("apple", "Apfel");
    testEntries.put("crab apple", "Holzapfel");
    Assertions.assertEquals(
        testEntries, GlossaryEntries.fromTsv("apple\tApfel\n crab apple \t Holzapfel "));
    Assertions.assertThrows(Exception.class, () -> GlossaryEntries.fromTsv(""));
    Assertions.assertThrows(
        Exception.class, () -> GlossaryEntries.fromTsv("Küche\tKitchen\nKüche\tCuisine"));
    Assertions.assertThrows(Exception.class, () -> GlossaryEntries.fromTsv("A\tB\tC"));
    Assertions.assertThrows(Exception.class, () -> GlossaryEntries.fromTsv("A\t "));

    Assertions.assertThrows(
        Exception.class, () -> new GlossaryEntries(Collections.singletonMap("A", "B\tC")));
  }

  @Test
  void testGlossaryCreate() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("Hello", "Hallo"));
      System.out.println(entries);
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        System.out.println(entry.getKey() + ":" + entry.getValue());
      }
      String glossaryName = cleanup.getGlossaryName();
      GlossaryInfo glossary =
          translator.createGlossary(glossaryName, sourceLang, targetLang, entries);

      Assertions.assertEquals(glossaryName, glossary.getName());
      Assertions.assertEquals(sourceLang, glossary.getSourceLang());
      Assertions.assertEquals(targetLang, glossary.getTargetLang());
      Assertions.assertEquals(1, glossary.getEntryCount());

      GlossaryInfo getResult = translator.getGlossary(glossary.getGlossaryId());
      Assertions.assertEquals(getResult.getName(), glossary.getName());
      Assertions.assertEquals(getResult.getSourceLang(), glossary.getSourceLang());
      Assertions.assertEquals(getResult.getTargetLang(), glossary.getTargetLang());
      Assertions.assertEquals(getResult.getCreationTime(), glossary.getCreationTime());
      Assertions.assertEquals(getResult.getEntryCount(), glossary.getEntryCount());
    }
  }

  @Test
  void testGlossaryCreateLarge() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();

      Map<String, String> entryPairs = new HashMap<>();
      for (int i = 0; i < 10000; i++) {
        entryPairs.put(String.format("Source-%d", i), String.format("Target-%d", i));
      }
      GlossaryEntries entries = new GlossaryEntries(entryPairs);
      Assertions.assertTrue(entries.toTsv().length() > 100000);
      GlossaryInfo glossary =
          translator.createGlossary(glossaryName, sourceLang, targetLang, entries);

      Assertions.assertEquals(glossaryName, glossary.getName());
      Assertions.assertEquals(sourceLang, glossary.getSourceLang());
      Assertions.assertEquals(targetLang, glossary.getTargetLang());
      Assertions.assertEquals(entryPairs.size(), glossary.getEntryCount());
    }
  }

  @Test
  void testGlossaryCreateCsv() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      Map<String, String> expectedEntries = new HashMap<>();
      expectedEntries.put("sourceEntry1", "targetEntry1");
      expectedEntries.put("source\"Entry", "target,Entry");

      String csvContent =
          "sourceEntry1,targetEntry1,en,de\n\"source\"\"Entry\",\"target,Entry\",en,de";

      GlossaryInfo glossary =
          translator.createGlossaryFromCsv(glossaryName, sourceLang, targetLang, csvContent);

      GlossaryEntries entries = translator.getGlossaryEntries(glossary);
      Assertions.assertEquals(expectedEntries, entries);
    }
  }

  @Test
  void testGlossaryCreateInvalid() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      Assertions.assertThrows(
          Exception.class,
          () -> translator.createGlossary("", sourceLang, targetLang, testEntries));
      Assertions.assertThrows(
          Exception.class, () -> translator.createGlossary(glossaryName, "en", "xx", testEntries));
    }
  }

  @Test
  void testGlossaryGet() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      GlossaryInfo createdGlossary =
          translator.createGlossary(glossaryName, sourceLang, targetLang, testEntries);

      GlossaryInfo glossary = translator.getGlossary(createdGlossary.getGlossaryId());
      Assertions.assertEquals(createdGlossary.getGlossaryId(), glossary.getGlossaryId());
      Assertions.assertEquals(glossaryName, glossary.getName());
      Assertions.assertEquals(sourceLang, glossary.getSourceLang());
      Assertions.assertEquals(targetLang, glossary.getTargetLang());
      Assertions.assertEquals(createdGlossary.getCreationTime(), glossary.getCreationTime());
      Assertions.assertEquals(testEntries.size(), glossary.getEntryCount());
    }
    Assertions.assertThrows(DeepLException.class, () -> translator.getGlossary(invalidGlossaryId));
    Assertions.assertThrows(
        GlossaryNotFoundException.class, () -> translator.getGlossary(nonexistentGlossaryId));
  }

  @Test
  void testGlossaryGetEntries() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      GlossaryEntries entries = new GlossaryEntries();
      entries.put("Apple", "Apfel");
      entries.put("Banana", "Banane");
      entries.put("A%=&", "B&=%");
      entries.put("\u0394\u3041", "\u6DF1");
      entries.put("\uD83E\uDEA8", "\uD83E\uDEB5");

      GlossaryInfo createdGlossary =
          translator.createGlossary(glossaryName, sourceLang, targetLang, entries);
      Assertions.assertEquals(entries, translator.getGlossaryEntries(createdGlossary));
      Assertions.assertEquals(
          entries, translator.getGlossaryEntries(createdGlossary.getGlossaryId()));
    }

    Assertions.assertThrows(
        DeepLException.class, () -> translator.getGlossaryEntries(invalidGlossaryId));
    Assertions.assertThrows(
        GlossaryNotFoundException.class,
        () -> translator.getGlossaryEntries(nonexistentGlossaryId));
  }

  @Test
  void testGlossaryList() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      translator.createGlossary(glossaryName, sourceLang, targetLang, testEntries);

      List<GlossaryInfo> glossaries = translator.listGlossaries();
      Assertions.assertTrue(
          glossaries.stream()
              .anyMatch((glossaryInfo -> Objects.equals(glossaryInfo.getName(), glossaryName))));
    }
  }

  @Test
  void testGlossaryDelete() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      GlossaryInfo glossary =
          translator.createGlossary(glossaryName, sourceLang, targetLang, testEntries);

      translator.deleteGlossary(glossary);
      Assertions.assertThrows(
          GlossaryNotFoundException.class, () -> translator.deleteGlossary(glossary));

      Assertions.assertThrows(
          DeepLException.class, () -> translator.deleteGlossary(invalidGlossaryId));
      Assertions.assertThrows(
          GlossaryNotFoundException.class, () -> translator.deleteGlossary(nonexistentGlossaryId));
    }
  }

  @Test
  void testGlossaryTranslateTextSentence() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      GlossaryEntries entries =
          new GlossaryEntries() {
            {
              put("artist", "Maler");
              put("prize", "Gewinn");
            }
          };
      String inputText = "The artist was awarded a prize.";

      GlossaryInfo glossary =
          translator.createGlossary(glossaryName, sourceLang, targetLang, entries);

      TextResult result =
          translator.translateText(
              inputText,
              sourceLang,
              targetLang,
              new TextTranslationOptions().setGlossary(glossary.getGlossaryId()));
      if (!isMockServer) {
        Assertions.assertTrue(result.getText().contains("Maler"));
        Assertions.assertTrue(result.getText().contains("Gewinn"));
      }

      // It is also possible to specify GlossaryInfo
      result =
          translator.translateText(
              inputText,
              sourceLang,
              targetLang,
              new TextTranslationOptions().setGlossary(glossary));
      if (!isMockServer) {
        Assertions.assertTrue(result.getText().contains("Maler"));
        Assertions.assertTrue(result.getText().contains("Gewinn"));
      }
    }
  }

  @Test
  void testGlossaryTranslateTextBasic() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanupEnDe = new GlossaryCleanupUtility(translator, "EnDe");
        GlossaryCleanupUtility cleanupDeEn = new GlossaryCleanupUtility(translator, "DeEn")) {
      String glossaryNameEnDe = cleanupEnDe.getGlossaryName();
      String glossaryNameDeEn = cleanupDeEn.getGlossaryName();
      List<String> textsEn =
          new ArrayList<String>() {
            {
              add("Apple");
              add("Banana");
            }
          };
      List<String> textsDe =
          new ArrayList<String>() {
            {
              add("Apfel");
              add("Banane");
            }
          };
      GlossaryEntries glossaryEntriesEnDe = new GlossaryEntries();
      GlossaryEntries glossaryEntriesDeEn = new GlossaryEntries();
      for (int i = 0; i < textsEn.size(); i++) {
        glossaryEntriesEnDe.put(textsEn.get(i), textsDe.get(i));
        glossaryEntriesDeEn.put(textsDe.get(i), textsEn.get(i));
      }

      GlossaryInfo glossaryEnDe =
          translator.createGlossary(glossaryNameEnDe, "en", "de", glossaryEntriesEnDe);
      GlossaryInfo glossaryDeEn =
          translator.createGlossary(glossaryNameDeEn, "de", "en", glossaryEntriesDeEn);

      List<TextResult> result =
          translator.translateText(
              textsEn, "en", "de", new TextTranslationOptions().setGlossary(glossaryEnDe));
      Assertions.assertArrayEquals(
          textsDe.toArray(), result.stream().map(TextResult::getText).toArray());

      result =
          translator.translateText(
              textsDe,
              "de",
              "en-US",
              new TextTranslationOptions().setGlossary(glossaryDeEn.getGlossaryId()));
      Assertions.assertArrayEquals(
          textsEn.toArray(), result.stream().map(TextResult::getText).toArray());
    }
  }

  @Test
  void testGlossaryTranslateDocument() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup = new GlossaryCleanupUtility(translator)) {
      String glossaryName = cleanup.getGlossaryName();
      File inputFile = createInputFile("artist\nprize");
      File outputFile = createOutputFile();
      String expectedOutput = "Maler\nGewinn";
      GlossaryEntries entries =
          new GlossaryEntries() {
            {
              put("artist", "Maler");
              put("prize", "Gewinn");
            }
          };

      GlossaryInfo glossary =
          translator.createGlossary(glossaryName, sourceLang, targetLang, entries);

      translator.translateDocument(
          inputFile,
          outputFile,
          sourceLang,
          targetLang,
          new DocumentTranslationOptions().setGlossary(glossary));
      Assertions.assertEquals(expectedOutput, readFromFile(outputFile));
      boolean ignored = outputFile.delete();

      translator.translateDocument(
          inputFile,
          outputFile,
          sourceLang,
          targetLang,
          new DocumentTranslationOptions().setGlossary(glossary.getGlossaryId()));
      Assertions.assertEquals(expectedOutput, readFromFile(outputFile));
    }
  }

  @Test
  void testGlossaryTranslateTextInvalid() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanupEnDe = new GlossaryCleanupUtility(translator, "EnDe")) {
      String glossaryNameEnDe = cleanupEnDe.getGlossaryName();

      GlossaryInfo glossaryEnDe =
          translator.createGlossary(glossaryNameEnDe, "en", "de", testEntries);

      IllegalArgumentException exception =
          Assertions.assertThrows(
              IllegalArgumentException.class,
              () ->
                  translator.translateText(
                      "test", null, "de", new TextTranslationOptions().setGlossary(glossaryEnDe)));
      Assertions.assertTrue(exception.getMessage().contains("sourceLang is required"));
    }
  }

  @Test
  void testGlossaryIdsTranslateTextBasic() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanupEnDe1 = new GlossaryCleanupUtility(translator, "EnDe1");
        GlossaryCleanupUtility cleanupEnDe2 = new GlossaryCleanupUtility(translator, "EnDe2")) {
      String glossaryName1 = cleanupEnDe1.getGlossaryName();
      String glossaryName2 = cleanupEnDe2.getGlossaryName();

      GlossaryEntries entries1 = GlossaryEntries.fromTsv("Apple\tApfel");
      GlossaryEntries entries2 = GlossaryEntries.fromTsv("Banana\tBanane");

      GlossaryInfo glossary1 = translator.createGlossary(glossaryName1, "en", "de", entries1);
      GlossaryInfo glossary2 = translator.createGlossary(glossaryName2, "en", "de", entries2);

      List<String> textsEn =
          new ArrayList<String>() {
            {
              add("Apple");
              add("Banana");
            }
          };
      List<String> textsDe =
          new ArrayList<String>() {
            {
              add("Apfel");
              add("Banane");
            }
          };

      // Using a list of glossary IDs
      List<TextResult> result =
          translator.translateText(
              textsEn,
              "en",
              "de",
              new TextTranslationOptions()
                  .setGlossaryIds(glossary1.getGlossaryId(), glossary2.getGlossaryId()));
      Assertions.assertArrayEquals(
          textsDe.toArray(), result.stream().map(TextResult::getText).toArray());

      // Using GlossaryInfo objects
      result =
          translator.translateText(
              textsEn,
              "en",
              "de",
              new TextTranslationOptions().setGlossaries(glossary1, glossary2));
      Assertions.assertArrayEquals(
          textsDe.toArray(), result.stream().map(TextResult::getText).toArray());
    }
  }

  @Test
  void testGlossaryIdsTranslateDocument() throws Exception {
    Translator translator = createTranslator();
    try (GlossaryCleanupUtility cleanup1 = new GlossaryCleanupUtility(translator, "1");
        GlossaryCleanupUtility cleanup2 = new GlossaryCleanupUtility(translator, "2")) {
      String glossaryName1 = cleanup1.getGlossaryName();
      String glossaryName2 = cleanup2.getGlossaryName();
      File inputFile = createInputFile("artist\nprize");
      File outputFile = createOutputFile();
      String expectedOutput = "Maler\nGewinn";

      GlossaryEntries entries1 = GlossaryEntries.fromTsv("artist\tMaler");
      GlossaryEntries entries2 = GlossaryEntries.fromTsv("prize\tGewinn");

      GlossaryInfo glossary1 =
          translator.createGlossary(glossaryName1, sourceLang, targetLang, entries1);
      GlossaryInfo glossary2 =
          translator.createGlossary(glossaryName2, sourceLang, targetLang, entries2);

      translator.translateDocument(
          inputFile,
          outputFile,
          sourceLang,
          targetLang,
          new DocumentTranslationOptions()
              .setGlossaryIds(glossary1.getGlossaryId(), glossary2.getGlossaryId()));
      Assertions.assertEquals(expectedOutput, readFromFile(outputFile));
    }
  }

  @Test
  void testGlossaryIdsAndGlossaryIdMutuallyExclusive() throws Exception {
    Translator translator = createTranslator();
    IllegalArgumentException exception =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                translator.translateText(
                    "test",
                    "en",
                    "de",
                    new TextTranslationOptions()
                        .setGlossaryId(nonexistentGlossaryId)
                        .setGlossaryIds(nonexistentGlossaryId)));
    Assertions.assertTrue(
        exception.getMessage().contains("cannot be used together with glossaryId"));
  }

  @Test
  void testGlossaryIdsTooMany() throws Exception {
    Translator translator = createTranslator();
    IllegalArgumentException exception =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                translator.translateText(
                    "test",
                    "en",
                    "de",
                    new TextTranslationOptions()
                        .setGlossaryIds("id1", "id2", "id3", "id4", "id5", "id6")));
    Assertions.assertTrue(exception.getMessage().contains("more than 5"));
  }

  @Test
  void testGlossaryIdsMissingSourceLang() throws Exception {
    Translator translator = createTranslator();
    IllegalArgumentException exception =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                translator.translateText(
                    "test",
                    null,
                    "de",
                    new TextTranslationOptions().setGlossaryIds(nonexistentGlossaryId)));
    Assertions.assertTrue(exception.getMessage().contains("sourceLang is required"));
  }
}
