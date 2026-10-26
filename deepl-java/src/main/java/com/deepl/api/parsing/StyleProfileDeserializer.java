// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.StyleProfile;
import com.google.gson.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Parses style profiles tolerantly: a malformed timestamp or ID list entry does not fail the call.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class StyleProfileDeserializer implements JsonDeserializer<StyleProfile> {
  public StyleProfile deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (!json.isJsonObject()) {
      return null;
    }
    JsonObject jsonObject = json.getAsJsonObject();
    JsonElement priorityOrder = jsonObject.get("glossary_priority_order_enabled");
    boolean glossaryPriorityOrderEnabled =
        priorityOrder != null
            && priorityOrder.isJsonPrimitive()
            && priorityOrder.getAsJsonPrimitive().isBoolean()
            && priorityOrder.getAsBoolean();
    return new StyleProfile(
        Parser.getAsStringOrNull(jsonObject, "style_profile_id"),
        Parser.getAsStringOrNull(jsonObject, "name"),
        Parser.getAsStringOrNull(jsonObject, "description"),
        Parser.getAsStringOrNull(jsonObject, "industry"),
        getStringList(jsonObject, "glossary_ids"),
        getStringList(jsonObject, "style_rule_ids"),
        getStringList(jsonObject, "translation_memory_ids"),
        glossaryPriorityOrderEnabled,
        Parser.getAsDateOrNull(jsonObject, "creation_time", context),
        Parser.getAsDateOrNull(jsonObject, "updated_time", context));
  }

  private static List<String> getStringList(JsonObject jsonObject, String parameterName) {
    List<String> result = new ArrayList<>();
    JsonElement element = jsonObject.get(parameterName);
    if (element == null || !element.isJsonArray()) {
      return result;
    }
    for (JsonElement item : element.getAsJsonArray()) {
      if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
        result.add(item.getAsString());
      }
    }
    return result;
  }
}
