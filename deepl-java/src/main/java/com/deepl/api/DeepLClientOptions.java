// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.net.Proxy;
import java.time.Duration;
import java.util.Map;

/** {@inheritDoc} */
@SuppressWarnings("deprecation")
public class DeepLClientOptions extends TranslatorOptions {
  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setMaxRetries(int maxRetries) {
    super.setMaxRetries(maxRetries);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setTimeout(Duration timeout) {
    super.setTimeout(timeout);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setProxy(Proxy proxy) {
    super.setProxy(proxy);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setProxyUrl(String proxyUrl) {
    super.setProxyUrl(proxyUrl);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setHeaders(Map<String, String> headers) {
    super.setHeaders(headers);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setServerUrl(String serverUrl) {
    super.setServerUrl(serverUrl);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setSendPlatformInfo(boolean sendPlatformInfo) {
    super.setSendPlatformInfo(sendPlatformInfo);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DeepLClientOptions setAppInfo(String appName, String appVersion) {
    super.setAppInfo(appName, appVersion);
    return this;
  }
}
