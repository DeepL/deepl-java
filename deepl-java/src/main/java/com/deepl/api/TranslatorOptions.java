// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.net.Proxy;
import java.time.Duration;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options to control client behaviour, shared by {@link DeepLClientOptions}. Create a {@link
 * DeepLClientOptions} and pass it to the {@link DeepLClient} constructor.
 */
public class TranslatorOptions {
  private int maxRetries = 5;
  private Duration timeout = Duration.ofSeconds(10);
  @Nullable private Proxy proxy = null;
  @Nullable private Map<String, String> headers = null;
  @Nullable private String serverUrl = null;
  private boolean sendPlatformInfo = true;
  @Nullable private AppInfo appInfo = null;

  protected TranslatorOptions() {}

  /**
   * Set the maximum number of failed attempts that the client will retry, per request. By default,
   * 5 retries are made. Note: only errors due to transient conditions are retried.
   */
  public TranslatorOptions setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }

  /** Set the connection timeout used for each HTTP request retry, the default is 10 seconds. */
  public TranslatorOptions setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Set the proxy to use for HTTP requests, as any {@link Proxy}, HTTP or SOCKS. {@link
   * Proxy#NO_PROXY} connects directly, ignoring the JVM proxy settings. By default, this value is
   * <code>null</code> and the JVM proxy settings apply.
   */
  public TranslatorOptions setProxy(Proxy proxy) {
    this.proxy = proxy;
    return this;
  }

  /**
   * Set the HTTP proxy to use for HTTP requests, as a URL such as {@code
   * http://proxy.example.com:3128}. The port defaults to 80. Only <code>http</code> proxy URLs
   * without credentials are supported; use {@link #setProxy(Proxy)} for other proxies. <code>null
   * </code> clears the proxy, so the JVM proxy settings apply.
   *
   * @throws IllegalArgumentException If the URL is not a valid HTTP proxy URL.
   */
  public TranslatorOptions setProxyUrl(@Nullable String proxyUrl) {
    this.proxy = proxyUrl == null ? null : ProxyUrl.parse(proxyUrl);
    return this;
  }

  /**
   * Set HTTP headers attached to every HTTP request. By default, this value is <code>null</code>
   * and no extra headers are used. An <code>Authorization</code> or <code>User-Agent</code> header,
   * in any casing, replaces the one built by the client. The headers <code>Content-Type</code>,
   * <code>Content-Length</code>, <code>Transfer-Encoding</code> and <code>Host</code> keep the
   * library's values.
   */
  public TranslatorOptions setHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  /**
   * Set the base URL for DeepL API that may be overridden for testing purposes. By default, this
   * value is <code>null</code> and the correct DeepL API base URL is selected based on the API
   * account type (free or paid).
   *
   * <p>The URL must be an absolute <code>http</code> or <code>https</code> URL with a host; a port
   * and a path prefix are allowed. It is checked when the client is created: the client constructor
   * throws {@link IllegalArgumentException} for an invalid URL. A trailing <code>/</code> is
   * ignored.
   */
  public TranslatorOptions setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
    return this;
  }

  /**
   * Set whether to send basic platform information with each API call to improve DeepL products.
   * Defaults to `true`, set to `false` to opt out. This option will be overriden if a `User-Agent`
   * header (in any casing) is present in this objects `headers`.
   */
  public TranslatorOptions setSendPlatformInfo(boolean sendPlatformInfo) {
    this.sendPlatformInfo = sendPlatformInfo;
    return this;
  }

  /**
   * Set an identifier and a version for the program/plugin that uses this Client Library. Example:
   * `DeepLClient client = new DeepLClient(myAuthKey, new DeepLClientOptions()
   * .setAppInfo("deepl-hadoop-plugin", "1.2.0"))`
   */
  public TranslatorOptions setAppInfo(String appName, String appVersion) {
    this.appInfo = new AppInfo(appName, appVersion);
    return this;
  }

  /** Gets the current maximum number of retries. */
  public int getMaxRetries() {
    return maxRetries;
  }

  /** Gets the current maximum request timeout. */
  public Duration getTimeout() {
    return timeout;
  }

  /** Gets the current proxy. */
  public @Nullable Proxy getProxy() {
    return proxy;
  }

  /** Gets the current HTTP headers. */
  public @Nullable Map<String, String> getHeaders() {
    return headers;
  }

  /** Gets the current custom server URL. */
  public @Nullable String getServerUrl() {
    return serverUrl;
  }

  /** Gets the `sendPlatformInfo` option */
  public boolean getSendPlatformInfo() {
    return sendPlatformInfo;
  }

  /** Gets the `appInfo` identifiers */
  public @Nullable AppInfo getAppInfo() {
    return appInfo;
  }
}
