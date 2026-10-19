// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsageTest {
  @Test
  void sendsGetV2UsageAndParsesTheUsage() throws Exception {
    Usage usage = RealApiClient.createTranslator().getUsage();

    assertTrue(usage.toString().contains("Usage this billing period"));
  }
}
