// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.*;
import org.junit.jupiter.api.*;

public class LanguagesTest extends TestBase {
  private static final String[] RESOURCES = {
    LanguageResource.RESOURCE_TRANSLATE_TEXT,
    LanguageResource.RESOURCE_TRANSLATE_DOCUMENT,
    LanguageResource.RESOURCE_GLOSSARY,
    LanguageResource.RESOURCE_WRITE,
    LanguageResource.RESOURCE_VOICE,
    LanguageResource.RESOURCE_STYLE_RULES,
    LanguageResource.RESOURCE_TRANSLATION_MEMORY,
  };

  @Test
  void testGetLanguagesForResource() throws DeepLException, InterruptedException {
    DeepLClient client = createDeepLClient();
    for (String resource : RESOURCES) {
      List<LanguageSupport> languages = client.getLanguagesForResource(resource);
      Assertions.assertFalse(languages.isEmpty(), resource);
      for (LanguageSupport language : languages) {
        Assertions.assertFalse(language.getCode().isEmpty());
        Assertions.assertFalse(language.getName().isEmpty());
        Assertions.assertFalse(language.getStatus().isEmpty());
        Assertions.assertTrue(language.isUsableAsSource() || language.isUsableAsTarget());
        for (Map.Entry<String, LanguageFeature> feature : language.getFeatures().entrySet()) {
          Assertions.assertFalse(feature.getKey().isEmpty());
          Assertions.assertFalse(feature.getValue().getStatus().isEmpty());
          Assertions.assertTrue(language.supports(feature.getKey()));
        }
      }
    }
  }

  @Test
  void testGetLanguagesForResourceTranslateText() throws DeepLException, InterruptedException {
    DeepLClient client = createDeepLClient();
    Map<String, LanguageSupport> byCode = new HashMap<>();
    for (LanguageSupport language :
        client.getLanguagesForResource(LanguageResource.RESOURCE_TRANSLATE_TEXT)) {
      byCode.put(language.getCode(), language);
    }
    Assertions.assertTrue(byCode.containsKey("de"));
    Assertions.assertTrue(byCode.containsKey("en-US"));
    Assertions.assertTrue(byCode.containsKey("pt-BR"));
    LanguageSupport german = byCode.get("de");
    Assertions.assertTrue(german.isUsableAsSource());
    Assertions.assertTrue(german.isUsableAsTarget());
    Assertions.assertTrue(german.supports(LanguageFeature.FEATURE_FORMALITY));
  }

  @Test
  void testGetLanguagesForResourceWithInclude() throws DeepLException, InterruptedException {
    DeepLClient client = createDeepLClient();
    List<LanguageSupport> stable =
        client.getLanguagesForResource(LanguageResource.RESOURCE_TRANSLATE_TEXT);
    List<LanguageSupport> withBeta =
        client.getLanguagesForResource(
            LanguageResource.RESOURCE_TRANSLATE_TEXT,
            Arrays.asList(LanguageSupport.INCLUDE_BETA, LanguageSupport.INCLUDE_EXTERNAL));
    Assertions.assertTrue(withBeta.size() >= stable.size());
  }

  @Test
  void testGetLanguagesForResourceInvalid() {
    DeepLClient client = createDeepLClient();
    Assertions.assertThrows(
        DeepLException.class, () -> client.getLanguagesForResource("not_a_resource"));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> client.getLanguagesForResource(""));
  }

  @Test
  void testGetLanguageResources() throws DeepLException, InterruptedException {
    DeepLClient client = createDeepLClient();
    List<LanguageResource> resources = client.getLanguageResources();
    Assertions.assertFalse(resources.isEmpty());
    Map<String, LanguageResource> byName = new HashMap<>();
    for (LanguageResource resource : resources) {
      Assertions.assertFalse(resource.getResource().isEmpty());
      for (Map.Entry<String, ResourceFeature> feature : resource.getFeatures().entrySet()) {
        Assertions.assertEquals(feature.getKey(), feature.getValue().getName());
      }
      byName.put(resource.getResource(), resource);
    }
    LanguageResource translateText = byName.get(LanguageResource.RESOURCE_TRANSLATE_TEXT);
    Assertions.assertNotNull(translateText);
    Assertions.assertTrue(translateText.supports(LanguageFeature.FEATURE_GLOSSARY));
    ResourceFeature glossary = translateText.getFeatures().get(LanguageFeature.FEATURE_GLOSSARY);
    Assertions.assertEquals(Boolean.TRUE, glossary.getNeedsSourceSupport());
    Assertions.assertEquals(Boolean.TRUE, glossary.getNeedsTargetSupport());
  }
}
