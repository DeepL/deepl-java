// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.TextResult;
import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * Utility class for deserializing text translation results returned by the DeepL API.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class TextResultDeserializer implements JsonDeserializer<TextResult> {
  public TextResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();
    Integer billedCharacters = Parser.getAsIntOrNull(jsonObject, "billed_characters");
    return new TextResult(
        jsonObject.get("text").getAsString(),
        jsonObject.get("detected_source_language").getAsString(),
        billedCharacters != null ? billedCharacters : 0,
        Parser.getAsStringOrNull(jsonObject, "model_type_used"),
        stringOrNull(jsonObject.get("tag_handling_version")));
  }

  /** Parses a new response field tolerantly: anything but a JSON string or number yields null. */
  private static String stringOrNull(JsonElement element) {
    return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
  }
}
