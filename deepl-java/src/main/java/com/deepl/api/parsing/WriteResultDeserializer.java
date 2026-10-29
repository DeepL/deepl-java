// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.WriteResult;
import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * Utility class for deserializing text rephrase results returned by the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class WriteResultDeserializer implements JsonDeserializer<WriteResult> {
  public WriteResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (!json.isJsonObject()) {
      throw new JsonParseException("Write API improvement is not an object");
    }
    JsonObject jsonObject = json.getAsJsonObject();
    return new WriteResult(
        getRequiredString(jsonObject, "text"),
        getRequiredString(jsonObject, "detected_source_language"),
        getRequiredString(jsonObject, "target_language"));
  }

  private static String getRequiredString(JsonObject jsonObject, String parameterName) {
    String value = Parser.getAsStringOrNull(jsonObject, parameterName);
    if (value == null) {
      throw new JsonParseException("Write API improvement is missing " + parameterName);
    }
    return value;
  }
}
