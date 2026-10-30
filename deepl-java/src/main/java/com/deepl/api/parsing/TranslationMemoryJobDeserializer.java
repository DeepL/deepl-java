// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Deserializer for {@link TranslationMemoryJob} objects, flattening the nested <code>parameters
 * </code>, <code>source_file</code>, <code>status_metadata</code> and <code>error</code> objects of
 * the API response onto the job and its results.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class TranslationMemoryJobDeserializer implements JsonDeserializer<TranslationMemoryJob> {
  @Override
  public TranslationMemoryJob deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();
    JsonObject parameters = getAsObjectOrNull(jsonObject, "parameters");
    JsonObject sourceFile = getAsObjectOrNull(jsonObject, "source_file");

    List<TranslationMemoryJobResult> results = new ArrayList<>();
    if (jsonObject.has("results") && jsonObject.get("results").isJsonArray()) {
      for (JsonElement result : jsonObject.get("results").getAsJsonArray()) {
        results.add(deserializeResult(result.getAsJsonObject(), context));
      }
    }

    return new TranslationMemoryJob(
        Parser.getAsStringOrNull(jsonObject, "job_id"),
        Parser.getAsStringOrNull(jsonObject, "product"),
        context.deserialize(jsonObject.get("operation"), TranslationMemoryJob.Operation.class),
        results,
        deserializeDate(jsonObject, "creation_time", context),
        deserializeDate(jsonObject, "updated_time", context),
        parameters == null ? null : Parser.getAsStringOrNull(parameters, "translation_memory_id"),
        parameters == null ? null : Parser.getAsStringOrNull(parameters, "display_name"),
        sourceFile == null ? null : Parser.getAsStringOrNull(sourceFile, "content_type"),
        sourceFile == null ? null : Parser.getAsLongOrNull(sourceFile, "content_length"));
  }

  private static TranslationMemoryJobResult deserializeResult(
      JsonObject jsonObject, JsonDeserializationContext context) {
    JsonObject statusMetadata = getAsObjectOrNull(jsonObject, "status_metadata");
    JsonObject error = getAsObjectOrNull(jsonObject, "error");

    return new TranslationMemoryJobResult(
        context.deserialize(jsonObject.get("status"), TranslationMemoryJobResult.Status.class),
        Parser.getAsStringOrNull(jsonObject, "status"),
        statusMetadata == null ? null : Parser.getAsStringOrNull(statusMetadata, "required_action"),
        Parser.getAsStringOrNull(jsonObject, "download_url"),
        deserializeDate(jsonObject, "expires_at", context),
        error == null ? null : Parser.getAsStringOrNull(error, "message"),
        Parser.getAsStringOrNull(jsonObject, "translation_memory_id"),
        Parser.getAsIntOrNull(jsonObject, "skipped_segment_count"));
  }

  private static Date deserializeDate(
      JsonObject jsonObject, String parameterName, JsonDeserializationContext context) {
    if (!jsonObject.has(parameterName) || jsonObject.get(parameterName).isJsonNull()) {
      return null;
    }
    return context.deserialize(jsonObject.get(parameterName), Date.class);
  }

  private static JsonObject getAsObjectOrNull(JsonObject jsonObject, String parameterName) {
    if (!jsonObject.has(parameterName) || !jsonObject.get(parameterName).isJsonObject()) {
      return null;
    }
    return jsonObject.get(parameterName).getAsJsonObject();
  }
}
