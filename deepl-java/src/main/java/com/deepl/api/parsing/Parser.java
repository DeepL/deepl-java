// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.*;
import com.google.gson.*;
import com.google.gson.reflect.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import org.jetbrains.annotations.*;

/**
 * Parsing functions for responses from the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
public class Parser {
  private final Gson gson;
  private final AdminResponseParser adminParser;

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
    gsonBuilder.registerTypeAdapter(StyleProfile.class, new StyleProfileDeserializer());
    gson = gsonBuilder.create();
    adminParser =
        new AdminResponseParser(
            gson.newBuilder()
                .registerTypeAdapter(Date.class, new LenientDateDeserializer())
                .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
                .create());
  }

  public List<TextResult> parseTextResult(String json) throws DeepLException {
    return parse(
        () -> {
          TextResponse result = gson.fromJson(json, TextResponse.class);
          return result.translations;
        });
  }

  public List<WriteResult> parseWriteResult(String json) throws DeepLException {
    return parse(
        () -> {
          WriteResponse result = gson.fromJson(json, WriteResponse.class);
          return result.improvements;
        });
  }

  public Usage parseUsage(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, Usage.class));
  }

  public List<Language> parseV3Languages(String json, boolean target) throws DeepLException {
    return parse(
        () -> {
          Type v3LanguageListType = new TypeToken<ArrayList<V3Language>>() {}.getType();
          List<V3Language> entries = gson.fromJson(json, v3LanguageListType);
          List<Language> result = new ArrayList<>();
          for (V3Language entry : entries) {
            if (target) {
              if (!entry.isUsableAsTarget()) continue;
              result.add(
                  new Language(entry.getName(), entry.getLang(), entry.hasFeature("formality")));
            } else {
              if (!entry.isUsableAsSource()) continue;
              result.add(new Language(entry.getName(), entry.getLang(), null));
            }
          }
          return result;
        });
  }

  public List<LanguageSupport> parseLanguageSupportList(String json) throws DeepLException {
    return parse(
        () -> {
          Type listType = new TypeToken<ArrayList<LanguageSupport>>() {}.getType();
          return gson.fromJson(json, listType);
        });
  }

  public List<LanguageResource> parseLanguageResourceList(String json) throws DeepLException {
    return parse(
        () -> {
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
        });
  }

  public List<GlossaryLanguagePair> parseV3GlossaryLanguagePairs(String json)
      throws DeepLException {
    return parse(
        () -> {
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
        });
  }

  public DocumentStatus parseDocumentStatus(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, DocumentStatus.class));
  }

  public DocumentHandle parseDocumentHandle(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, DocumentHandle.class));
  }

  public GlossaryInfo parseGlossaryInfo(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, GlossaryInfo.class));
  }

  public MultilingualGlossaryInfo parseMultilingualGlossaryInfo(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, MultilingualGlossaryInfo.class));
  }

  public MultilingualGlossaryDictionaryListResponse parseMultilingualGlossaryDictionaryListResponse(
      String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, MultilingualGlossaryDictionaryListResponse.class));
  }

  public List<GlossaryInfo> parseGlossaryInfoList(String json) throws DeepLException {
    return parse(
        () -> {
          GlossaryListResponse result = gson.fromJson(json, GlossaryListResponse.class);
          return result.getGlossaries();
        });
  }

  public List<MultilingualGlossaryInfo> parseMultilingualGlossaryInfoList(String json)
      throws DeepLException {
    return parse(
        () -> {
          MultilingualGlossaryListResponse result =
              gson.fromJson(json, MultilingualGlossaryListResponse.class);
          return result.getGlossaries();
        });
  }

  public MultilingualGlossaryDictionaryInfo parseMultilingualGlossaryDictionaryInfo(String json)
      throws DeepLException {
    return parse(() -> gson.fromJson(json, MultilingualGlossaryDictionaryInfo.class));
  }

  public List<StyleRuleInfo> parseStyleRuleInfoList(String json) throws DeepLException {
    return parse(
        () -> {
          StyleRuleListResponse result = gson.fromJson(json, StyleRuleListResponse.class);
          return result.getStyleRules();
        });
  }

  public StyleRuleInfo parseStyleRuleInfo(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, StyleRuleInfo.class));
  }

  public List<TranslationMemoryInfo> parseTranslationMemoryInfoList(String json)
      throws DeepLException {
    return parse(
        () -> {
          TranslationMemoryListResponse result =
              gson.fromJson(json, TranslationMemoryListResponse.class);
          return result.getTranslationMemories();
        });
  }

  public TranslationMemoryInfo parseTranslationMemoryInfo(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, TranslationMemoryInfo.class));
  }

  public TranslationMemorySegments parseTranslationMemorySegments(String json)
      throws DeepLException {
    return parse(() -> gson.fromJson(json, TranslationMemorySegments.class));
  }

  public TranslationMemoryImport parseTranslationMemoryImport(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, TranslationMemoryImport.class));
  }

  public TranslationMemoryExport parseTranslationMemoryExport(String json, boolean reusedExisting)
      throws DeepLException {
    return parse(
        () -> {
          TranslationMemoryExportResponse response =
              gson.fromJson(json, TranslationMemoryExportResponse.class);
          return new TranslationMemoryExport(
              response.getJobId(), response.getTranslationMemoryId(), reusedExisting);
        });
  }

  public TranslationMemoryJob parseTranslationMemoryJob(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, TranslationMemoryJob.class));
  }

  public CustomInstruction parseCustomInstruction(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, CustomInstruction.class));
  }

  public StyleProfile parseStyleProfile(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, StyleProfile.class));
  }

  public List<StyleProfile> parseStyleProfileList(String json) throws DeepLException {
    return parse(
        () -> {
          StyleProfileListResponse result = gson.fromJson(json, StyleProfileListResponse.class);
          List<StyleProfile> styleProfiles = new ArrayList<>();
          if (result != null && result.getStyleProfiles() != null) {
            for (StyleProfile styleProfile : result.getStyleProfiles()) {
              if (styleProfile != null) {
                styleProfiles.add(styleProfile);
              }
            }
          }
          return styleProfiles;
        });
  }

  public SpokenTerms parseSpokenTerms(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, SpokenTerms.class));
  }

  public List<SpokenTerms> parseSpokenTermsList(String json) throws DeepLException {
    return parse(
        () -> {
          SpokenTermsListResponse result = gson.fromJson(json, SpokenTermsListResponse.class);
          List<SpokenTerms> spokenTerms = result != null ? result.getSpokenTerms() : null;
          return spokenTerms != null ? spokenTerms : new ArrayList<>();
        });
  }

  public SpokenTermsList parseSpokenTermsTermList(String json) throws DeepLException {
    return parse(() -> gson.fromJson(json, SpokenTermsList.class));
  }

  public SpokenTermsEntries parseSpokenTermsEntries(String json) throws DeepLException {
    return parse(
        () -> {
          SpokenTermsEntriesResponse response =
              gson.fromJson(json, SpokenTermsEntriesResponse.class);
          List<String> entries = new ArrayList<>();
          if (response.getEntries() != null) {
            for (String entry : response.getEntries().split("\n")) {
              if (!entry.isEmpty()) {
                entries.add(entry);
              }
            }
          }
          return new SpokenTermsEntries(response.getLang(), entries);
        });
  }

  public DeveloperKey parseDeveloperKey(String json) throws DeepLException {
    return parseAdmin(json, adminParser::parseDeveloperKey);
  }

  public List<DeveloperKey> parseDeveloperKeyList(String json) throws DeepLException {
    return parseAdmin(json, adminParser::parseDeveloperKeys);
  }

  public AdminUsageReport parseAdminUsageReport(String json) throws DeepLException {
    return parseAdmin(json, adminParser::parseUsageReport);
  }

  public CustomTagUsageReport parseCustomTagUsageReport(String json) throws DeepLException {
    return parseAdmin(json, adminParser::parseCustomTagUsageReport);
  }

  // B8: a malformed or empty response raises DeepLException, never a raw JSON or null error.
  private static <T> T parse(java.util.function.Supplier<T> parse) throws DeepLException {
    T result;
    try {
      result = parse.get();
    } catch (RuntimeException e) {
      throw new DeepLException(
          "Failed to parse the response of the DeepL API: " + e.getMessage(), e);
    }
    if (result == null) {
      throw new DeepLException("Failed to parse the response of the DeepL API: empty response");
    }
    return result;
  }

  private <T> T parseAdmin(String json, java.util.function.Function<JsonElement, T> parse)
      throws DeepLException {
    try {
      return parse.apply(json == null ? JsonNull.INSTANCE : JsonParser.parseString(json));
    } catch (JsonParseException e) {
      throw new DeepLException(
          "Failed to parse the response of the DeepL API: " + e.getMessage(), e);
    }
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

  /** Parses a timestamp tolerantly: a missing, null or unparseable value yields null. */
  static @Nullable Date getAsDateOrNull(
      JsonObject jsonObject, String parameterName, JsonDeserializationContext context) {
    if (!jsonObject.has(parameterName) || jsonObject.get(parameterName).isJsonNull()) {
      return null;
    }
    try {
      return context.deserialize(jsonObject.get(parameterName), Date.class);
    } catch (JsonParseException e) {
      return null;
    }
  }
}
