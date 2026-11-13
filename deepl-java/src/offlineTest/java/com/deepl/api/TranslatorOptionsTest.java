// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TranslatorOptionsTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String USAGE = "{\"character_count\":1,\"character_limit\":2}";
  private static final String APP = "my-java-translation-plugin/1.2.3";

  private void expectUsage(String headerName, StringValuePattern value) {
    wm.stubFor(get("/v2/usage").withHeader(headerName, value).willReturn(okJson(USAGE)));
  }

  @Test
  void sendsTheAuthKeyWithoutSurroundingWhitespaceInTheAuthorizationHeader() throws Exception {
    expectUsage("Authorization", equalTo("DeepL-Auth-Key abc-123"));
    new DeepLClient(" abc-123 \n", new DeepLClientOptions().setServerUrl(wm.baseUrl())).getUsage();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Authorization", "authorization"})
  void sendsTheAuthorizationHeaderOfTheHeadersInPlaceOfTheKey(String name) throws Exception {
    wm.stubFor(get("/v2/usage").willReturn(okJson(USAGE)));
    OfflineClient.createClient(
            wm,
            (DeepLClientOptions)
                new DeepLClientOptions().setHeaders(Collections.singletonMap(name, "Bearer token")))
        .getUsage();
    assertEquals(Collections.singletonList("Bearer token"), recordedAuthorizationValues());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Authorization", "authorization"})
  void sendsTheAuthorizationHeaderOfTheHeadersInPlaceOfTheAdminKey(String name) throws Exception {
    wm.stubFor(get("/v2/admin/developer-keys").willReturn(okJson("[]")));
    OfflineClient.createAdminClient(
            wm,
            new DeepLAdminClientOptions()
                .setHeaders(Collections.singletonMap(name, "Bearer token")))
        .listDeveloperKeys();
    assertEquals(Collections.singletonList("Bearer token"), recordedAuthorizationValues());
  }

  private static List<String> recordedAuthorizationValues() {
    List<ServeEvent> events = wm.getAllServeEvents();
    assertEquals(1, events.size());
    HttpHeader header = events.get(0).getRequest().getHeaders().getHeader("Authorization");
    return header.isPresent() ? header.values() : Collections.emptyList();
  }

  @Test
  void sendsRequestsThroughTheProxyOfAProxyUrl() throws Exception {
    // WireMock answers plain HTTP proxy requests, so it stands in for both proxy and server.
    wm.stubFor(
        get("/v2/usage").withHeader("Host", equalTo("example.invalid")).willReturn(okJson(USAGE)));
    DeepLClientOptions options = new DeepLClientOptions();
    options.setServerUrl("http://example.invalid").setProxyUrl("http://localhost:" + wm.getPort());
    new DeepLClient("k", options).getUsage();
  }

  @Test
  void sendsAdminRequestsThroughTheProxyOfAProxyUrl() throws Exception {
    wm.stubFor(
        get("/v2/admin/developer-keys")
            .withHeader("Host", equalTo("example.invalid"))
            .willReturn(okJson("[]")));
    new DeepLAdminClient(
            "k",
            new DeepLAdminClientOptions()
                .setServerUrl("http://example.invalid")
                .setProxyUrl("http://127.0.0.1:" + wm.getPort()))
        .listDeveloperKeys();
  }

  @ParameterizedTest
  @ValueSource(strings = {"socks5://proxy:1080", "proxy:3128", "http://", "http://u:p@proxy:3128"})
  void rejectsProxyUrlsThatAreNotPlainHttpProxies(String url) {
    assertThrows(IllegalArgumentException.class, () -> new DeepLClientOptions().setProxyUrl(url));
    assertThrows(
        IllegalArgumentException.class, () -> new DeepLAdminClientOptions().setProxyUrl(url));
  }

  @Test
  void sendsCustomHeaders() throws Exception {
    expectUsage("X-Custom", equalTo("value"));
    OfflineClient.createClient(
            wm, new DeepLClientOptions().setHeaders(Collections.singletonMap("X-Custom", "value")))
        .getUsage();
  }

  @Test
  void sendsToTheServerUrlIncludingItsPath() throws Exception {
    wm.stubFor(get("/prefix/v2/usage").willReturn(okJson(USAGE)));
    new DeepLClient("k", new DeepLClientOptions().setServerUrl(wm.baseUrl() + "/prefix"))
        .getUsage();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/", "/prefix/", "/prefix//"})
  void sendsToTheServerUrlWithoutItsTrailingSlash(String suffix) throws Exception {
    String prefix = suffix.replaceAll("/+$", "");
    wm.stubFor(get(prefix + "/v2/usage").willReturn(okJson(USAGE)));
    Usage usage =
        new DeepLClient("k", new DeepLClientOptions().setServerUrl(wm.baseUrl() + suffix))
            .getUsage();
    assertEquals(1, usage.getCharacter().getCount());
  }

  @ParameterizedTest
  @MethodSource("userAgents")
  void sendsTheUserAgent(DeepLClientOptions options, String expectedPattern) throws Exception {
    expectUsage("User-Agent", matching(expectedPattern));
    OfflineClient.createClient(wm, options).getUsage();
  }

  private static Stream<Arguments> userAgents() {
    Map<String, String> custom = Collections.singletonMap("User-Agent", "my custom user agent");
    String platform = "deepl-java/[0-9.]+ \\(.+\\) java/\\S+";
    return Stream.of(
        Arguments.of(new DeepLClientOptions(), platform),
        Arguments.of(new DeepLClientOptions().setSendPlatformInfo(false), "deepl-java/[0-9.]+"),
        Arguments.of(new DeepLClientOptions().setHeaders(custom), "my custom user agent"),
        Arguments.of(
            new DeepLClientOptions().setAppInfo("my-java-translation-plugin", "1.2.3"),
            platform + " " + APP),
        Arguments.of(
            new DeepLClientOptions()
                .setSendPlatformInfo(false)
                .setAppInfo("my-java-translation-plugin", "1.2.3"),
            "deepl-java/[0-9.]+ " + APP),
        Arguments.of(
            new DeepLClientOptions()
                .setHeaders(custom)
                .setAppInfo("my-java-translation-plugin", "1.2.3"),
            "my custom user agent"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"USER-AGENT", "user-agent", "User-agent"})
  void replacesTheUserAgentWithAUserAgentHeaderInAnyCasing(String name) throws Exception {
    expectUsage("User-Agent", equalTo("my custom user agent"));
    OfflineClient.createClient(
            wm,
            new DeepLClientOptions()
                .setHeaders(Collections.singletonMap(name, "my custom user agent")))
        .getUsage();
  }

  @Test
  void throwsIllegalArgumentWhenTheAuthKeyIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> new DeepLClient(""));
    assertThrows(IllegalArgumentException.class, () -> new DeepLClient(" \n"));
  }

  @Test
  void throwsNullPointerExceptionNamingANullKey() {
    assertEquals(
        "authKey",
        assertThrows(NullPointerException.class, () -> new DeepLClient(null)).getMessage());
    assertEquals(
        "adminKey",
        assertThrows(NullPointerException.class, () -> new DeepLAdminClient(null)).getMessage());
  }

  @Test
  void usesTheDefaultOptionsWhenTheOptionsAreNull() {
    assertDoesNotThrow(() -> new DeepLClient("k", null));
    assertDoesNotThrow(() -> new DeepLAdminClient("k", null));
  }

  @Test
  void clearsTheProxyWhenTheProxyUrlIsNull() {
    assertNull(
        new DeepLClientOptions().setProxyUrl("http://proxy:3128").setProxyUrl(null).getProxy());
    assertNull(
        new DeepLAdminClientOptions()
            .setProxyUrl("http://proxy:3128")
            .setProxyUrl(null)
            .getProxy());
  }

  @Test
  void acceptsAServerUrlWithAnUnderscoreInTheHostName() {
    assertDoesNotThrow(
        () ->
            new DeepLClient("k", new DeepLClientOptions().setServerUrl("http://deepl_mock:3000")));
    assertDoesNotThrow(
        () ->
            new DeepLAdminClient(
                "k", new DeepLAdminClientOptions().setServerUrl("http://deepl_mock:3000")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"http:/api.deepl.com", "api.deepl.com", "ftp://api.deepl.com", "http://"})
  void throwsIllegalArgumentWhenTheServerUrlIsNotAnAbsoluteHttpUrl(String url) {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeepLClient("k", new DeepLClientOptions().setServerUrl(url)));
    assertFalse(thrown.getMessage().contains(url), thrown.getMessage());
    thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeepLAdminClient("k", new DeepLAdminClientOptions().setServerUrl(url)));
    assertFalse(thrown.getMessage().contains(url), thrown.getMessage());
  }

  @Test
  void recognizesAuthKeysOfFreeAccounts() {
    assertTrue(DeepLClient.isFreeAccountAuthKey("b493b8ef-0176-215d-82fe-e28f182c9544:fx"));
    assertFalse(DeepLClient.isFreeAccountAuthKey("b493b8ef-0176-215d-82fe-e28f182c9544"));
    assertFalse(DeepLClient.isFreeAccountAuthKey(null));
  }
}
