// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api.parsing;

import com.google.gson.*;
import java.lang.reflect.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/**
 * Deserializes ISO 8601 timestamps with or without a time zone into a {@link Date}. Timestamps
 * without a time zone are interpreted as UTC. Malformed values become <code>null</code>.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class LenientDateDeserializer implements JsonDeserializer<Date> {
  public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (!json.isJsonPrimitive()) {
      return null;
    }
    try {
      String value = json.getAsString();
      TemporalAccessor parsed =
          DateTimeFormatter.ISO_DATE_TIME.parseBest(
              value, OffsetDateTime::from, LocalDateTime::from);
      Instant instant =
          parsed instanceof OffsetDateTime
              ? ((OffsetDateTime) parsed).toInstant()
              : ((LocalDateTime) parsed).toInstant(ZoneOffset.UTC);
      return Date.from(instant);
    } catch (DateTimeParseException exception) {
      return null;
    }
  }
}
