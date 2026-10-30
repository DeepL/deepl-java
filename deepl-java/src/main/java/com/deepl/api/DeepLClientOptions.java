// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.net.Proxy;
import java.time.Duration;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

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

  /**
   * Set the version of the DeepL API to use. By default, this value is <code>
   * DeepLApiVersion.VERSION_2</code> and the most recent DeepL API version is used. Note that older
   * API versions like <code>DeepLApiVersion.VERSION_1</code> might not support all features of the
   * more modern API (eg. document translation is v2-only), and that not all API subscriptions have
   * access to one or the other API version. If in doubt, always use the most recent API version you
   * have access to.
   *
   * @deprecated The API version is not a client option of the other DeepL client libraries and will
   *     be removed in the next major version; the default, version 2, is always used then.
   */
  @Deprecated
  public DeepLClientOptions setApiVersion(DeepLApiVersion apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  /**
   * Gets the current API version.
   *
   * @deprecated See {@link #setApiVersion(DeepLApiVersion)}.
   */
  @Deprecated
  public @Nullable DeepLApiVersion getApiVersion() {
    return apiVersion;
  }
}
