// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LanguagesTest {
  @Test
  void sendsGetV3LanguagesAndParsesTheSourceLanguages() throws Exception {
    List<Language> languages = RealApiClient.createTranslator().getSourceLanguages();

    assertEquals(1, languages.stream().filter(l -> l.getCode().equals("en")).count());
    for (Language language : languages) {
      assertFalse(language.getName().isEmpty());
      assertFalse(language.getCode().isEmpty());
      assertNull(language.getSupportsFormality());
    }
  }

  @Test
  void sendsGetV3LanguagesResourcesAndParsesTheResourcesAndTheirFeatures() throws Exception {
    List<LanguageResource> resources = RealApiClient.createClient().getLanguageResources();

    assertFalse(resources.isEmpty());
    LanguageResource translateText = null;
    for (LanguageResource resource : resources) {
      assertFalse(resource.getName().isEmpty());
      for (Map.Entry<String, ResourceFeature> feature : resource.getFeatures().entrySet()) {
        assertEquals(feature.getKey(), feature.getValue().getName());
      }
      if (resource.getName().equals(LanguageResource.RESOURCE_TRANSLATE_TEXT)) {
        translateText = resource;
      }
    }
    assertNotNull(translateText);
    ResourceFeature glossary = translateText.getFeatures().get(LanguageFeature.FEATURE_GLOSSARY);
    assertEquals(Boolean.TRUE, glossary.getNeedsSourceSupport());
    assertEquals(Boolean.TRUE, glossary.getNeedsTargetSupport());
    assertTrue(translateText.supports(LanguageFeature.FEATURE_GLOSSARY));
  }
}
