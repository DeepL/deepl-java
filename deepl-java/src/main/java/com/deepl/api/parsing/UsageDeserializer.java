// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.util.*;
import org.jetbrains.annotations.*;

/**
 * Class representing usage responses from the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class UsageDeserializer implements JsonDeserializer<Usage> {
  public Usage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    return new Usage(
        createDetail(jsonObject, "character_"),
        createDetail(jsonObject, "document_"),
        createDetail(jsonObject, "team_document_"),
        createDetail(jsonObject, "api_key_character_"),
        createDetail(jsonObject, "speech_to_text_minutes_"),
        createDetail(jsonObject, "speech_to_speech_minutes_"),
        createProducts(jsonObject),
        deserializeDate(jsonObject, "start_time", context),
        deserializeDate(jsonObject, "end_time", context));
  }

  private static @Nullable List<Usage.Product> createProducts(JsonObject jsonObject) {
    JsonElement element = jsonObject.get("products");
    if (element == null || !element.isJsonArray()) return null;
    List<Usage.Product> products = new ArrayList<>();
    for (JsonElement item : element.getAsJsonArray()) {
      if (!item.isJsonObject()) continue;
      JsonObject product = item.getAsJsonObject();
      Long apiKeyUnitCount = Parser.getAsLongOrNull(product, "api_key_unit_count");
      Long accountUnitCount = Parser.getAsLongOrNull(product, "account_unit_count");
      products.add(
          new Usage.Product(
              Parser.getAsStringOrNull(product, "product_type"),
              Parser.getAsStringOrNull(product, "billing_unit"),
              apiKeyUnitCount == null ? 0 : apiKeyUnitCount,
              accountUnitCount == null ? 0 : accountUnitCount));
    }
    return products;
  }

  private static @Nullable Date deserializeDate(
      JsonObject jsonObject, String parameterName, JsonDeserializationContext context) {
    if (!jsonObject.has(parameterName) || jsonObject.get(parameterName).isJsonNull()) {
      return null;
    }
    return context.deserialize(jsonObject.get(parameterName), Date.class);
  }

  public static @Nullable Usage.Detail createDetail(JsonObject jsonObject, String prefix) {
    Long count = Parser.getAsLongOrNull(jsonObject, prefix + "count");
    Long limit = Parser.getAsLongOrNull(jsonObject, prefix + "limit");
    if (count == null || limit == null) return null;
    return new Usage.Detail(count, limit);
  }
}
