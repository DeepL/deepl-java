// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.concurrent.Callable;

/** Creates clients for the API at DEEPL_SERVER_URL, using DEEPL_AUTH_KEY. */
final class RealApiClient {
  private RealApiClient() {}

  static String authKey() {
    String key = System.getenv("DEEPL_AUTH_KEY");
    if (key == null || key.isEmpty()) {
      throw new IllegalStateException("The DEEPL_AUTH_KEY environment variable must be set");
    }
    return key;
  }

  /** The URL of the API the tests talk to: DEEPL_SERVER_URL, or else the SDK's default. */
  static String serverUrl() {
    String url = System.getenv("DEEPL_SERVER_URL");
    if (url != null && !url.isEmpty()) {
      return url;
    }
    return Translator.isFreeAccountAuthKey(authKey())
        ? "https://api-free.deepl.com"
        : "https://api.deepl.com";
  }

  static DeepLClient createClient() {
    return createClient(new DeepLClientOptions());
  }

  static DeepLClient createClient(DeepLClientOptions options) {
    options.setServerUrl(serverUrl());
    return new DeepLClient(authKey(), options);
  }

  static Translator createTranslator() {
    return createClient();
  }

  /** Calls {@code call}, retrying while a just-created glossary is not yet visible. */
  static <T> T retryWhileGlossaryNotFound(Callable<T> call) throws Exception {
    for (int attempt = 0; ; attempt++) {
      try {
        return call.call();
      } catch (GlossaryNotFoundException e) {
        if (attempt >= 9) {
          throw e;
        }
        Thread.sleep(1000);
      }
    }
  }
}
