// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("deprecation")
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
    new Translator(" abc-123 \n", new TranslatorOptions().setServerUrl(wm.baseUrl())).getUsage();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Authorization", "authorization"})
  void sendsTheKeyEvenWhenTheHeadersContainAnAuthorizationHeader(String name) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(name, "Bearer token");
    wm.stubFor(
        get("/v2/usage")
            .withHeader("Authorization", equalTo("DeepL-Auth-Key k"))
            .withHeader("Authorization", notMatching(".*Bearer.*"))
            .willReturn(okJson(USAGE)));
    OfflineClient.createClient(
            wm, (DeepLClientOptions) new DeepLClientOptions().setHeaders(headers))
        .getUsage();
  }

  @Test
  void sendsTheAdminKeyEvenWhenTheHeadersContainAnAuthorizationHeader() throws Exception {
    wm.stubFor(
        get("/v2/admin/developer-keys")
            .withHeader("Authorization", equalTo("DeepL-Auth-Key admin-key"))
            .willReturn(okJson("[]")));
    OfflineClient.createAdminClient(
            wm,
            new DeepLAdminClientOptions()
                .setHeaders(Collections.singletonMap("Authorization", "Bearer token")))
        .listDeveloperKeys();
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
    OfflineClient.createTranslator(
            wm, new TranslatorOptions().setHeaders(Collections.singletonMap("X-Custom", "value")))
        .getUsage();
  }

  @Test
  void sendsToTheServerUrlIncludingItsPath() throws Exception {
    wm.stubFor(get("/prefix/v2/usage").willReturn(okJson(USAGE)));
    new Translator("k", new TranslatorOptions().setServerUrl(wm.baseUrl() + "/prefix")).getUsage();
  }

  @ParameterizedTest
  @MethodSource("userAgents")
  void sendsTheUserAgent(TranslatorOptions options, String expectedPattern) throws Exception {
    expectUsage("User-Agent", matching(expectedPattern));
    OfflineClient.createTranslator(wm, options).getUsage();
  }

  private static Stream<Arguments> userAgents() {
    Map<String, String> custom = Collections.singletonMap("User-Agent", "my custom user agent");
    String platform = "deepl-java/[0-9.]+ \\(.+\\) java/\\S+";
    return Stream.of(
        Arguments.of(new TranslatorOptions(), platform),
        Arguments.of(new TranslatorOptions().setSendPlatformInfo(false), "deepl-java/[0-9.]+"),
        Arguments.of(new TranslatorOptions().setHeaders(custom), "my custom user agent"),
        Arguments.of(
            new TranslatorOptions().setAppInfo("my-java-translation-plugin", "1.2.3"),
            platform + " " + APP),
        Arguments.of(
            new TranslatorOptions()
                .setSendPlatformInfo(false)
                .setAppInfo("my-java-translation-plugin", "1.2.3"),
            "deepl-java/[0-9.]+ " + APP),
        Arguments.of(
            new TranslatorOptions()
                .setHeaders(custom)
                .setAppInfo("my-java-translation-plugin", "1.2.3"),
            "my custom user agent"));
  }

  @Test
  void throwsIllegalArgumentWhenTheAuthKeyIsEmptyOrNull() {
    assertThrows(IllegalArgumentException.class, () -> new Translator(""));
    assertThrows(IllegalArgumentException.class, () -> new Translator(null));
    assertThrows(IllegalArgumentException.class, () -> new DeepLClient(""));
    assertThrows(IllegalArgumentException.class, () -> new DeepLClient(" \n"));
  }

  @Test
  void throwsConnectionExceptionWhenTheServerUrlIsInvalid() {
    Translator translator =
        new Translator(
            "k", new TranslatorOptions().setServerUrl("http:/api.deepl.com").setMaxRetries(0));
    assertThrows(ConnectionException.class, translator::getUsage);
  }

  @Test
  void recognizesAuthKeysOfFreeAccounts() {
    assertTrue(Translator.isFreeAccountAuthKey("b493b8ef-0176-215d-82fe-e28f182c9544:fx"));
    assertFalse(Translator.isFreeAccountAuthKey("b493b8ef-0176-215d-82fe-e28f182c9544"));
    assertFalse(Translator.isFreeAccountAuthKey(null));
  }
}
