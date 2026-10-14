// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.io.File;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultilingualGlossaryTest extends TestBase {
  private final String invalidGlossaryId = "invalid_glossary_id";
  private final String nonexistentGlossaryId = "96ab91fd-e715-41a1-adeb-5d701f84a483";
  private final String sourceLang = "en";
  private final String targetLang = "de";

  private final GlossaryEntries testEntries = GlossaryEntries.fromTsv("Hello\tHallo");
  private final MultilingualGlossaryDictionaryEntries testGlossaryDict =
      new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, testEntries);

  @Test
  void testGlossaryCreate() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("Hello", "Hallo"));
      System.out.println(entries);
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        System.out.println(entry.getKey() + ":" + entry.getValue());
      }
      String glossaryName = cleanup.getGlossaryName();
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts =
          Arrays.asList(
              testGlossaryDict,
              new MultilingualGlossaryDictionaryEntries(targetLang, sourceLang, entries));
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      Assertions.assertEquals(glossaryName, glossary.getName());
      AssertGlossaryDictionariesEquivalent(glossaryDicts, glossary.getDictionaries());

      MultilingualGlossaryInfo getResult =
          deepLClient.getMultilingualGlossary(glossary.getGlossaryId());
      AssertGlossaryDictionariesEquivalent(glossaryDicts, getResult.getDictionaries());
    }
  }

  @Test
  void testGlossaryCreateLarge() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();

      Map<String, String> entryPairs = new HashMap<>();
      for (int i = 0; i < 10000; i++) {
        entryPairs.put(String.format("Source-%d", i), String.format("Target-%d", i));
      }
      GlossaryEntries entries = new GlossaryEntries(entryPairs);
      Assertions.assertTrue(entries.toTsv().length() > 100000);
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts =
          Arrays.asList(new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries));
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      Assertions.assertEquals(glossaryName, glossary.getName());
      AssertGlossaryDictionariesEquivalent(glossaryDicts, glossary.getDictionaries());
    }
  }

  @Test
  void testGlossaryCreateCsv() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      Map<String, String> expectedEntries = new HashMap<>();
      expectedEntries.put("sourceEntry1", "targetEntry1");
      expectedEntries.put("source\"Entry", "target,Entry");

      String csvContent =
          "sourceEntry1,targetEntry1,en,de\n\"source\"\"Entry\",\"target,Entry\",en,de";

      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossaryFromCsv(
              glossaryName, sourceLang, targetLang, csvContent);

      MultilingualGlossaryDictionaryEntries createdGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(glossary, sourceLang, targetLang);
      Assertions.assertEquals(expectedEntries, createdGlossaryDict.getEntries());
    }
  }

  @Test
  void testGlossaryCreateInvalid() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> deepLClient.createMultilingualGlossary("", Arrays.asList(testGlossaryDict)));
      Assertions.assertThrows(
          Exception.class,
          () ->
              deepLClient.createMultilingualGlossary(
                  glossaryName,
                  Arrays.asList(
                      new MultilingualGlossaryDictionaryEntries("en", "xx", testEntries))));
    }
  }

  @Test
  void testGlossaryGet() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts = Arrays.asList(testGlossaryDict);
      MultilingualGlossaryInfo createdGlossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      MultilingualGlossaryInfo glossary =
          deepLClient.getMultilingualGlossary(createdGlossary.getGlossaryId());
      Assertions.assertEquals(createdGlossary.getGlossaryId(), glossary.getGlossaryId());
      Assertions.assertEquals(glossaryName, glossary.getName());
      AssertGlossaryDictionariesEquivalent(glossaryDicts, glossary.getDictionaries());
    }
    Assertions.assertThrows(
        DeepLException.class, () -> deepLClient.getMultilingualGlossary(invalidGlossaryId));
    Assertions.assertThrows(
        GlossaryNotFoundException.class,
        () -> deepLClient.getMultilingualGlossary(nonexistentGlossaryId));
  }

  @Test
  void testGlossaryGetEntries() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      GlossaryEntries entries = new GlossaryEntries();
      entries.put("Apple", "Apfel");
      entries.put("Banana", "Banane");
      entries.put("A%=&", "B&=%");
      entries.put("\u0394\u3041", "\u6DF1");
      entries.put("\uD83E\uDEA8", "\uD83E\uDEB5");

      MultilingualGlossaryDictionaryEntries glossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries);
      MultilingualGlossaryInfo createdGlossary =
          deepLClient.createMultilingualGlossary(glossaryName, Arrays.asList(glossaryDict));
      Assertions.assertEquals(1, createdGlossary.getDictionaries().size());
      MultilingualGlossaryDictionaryInfo createdGlossaryDict =
          createdGlossary.getDictionaries().get(0);

      MultilingualGlossaryDictionaryEntries updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(
              createdGlossary, sourceLang, targetLang);
      Assertions.assertEquals(entries, updatedGlossaryDict.getEntries());
      updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(
              createdGlossary, sourceLang, targetLang);
      Assertions.assertEquals(entries, updatedGlossaryDict.getEntries());
      updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(
              createdGlossary, createdGlossaryDict);
      Assertions.assertEquals(entries, updatedGlossaryDict.getEntries());
      updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(
              createdGlossary.getGlossaryId(), createdGlossaryDict);
      Assertions.assertEquals(entries, updatedGlossaryDict.getEntries());

      Assertions.assertThrows(
          DeepLException.class,
          () ->
              deepLClient.getMultilingualGlossaryDictionaryEntries(
                  invalidGlossaryId, sourceLang, targetLang));
      Assertions.assertThrows(
          GlossaryNotFoundException.class,
          () ->
              deepLClient.getMultilingualGlossaryDictionaryEntries(
                  nonexistentGlossaryId, sourceLang, targetLang));
      Assertions.assertThrows(
          Exception.class,
          () -> deepLClient.getMultilingualGlossaryDictionaryEntries(createdGlossary, "en", "xx"));
      Assertions.assertThrows(
          IllegalArgumentException.class,
          () ->
              deepLClient.getMultilingualGlossaryDictionaryEntries(
                  createdGlossary, "", targetLang));
    }
  }

  @Test
  void testGlossaryList() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      deepLClient.createMultilingualGlossary(glossaryName, Arrays.asList(testGlossaryDict));

      List<MultilingualGlossaryInfo> glossaries = deepLClient.listMultilingualGlossaries();
      Assertions.assertTrue(
          glossaries.stream()
              .anyMatch((glossaryInfo -> Objects.equals(glossaryInfo.getName(), glossaryName))));
    }
  }

  @Test
  void testGlossaryDelete() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, Arrays.asList(testGlossaryDict));

      deepLClient.deleteMultilingualGlossary(glossary);
      Assertions.assertThrows(
          GlossaryNotFoundException.class, () -> deepLClient.deleteMultilingualGlossary(glossary));

      Assertions.assertThrows(
          DeepLException.class, () -> deepLClient.deleteMultilingualGlossary(invalidGlossaryId));
      Assertions.assertThrows(
          GlossaryNotFoundException.class,
          () -> deepLClient.deleteMultilingualGlossary(nonexistentGlossaryId));
    }
  }

  @Test
  void testGlossaryDictionaryDelete() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, Arrays.asList(testGlossaryDict));

      deepLClient.deleteMultilingualGlossaryDictionary(glossary, sourceLang, targetLang);
      Assertions.assertThrows(
          GlossaryNotFoundException.class,
          () -> deepLClient.deleteMultilingualGlossaryDictionary(glossary, sourceLang, targetLang));

      Assertions.assertThrows(
          DeepLException.class,
          () ->
              deepLClient.deleteMultilingualGlossaryDictionary(
                  invalidGlossaryId, sourceLang, targetLang));
      Assertions.assertThrows(
          GlossaryNotFoundException.class,
          () ->
              deepLClient.deleteMultilingualGlossaryDictionary(
                  nonexistentGlossaryId, sourceLang, targetLang));
    }
  }

  @Test
  void testGlossaryReplaceDictionary() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts = Arrays.asList(testGlossaryDict);
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      GlossaryEntries newEntries = new GlossaryEntries();

      newEntries.put("key1", "value1");
      MultilingualGlossaryDictionaryEntries newGlossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, newEntries);
      MultilingualGlossaryDictionaryInfo updatedGlossary =
          deepLClient.replaceMultilingualGlossaryDictionary(glossary, newGlossaryDict);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), Arrays.asList(updatedGlossary));

      newEntries.put("key2", "value2");
      newGlossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, newEntries);
      updatedGlossary =
          deepLClient.replaceMultilingualGlossaryDictionary(
              glossary.getGlossaryId(), newGlossaryDict);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), Arrays.asList(updatedGlossary));

      newEntries.put("key3", "value3");
      newGlossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, newEntries);
      updatedGlossary =
          deepLClient.replaceMultilingualGlossaryDictionary(
              glossary.getGlossaryId(), sourceLang, targetLang, newEntries);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), Arrays.asList(updatedGlossary));

      newEntries.put("key4", "value4");
      newGlossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, newEntries);
      updatedGlossary =
          deepLClient.replaceMultilingualGlossaryDictionary(
              glossary, sourceLang, targetLang, newEntries);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), Arrays.asList(updatedGlossary));
    }
  }

  @Test
  void testGlossaryReplaceDictionaryReplacesExistingEntries() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts = Arrays.asList(testGlossaryDict);
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      GlossaryEntries newEntries = new GlossaryEntries();
      newEntries.put("key1", "value1");
      newEntries.put("key2", "value2");
      MultilingualGlossaryDictionaryEntries newGlossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, newEntries);
      deepLClient.replaceMultilingualGlossaryDictionary(glossary, newGlossaryDict);

      MultilingualGlossaryDictionaryEntries updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(glossary, sourceLang, targetLang);
      Assertions.assertEquals(newEntries, updatedGlossaryDict.getEntries());
    }
  }

  @Test
  void testGlossaryReplaceDictionaryFromCsvReplacesExistingEntries() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts = Arrays.asList(testGlossaryDict);
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      GlossaryEntries newEntries = new GlossaryEntries();
      newEntries.put("key1", "value1");
      newEntries.put("key2", "value2");
      String csvContent = "key1,value1\nkey2,value2";
      deepLClient.replaceMultilingualGlossaryDictionaryFromCsv(
          glossary.getGlossaryId(), sourceLang, targetLang, csvContent);

      MultilingualGlossaryDictionaryEntries updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(glossary, sourceLang, targetLang);
      Assertions.assertEquals(newEntries, updatedGlossaryDict.getEntries());
    }
  }

  @Test
  void testGlossaryUpdateDictionary() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("key1", "value1"));
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts =
          Arrays.asList(new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries));
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      entries = new GlossaryEntries(Collections.singletonMap("key1", "value2"));
      MultilingualGlossaryDictionaryEntries newGlossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries);
      MultilingualGlossaryInfo updatedGlossary =
          deepLClient.updateMultilingualGlossaryDictionary(glossary, newGlossaryDict);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), updatedGlossary.getDictionaries());

      entries = new GlossaryEntries(Collections.singletonMap("key1", "value3"));
      newGlossaryDict = new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries);
      updatedGlossary =
          deepLClient.updateMultilingualGlossaryDictionary(
              glossary.getGlossaryId(), newGlossaryDict);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), updatedGlossary.getDictionaries());

      entries = new GlossaryEntries(Collections.singletonMap("key1", "value4"));
      newGlossaryDict = new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries);
      updatedGlossary =
          deepLClient.updateMultilingualGlossaryDictionary(
              glossary.getGlossaryId(), sourceLang, targetLang, entries);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), updatedGlossary.getDictionaries());

      entries.put("key1", "value5");
      newGlossaryDict = new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries);
      updatedGlossary =
          deepLClient.updateMultilingualGlossaryDictionary(
              glossary, sourceLang, targetLang, entries);
      AssertGlossaryDictionariesEquivalent(
          Arrays.asList(newGlossaryDict), updatedGlossary.getDictionaries());
    }
  }

  @Test
  void testGlossaryUpdateDictionaryUpdatesExistingEntries() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();

      GlossaryEntries entries = new GlossaryEntries();
      entries.put("key1", "value1");
      entries.put("key2", "value2");
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts =
          Arrays.asList(new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries));
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      GlossaryEntries newEntries = new GlossaryEntries();
      newEntries.put("key1", "updatedValue1");
      newEntries.put("newKey", "newValue");
      MultilingualGlossaryDictionaryEntries newGlossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, newEntries);
      deepLClient.updateMultilingualGlossaryDictionary(glossary, newGlossaryDict);

      /* We expect the entries to be the newly updated entries plus the old key2/value2 entry that was unchanged */
      GlossaryEntries expectedEntries = newEntries;
      expectedEntries.put("key2", "value2");

      MultilingualGlossaryDictionaryEntries updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(glossary, sourceLang, targetLang);
      Assertions.assertEquals(expectedEntries, updatedGlossaryDict.getEntries());
    }
  }

  @Test
  void testGlossaryUpdateDictionaryFromCsvUpdatesExistingEntries() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();

      GlossaryEntries entries = new GlossaryEntries();
      entries.put("key1", "value1");
      entries.put("key2", "value2");
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts =
          Arrays.asList(new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries));
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, glossaryDicts);

      GlossaryEntries csvEntries = new GlossaryEntries();
      csvEntries.put("key1", "updatedValue1");
      csvEntries.put("newKey", "newValue");
      String csvContent = "key1,updatedValue1\nnewKey,newValue";
      deepLClient.updateMultilingualGlossaryDictionaryFromCsv(
          glossary.getGlossaryId(), sourceLang, targetLang, csvContent);

      /* We expect the entries to be the newly updated entries plus the old key2/value2 entry that was unchanged */
      GlossaryEntries expectedEntries = csvEntries;
      expectedEntries.put("key2", "value2");

      MultilingualGlossaryDictionaryEntries updatedGlossaryDict =
          deepLClient.getMultilingualGlossaryDictionaryEntries(glossary, sourceLang, targetLang);
      Assertions.assertEquals(expectedEntries, updatedGlossaryDict.getEntries());
    }
  }

  @Test
  void testGlossaryUpdateName() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String originalGlossaryName = "original glossary name";

      GlossaryEntries entries = new GlossaryEntries();
      entries.put("key1", "value1");
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts =
          Arrays.asList(new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries));
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(originalGlossaryName, glossaryDicts);

      String glossaryName = cleanup.getGlossaryName();
      MultilingualGlossaryInfo updatedGlossary =
          deepLClient.updateMultilingualGlossaryName(glossary.getGlossaryId(), glossaryName);

      Assertions.assertEquals(glossaryName, updatedGlossary.getName());
    }
  }

  @Test
  void testGlossaryUpdateNameWithSpecialCharsIsProperlyEncoded() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      GlossaryEntries entries = new GlossaryEntries();
      entries.put("key1", "value1");
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts =
          Arrays.asList(new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries));
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(cleanup.getGlossaryName(), glossaryDicts);

      String nameWithSpecialChars = "Name with special chars: é &foo=bar \"quoted\"";
      MultilingualGlossaryInfo updatedGlossary =
          deepLClient.updateMultilingualGlossaryName(
              glossary.getGlossaryId(), nameWithSpecialChars);

      Assertions.assertEquals(nameWithSpecialChars, updatedGlossary.getName());
    }
  }

  @Test
  void testGlossaryTranslateTextSentence() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
      GlossaryEntries entries =
          new GlossaryEntries() {
            {
              put("artist", "Maler");
              put("prize", "Gewinn");
            }
          };
      String inputText = "The artist was awarded a prize.";
      MultilingualGlossaryDictionaryEntries glossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries);
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, Arrays.asList(glossaryDict));

      TextResult result =
          deepLClient.translateText(
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
          deepLClient.translateText(
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
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();
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

      MultilingualGlossaryDictionaryEntries glossaryDictEnDe =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, glossaryEntriesEnDe);
      MultilingualGlossaryDictionaryEntries glossaryDictDeEn =
          new MultilingualGlossaryDictionaryEntries(targetLang, sourceLang, glossaryEntriesDeEn);

      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(
              glossaryName, Arrays.asList(glossaryDictEnDe, glossaryDictDeEn));

      List<TextResult> result =
          deepLClient.translateText(
              textsEn, "en", "de", new TextTranslationOptions().setGlossary(glossary));
      Assertions.assertArrayEquals(
          textsDe.toArray(), result.stream().map(TextResult::getText).toArray());

      result =
          deepLClient.translateText(
              textsDe,
              "de",
              "en-US",
              new TextTranslationOptions().setGlossary(glossary.getGlossaryId()));
      Assertions.assertArrayEquals(
          textsEn.toArray(), result.stream().map(TextResult::getText).toArray());
    }
  }

  @Test
  void testGlossaryTranslateDocument() throws Exception {
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
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
      MultilingualGlossaryDictionaryEntries glossaryDict =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, entries);
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(glossaryName, Arrays.asList(glossaryDict));

      deepLClient.translateDocument(
          inputFile,
          outputFile,
          sourceLang,
          targetLang,
          new DocumentTranslationOptions().setGlossary(glossary));
      Assertions.assertEquals(expectedOutput, readFromFile(outputFile));
      boolean ignored = outputFile.delete();

      deepLClient.translateDocument(
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
    DeepLClient deepLClient = createDeepLClient();
    try (MultilingualGlossaryCleanupUtility cleanup =
        new MultilingualGlossaryCleanupUtility(deepLClient)) {
      String glossaryName = cleanup.getGlossaryName();

      MultilingualGlossaryDictionaryEntries glossaryDictEnDe =
          new MultilingualGlossaryDictionaryEntries(sourceLang, targetLang, testEntries);
      MultilingualGlossaryDictionaryEntries glossaryDictDeEn =
          new MultilingualGlossaryDictionaryEntries(targetLang, sourceLang, testEntries);
      MultilingualGlossaryInfo glossary =
          deepLClient.createMultilingualGlossary(
              glossaryName, Arrays.asList(glossaryDictEnDe, glossaryDictDeEn));

      IllegalArgumentException exception =
          Assertions.assertThrows(
              IllegalArgumentException.class,
              () ->
                  deepLClient.translateText(
                      "test", null, "de", new TextTranslationOptions().setGlossary(glossary)));
      Assertions.assertTrue(exception.getMessage().contains("sourceLang is required"));
    }
  }

  /**
   * Utility function for determining if a list of MultilingualGlossaryDictionaryEntries objects
   * (that have entries) matches a list of MultilingualGlossaryDictionaryInfo (that do not contain
   * entries, but just a count of the number of entries for that glossary dictionary
   */
  private void AssertGlossaryDictionariesEquivalent(
      List<MultilingualGlossaryDictionaryEntries> expectedDicts,
      List<MultilingualGlossaryDictionaryInfo> actualDicts) {
    Assertions.assertEquals(expectedDicts.size(), actualDicts.size());
    for (MultilingualGlossaryDictionaryEntries expectedDict : expectedDicts) {
      MultilingualGlossaryDictionaryInfo actualDict =
          findMatchingDictionary(
              actualDicts,
              expectedDict.getSourceLanguageCode(),
              expectedDict.getTargetLanguageCode());

      Assertions.assertEquals(
          expectedDict.getSourceLanguageCode().toLowerCase(),
          actualDict.getSourceLanguageCode().toLowerCase());
      Assertions.assertEquals(
          expectedDict.getTargetLanguageCode().toLowerCase(),
          actualDict.getTargetLanguageCode().toLowerCase());
      Assertions.assertEquals(
          expectedDict.getEntries().entrySet().size(), actualDict.getEntryCount());
    }
  }

  private MultilingualGlossaryDictionaryInfo findMatchingDictionary(
      List<MultilingualGlossaryDictionaryInfo> glossaryDicts,
      String sourceLang,
      String targetLang) {
    String lowerCaseSourceLang = sourceLang.toLowerCase();
    String lowerCaseTargetLang = targetLang.toLowerCase();
    for (MultilingualGlossaryDictionaryInfo glossaryDict : glossaryDicts) {
      if (glossaryDict.getSourceLanguageCode().equals(lowerCaseSourceLang)
          && glossaryDict.getTargetLanguageCode().equals(lowerCaseTargetLang)) {
        return glossaryDict;
      }
    }
    Assertions.fail("glossary did not contain expected language pair $sourceLang->$targetLang");
    return null;
  }
}
