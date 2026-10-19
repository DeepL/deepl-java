// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/** Names for the resources a test run creates, so that leftovers can be told apart. */
final class RealApiNames {
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final String RUN_ID = createRunId();

  private RealApiNames() {}

  /** Returns a name such as sdktest-java-202610151530-k3f9qz-glossary, unique to this run. */
  static String getUniqueName(String label) {
    return "sdktest-java-" + RUN_ID + "-" + label;
  }

  private static String createRunId() {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    SecureRandom random = new SecureRandom();
    StringBuilder rand = new StringBuilder();
    for (int i = 0; i < 6; i++) {
      rand.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return format.format(new Date()) + "-" + rand;
  }
}
