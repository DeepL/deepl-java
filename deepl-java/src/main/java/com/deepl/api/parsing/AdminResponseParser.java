// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.deepl.api.*;
import com.google.gson.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import org.jetbrains.annotations.*;

/**
 * Parses Admin API responses tolerantly: entries that are not objects or have no key_id are
 * skipped, missing optional fields (labels, usage limits, dates, a report's total usage and
 * speech-to-speech minutes) are null, and other missing or malformed fields get neutral defaults. A
 * response of the wrong overall shape, or a single key without key_id, is an error.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class AdminResponseParser {
  private final Gson gson;

  AdminResponseParser(Gson gson) {
    this.gson = gson;
  }

  DeveloperKey parseDeveloperKey(@Nullable JsonElement json) {
    if (json == null || !json.isJsonObject() || usableKeyId(json.getAsJsonObject()) == null) {
      throw new JsonParseException("Developer key response is missing key_id");
    }
    return parseKey(json.getAsJsonObject());
  }

  private DeveloperKey parseKey(JsonObject object) {
    JsonElement limitsElement = object.get("usage_limits");
    JsonObject limits =
        limitsElement != null && limitsElement.isJsonObject()
            ? limitsElement.getAsJsonObject()
            : null;
    return new DeveloperKey(
        string(object, "key_id"),
        stringOrNull(object, "label"),
        date(object, "creation_time"),
        date(object, "deactivated_time"),
        bool(object, "is_deactivated"),
        limits == null
            ? null
            : new DeveloperKeyUsageLimits(
                longOrNull(limits, "characters"),
                longOrNull(limits, "speech_to_text_milliseconds"),
                longOrNull(limits, "speech_to_speech_milliseconds")));
  }

  List<DeveloperKey> parseDeveloperKeys(@Nullable JsonElement json) {
    if (json == null || !json.isJsonArray()) {
      throw new JsonParseException("Developer keys response is not a list");
    }
    List<DeveloperKey> keys = new ArrayList<>();
    for (JsonObject entry : objects(json)) {
      if (usableKeyId(entry) != null) {
        keys.add(parseKey(entry));
      }
    }
    return keys;
  }

  private static @Nullable String usableKeyId(JsonObject object) {
    JsonElement id = object.get("key_id");
    if (id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString()) {
      String value = id.getAsString();
      return value.isEmpty() ? null : value;
    }
    return null;
  }

  private static JsonObject requireObject(@Nullable JsonElement json, String what) {
    if (json == null || !json.isJsonObject()) {
      throw new JsonParseException(what + " response is not an object");
    }
    return json.getAsJsonObject();
  }

  AdminUsageReport parseUsageReport(JsonElement json) {
    JsonObject report = asObject(requireObject(json, "Usage report").get("usage_report"));
    return new AdminUsageReport(
        breakdownOrNull(report.get("total_usage")),
        utcDay(report, "start_date"),
        utcDay(report, "end_date"),
        stringOrNull(report, "group_by"),
        parseKeyUsages(report.get("key_usages")),
        parseKeyUsages(report.get("key_and_day_usages")));
  }

  CustomTagUsageReport parseCustomTagUsageReport(JsonElement json) {
    JsonObject report =
        asObject(requireObject(json, "Custom tag usage report").get("custom_tag_usage_report"));
    List<CustomTagUsage> usage = new ArrayList<>();
    for (JsonObject entry : objects(report.get("usage"))) {
      usage.add(
          new CustomTagUsage(
              string(entry, "custom_tag"),
              parseCustomTagBreakdown(entry.get("breakdown")),
              utcDay(entry, "usage_date")));
    }
    Long nextPage = longOrNull(report, "next_page");
    return new CustomTagUsageReport(
        stringOrNull(report, "aggregate_by"),
        utcDay(report, "start_date"),
        utcDay(report, "end_date"),
        nextPage != null && nextPage >= Integer.MIN_VALUE && nextPage <= Integer.MAX_VALUE
            ? Integer.valueOf((int) (long) nextPage)
            : null,
        usage);
  }

  private List<KeyUsage> parseKeyUsages(@Nullable JsonElement json) {
    List<KeyUsage> usages = new ArrayList<>();
    for (JsonObject entry : objects(json)) {
      usages.add(
          new KeyUsage(
              string(entry, "api_key"),
              stringOrNull(entry, "api_key_label"),
              parseBreakdown(entry.get("usage")),
              utcDay(entry, "usage_date")));
    }
    return usages;
  }

  private AdminUsageBreakdown parseBreakdown(@Nullable JsonElement json) {
    JsonObject object = asObject(json);
    return new AdminUsageBreakdown(
        count(object, "total_characters"),
        count(object, "text_translation_characters"),
        count(object, "document_translation_characters"),
        count(object, "text_improvement_characters"),
        number(object, "speech_to_text_minutes"),
        numberOrNull(object, "speech_to_speech_minutes"));
  }

  private @Nullable AdminUsageBreakdown breakdownOrNull(@Nullable JsonElement json) {
    return json != null && json.isJsonObject() ? parseBreakdown(json) : null;
  }

  private CustomTagUsageBreakdown parseCustomTagBreakdown(@Nullable JsonElement json) {
    JsonObject object = asObject(json);
    return new CustomTagUsageBreakdown(
        count(object, "total_characters"),
        count(object, "text_translation_characters"),
        count(object, "text_improvement_characters"),
        number(object, "speech_to_text_minutes"),
        numberOrNull(object, "speech_to_speech_minutes"));
  }

  private static JsonObject asObject(@Nullable JsonElement json) {
    return json != null && json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
  }

  private static List<JsonObject> objects(@Nullable JsonElement json) {
    List<JsonObject> objects = new ArrayList<>();
    if (json != null && json.isJsonArray()) {
      for (JsonElement item : json.getAsJsonArray()) {
        if (item.isJsonObject()) {
          objects.add(item.getAsJsonObject());
        }
      }
    }
    return objects;
  }

  private static @Nullable String stringOrNull(JsonObject object, String name) {
    try {
      return Parser.getAsStringOrNull(object, name);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static String string(JsonObject object, String name) {
    String value = stringOrNull(object, name);
    return value != null ? value : "";
  }

  private static boolean bool(JsonObject object, String name) {
    JsonElement element = object.get(name);
    return element != null
        && element.isJsonPrimitive()
        && element.getAsJsonPrimitive().isBoolean()
        && element.getAsBoolean();
  }

  private static @Nullable Long longOrNull(JsonObject object, String name) {
    try {
      return Parser.getAsLongOrNull(object, name);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static long count(JsonObject object, String name) {
    Long value = longOrNull(object, name);
    return value != null ? value : 0;
  }

  private static double number(JsonObject object, String name) {
    Double value = numberOrNull(object, name);
    return value != null ? value : 0;
  }

  private static @Nullable Double numberOrNull(JsonObject object, String name) {
    JsonElement element = object.get(name);
    try {
      return element == null || element.isJsonNull() ? null : element.getAsDouble();
    } catch (RuntimeException e) {
      return null;
    }
  }

  private @Nullable Date date(JsonObject object, String name) {
    return gson.fromJson(object.get(name), Date.class);
  }

  /** Parses a report day, sent as a date-time, into midnight UTC of its UTC day. */
  private @Nullable Date utcDay(JsonObject object, String name) {
    LocalDate day = gson.fromJson(object.get(name), LocalDate.class);
    return day != null ? Date.from(day.atStartOfDay(ZoneOffset.UTC).toInstant()) : null;
  }
}
