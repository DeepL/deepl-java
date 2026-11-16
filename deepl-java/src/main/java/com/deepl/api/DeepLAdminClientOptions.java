// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.net.Proxy;
import java.time.Duration;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Options to control {@link DeepLAdminClient} behaviour. These options may be provided to the
 * {@link DeepLAdminClient} constructor.
 *
 * <p>All properties have corresponding setters in fluent-style, so the following is possible:
 * <code>
 *      DeepLAdminClientOptions options = new DeepLAdminClientOptions()
 *          .setTimeout(Duration.ofSeconds(1)).setMaxRetries(2);
 * </code>
 */
public class DeepLAdminClientOptions {
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  private int maxRetries = 5;
  private Duration timeout = DEFAULT_TIMEOUT;
  @Nullable private Proxy proxy = null;
  @Nullable private Map<String, String> headers = null;
  @Nullable private String serverUrl = null;
  private boolean sendPlatformInfo = true;
  @Nullable private AppInfo appInfo = null;

  /**
   * Set the maximum number of failed attempts that {@link DeepLAdminClient} will retry, per
   * request. By default, 5 retries are made. Note: only errors due to transient conditions are
   * retried. Developer key creation is retried only after HTTP 429 or a failure while connecting
   * (for example a DNS resolution failure, a refused connection, an unreachable host or network, or
   * a connect timeout), but not after a TLS or proxy failure, as the API cannot have processed the
   * request then.
   */
  public DeepLAdminClientOptions setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }

  /**
   * Set the connection timeout used for each HTTP request retry, the default is 10 seconds; <code>
   * null</code> restores the default.
   */
  public DeepLAdminClientOptions setTimeout(@Nullable Duration timeout) {
    this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
    return this;
  }

  /**
   * Set the proxy to use for HTTP requests, as any {@link Proxy}, HTTP or SOCKS. {@link
   * Proxy#NO_PROXY} connects directly, ignoring the JVM proxy settings. By default, this value is
   * <code>null</code> and the JVM proxy settings apply.
   */
  public DeepLAdminClientOptions setProxy(Proxy proxy) {
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
  public DeepLAdminClientOptions setProxyUrl(@Nullable String proxyUrl) {
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
  public DeepLAdminClientOptions setHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  /**
   * Set the base URL for the DeepL Admin API that may be overridden for testing purposes. By
   * default, this value is <code>null</code> and <code>https://api.deepl.com</code>, the only
   * server offering the Admin API, is used.
   *
   * <p>The URL must be an absolute <code>http</code> or <code>https</code> URL with a host; a port
   * and a path prefix are allowed. It is checked when the client is created: the client constructor
   * throws {@link IllegalArgumentException} for an invalid URL. A trailing <code>/</code> is
   * ignored.
   */
  public DeepLAdminClientOptions setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
    return this;
  }

  /**
   * Set whether to send basic platform information with each API call to improve DeepL products.
   * Defaults to `true`, set to `false` to opt out. This option is ignored if a `User-Agent` header
   * is set with {@link #setHeaders(Map)}.
   */
  public DeepLAdminClientOptions setSendPlatformInfo(boolean sendPlatformInfo) {
    this.sendPlatformInfo = sendPlatformInfo;
    return this;
  }

  /** Set an identifier and a version for the program/plugin that uses this Client Library. */
  public DeepLAdminClientOptions setAppInfo(String appName, String appVersion) {
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
