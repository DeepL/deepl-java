// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.*;
import com.google.gson.*;
import com.google.gson.reflect.*;
import java.lang.reflect.*;
import java.util.*;
import org.jetbrains.annotations.*;

/**
 * Parsing functions for responses from the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
public class Parser {
  private final Gson gson;

  public Gson getGson() {
    return gson;
  }

  public Parser() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(TextResult.class, new TextResultDeserializer());
    gsonBuilder.registerTypeAdapter(WriteResult.class, new WriteResultDeserializer());
    gsonBuilder.registerTypeAdapter(Usage.class, new UsageDeserializer());
    gsonBuilder.registerTypeAdapter(
        TranslationMemoryJob.class, new TranslationMemoryJobDeserializer());
    gson = gsonBuilder.create();
  }

  public List<TextResult> parseTextResult(String json) {
    TextResponse result = gson.fromJson(json, TextResponse.class);
    return result.translations;
  }

  public List<WriteResult> parseWriteResult(String json) {
    WriteResponse result = gson.fromJson(json, WriteResponse.class);
    return result.improvements;
  }

  public Usage parseUsage(String json) {
    return gson.fromJson(json, Usage.class);
  }

  public List<Language> parseV3Languages(String json, LanguageType languageType) {
    Type v3LanguageListType = new TypeToken<ArrayList<V3Language>>() {}.getType();
    List<V3Language> entries = gson.fromJson(json, v3LanguageListType);
    List<Language> result = new ArrayList<>();
    for (V3Language entry : entries) {
      if (languageType == LanguageType.Target) {
        if (!entry.isUsableAsTarget()) continue;
        result.add(new Language(entry.getName(), entry.getLang(), entry.hasFeature("formality")));
      } else {
        if (!entry.isUsableAsSource()) continue;
        result.add(new Language(entry.getName(), entry.getLang(), null));
      }
    }
    return result;
  }

  public List<LanguageSupport> parseLanguageSupportList(String json) {
    Type listType = new TypeToken<ArrayList<LanguageSupport>>() {}.getType();
    return gson.fromJson(json, listType);
  }

  public List<LanguageResource> parseLanguageResourceList(String json) {
    Type listType = new TypeToken<ArrayList<V3LanguageResource>>() {}.getType();
    List<V3LanguageResource> entries = gson.fromJson(json, listType);
    List<LanguageResource> result = new ArrayList<>();
    for (V3LanguageResource entry : entries) {
      Map<String, ResourceFeature> features = new LinkedHashMap<>();
      if (entry.features != null) {
        for (ResourceFeature feature : entry.features) {
          features.put(feature.getName(), feature);
        }
      }
      result.add(new LanguageResource(entry.name, features));
    }
    return result;
  }

  public List<GlossaryLanguagePair> parseV3GlossaryLanguagePairs(String json) {
    Type v3LanguageListType = new TypeToken<ArrayList<V3Language>>() {}.getType();
    List<V3Language> entries = gson.fromJson(json, v3LanguageListType);
    List<String> sourceCodes = new ArrayList<>();
    List<String> targetCodes = new ArrayList<>();
    for (V3Language entry : entries) {
      if (entry.isUsableAsSource()) sourceCodes.add(entry.getLang());
      if (entry.isUsableAsTarget()) targetCodes.add(entry.getLang());
    }
    List<GlossaryLanguagePair> pairs = new ArrayList<>();
    for (String source : sourceCodes) {
      for (String target : targetCodes) {
        if (!source.equals(target)) {
          pairs.add(new GlossaryLanguagePair(source, target));
        }
      }
    }
    return pairs;
  }

  public DocumentStatus parseDocumentStatus(String json) {
    return gson.fromJson(json, DocumentStatus.class);
  }

  public DocumentHandle parseDocumentHandle(String json) {
    return gson.fromJson(json, DocumentHandle.class);
  }

  public GlossaryInfo parseGlossaryInfo(String json) {
    return gson.fromJson(json, GlossaryInfo.class);
  }

  public MultilingualGlossaryInfo parseMultilingualGlossaryInfo(String json) {
    return gson.fromJson(json, MultilingualGlossaryInfo.class);
  }

  public MultilingualGlossaryDictionaryListResponse parseMultilingualGlossaryDictionaryListResponse(
      String json) {
    return gson.fromJson(json, MultilingualGlossaryDictionaryListResponse.class);
  }

  public List<GlossaryInfo> parseGlossaryInfoList(String json) {
    GlossaryListResponse result = gson.fromJson(json, GlossaryListResponse.class);
    return result.getGlossaries();
  }

  public List<MultilingualGlossaryInfo> parseMultilingualGlossaryInfoList(String json) {
    MultilingualGlossaryListResponse result =
        gson.fromJson(json, MultilingualGlossaryListResponse.class);
    return result.getGlossaries();
  }

  public MultilingualGlossaryDictionaryInfo parseMultilingualGlossaryDictionaryInfo(String json) {
    return gson.fromJson(json, MultilingualGlossaryDictionaryInfo.class);
  }

  public List<StyleRuleInfo> parseStyleRuleInfoList(String json) {
    StyleRuleListResponse result = gson.fromJson(json, StyleRuleListResponse.class);
    return result.getStyleRules();
  }

  public StyleRuleInfo parseStyleRuleInfo(String json) {
    return gson.fromJson(json, StyleRuleInfo.class);
  }

  public List<TranslationMemoryInfo> parseTranslationMemoryInfoList(String json) {
    TranslationMemoryListResponse result = gson.fromJson(json, TranslationMemoryListResponse.class);
    return result.getTranslationMemories();
  }

  public TranslationMemoryInfo parseTranslationMemoryInfo(String json) {
    return gson.fromJson(json, TranslationMemoryInfo.class);
  }

  public TranslationMemorySegments parseTranslationMemorySegments(String json) {
    return gson.fromJson(json, TranslationMemorySegments.class);
  }

  public TranslationMemoryImport parseTranslationMemoryImport(String json) {
    return gson.fromJson(json, TranslationMemoryImport.class);
  }

  public TranslationMemoryExport parseTranslationMemoryExport(String json, boolean reusedExisting) {
    TranslationMemoryExportResponse response =
        gson.fromJson(json, TranslationMemoryExportResponse.class);
    return new TranslationMemoryExport(
        response.getJobId(), response.getTranslationMemoryId(), reusedExisting);
  }

  public TranslationMemoryJob parseTranslationMemoryJob(String json) {
    return gson.fromJson(json, TranslationMemoryJob.class);
  }

  public CustomInstruction parseCustomInstruction(String json) {
    return gson.fromJson(json, CustomInstruction.class);
  }

  public SpokenTerms parseSpokenTerms(String json) {
    return gson.fromJson(json, SpokenTerms.class);
  }

  public List<SpokenTerms> parseSpokenTermsList(String json) {
    SpokenTermsListResponse result = gson.fromJson(json, SpokenTermsListResponse.class);
    List<SpokenTerms> spokenTerms = result != null ? result.getSpokenTerms() : null;
    return spokenTerms != null ? spokenTerms : new ArrayList<>();
  }

  public SpokenTermsList parseSpokenTermsTermList(String json) {
    return gson.fromJson(json, SpokenTermsList.class);
  }

  public SpokenTermsEntries parseSpokenTermsEntries(String json) {
    SpokenTermsEntriesResponse response = gson.fromJson(json, SpokenTermsEntriesResponse.class);
    List<String> entries = new ArrayList<>();
    if (response.getEntries() != null) {
      for (String entry : response.getEntries().split("\n")) {
        if (!entry.isEmpty()) {
          entries.add(entry);
        }
      }
    }
    return new SpokenTermsEntries(response.getLang(), entries);
  }

  public String parseErrorMessage(String json) {
    ErrorResponse response = gson.fromJson(json, ErrorResponse.class);

    if (response != null) {
      return response.getErrorMessage();
    } else {
      return "";
    }
  }

  static @Nullable Integer getAsIntOrNull(JsonObject jsonObject, String parameterName) {
    // An explicit JSON null must be treated as absent: getAsX() throws on JsonNull.
    if (!jsonObject.has(parameterName) || jsonObject.get(parameterName).isJsonNull()) return null;
    return jsonObject.get(parameterName).getAsInt();
  }

  static @Nullable Long getAsLongOrNull(JsonObject jsonObject, String parameterName) {
    // An explicit JSON null must be treated as absent: getAsX() throws on JsonNull.
    if (!jsonObject.has(parameterName) || jsonObject.get(parameterName).isJsonNull()) return null;
    return jsonObject.get(parameterName).getAsLong();
  }

  static @Nullable String getAsStringOrNull(JsonObject jsonObject, String parameterName) {
    // An explicit JSON null must be treated as absent: getAsX() throws on JsonNull.
    if (!jsonObject.has(parameterName) || jsonObject.get(parameterName).isJsonNull()) return null;
    return jsonObject.get(parameterName).getAsString();
  }

  static @Nullable Boolean getAsBooleanOrNull(JsonObject jsonObject, String parameterName) {
    if (!jsonObject.has(parameterName)) return null;
    return jsonObject.get(parameterName).getAsBoolean();
  }
}
