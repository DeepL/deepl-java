// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

/** Parses proxy URLs given to the client options. */
final class ProxyUrl {
  private ProxyUrl() {}

  static Proxy parse(String proxyUrl) {
    if (proxyUrl == null) {
      throw new IllegalArgumentException("proxyUrl cannot be null");
    }
    URI uri;
    try {
      uri = new URI(proxyUrl.trim());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid proxy URL", e);
    }
    if (!"http".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException("Proxy URL must use the http scheme");
    }
    if (uri.getHost() == null) {
      throw new IllegalArgumentException("Proxy URL has no host");
    }
    // HttpURLConnection cannot send proxy credentials without a global Authenticator.
    if (uri.getUserInfo() != null) {
      throw new IllegalArgumentException("Proxy URL credentials are not supported");
    }
    int port = uri.getPort() == -1 ? 80 : uri.getPort();
    String host = uri.getHost().replaceAll("^\\[|\\]$", ""); // IPv6 literals come in brackets
    return new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
  }
}
