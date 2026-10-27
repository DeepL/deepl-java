// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.google.gson.*;
import java.lang.reflect.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;

/**
 * Deserializes an ISO 8601 date, or a timestamp at the start of a day with or without a time zone,
 * into a {@link LocalDate}. Timestamps with a time zone are converted to UTC first. Malformed
 * values become <code>null</code>.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
  private static final DateTimeFormatter FORMATTER =
      new DateTimeFormatterBuilder()
          .append(DateTimeFormatter.ISO_LOCAL_DATE)
          .optionalStart()
          .appendLiteral('T')
          .append(DateTimeFormatter.ISO_LOCAL_TIME)
          .optionalStart()
          .appendOffsetId()
          .toFormatter();

  public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (!json.isJsonPrimitive()) {
      return null;
    }
    try {
      String value = json.getAsString();
      TemporalAccessor parsed = FORMATTER.parseBest(value, OffsetDateTime::from, LocalDate::from);
      if (parsed instanceof OffsetDateTime) {
        return ((OffsetDateTime) parsed).withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
      }
      return (LocalDate) parsed;
    } catch (DateTimeParseException exception) {
      return null;
    }
  }
}
