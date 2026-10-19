// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/** Builds clients that talk to the local {@link OfflineServer}. */
@SuppressWarnings("deprecation")
final class OfflineClient {
  static final String AUTH_KEY = "k";

  private OfflineClient() {}

  static DeepLClient createClient(OfflineServer server) {
    return createClient(server, new DeepLClientOptions());
  }

  static DeepLClient createClient(OfflineServer server, DeepLClientOptions options) {
    options.setServerUrl(server.baseUrl());
    return new DeepLClient(AUTH_KEY, options);
  }

  static Translator createTranslator(OfflineServer server) {
    return createTranslator(server, new TranslatorOptions());
  }

  static Translator createTranslator(OfflineServer server, TranslatorOptions options) {
    options.setServerUrl(server.baseUrl());
    return new Translator(AUTH_KEY, options);
  }
}
