// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HttpClientTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String USAGE = "{\"character_count\":1,\"character_limit\":2}";
  private static final String GLOSSARY =
      "{\"glossary_id\":\"g1\",\"name\":\"n\",\"creation_time\":\"2021-08-03T14:16:18.329Z\","
          + "\"dictionaries\":[]}";

  private DeepLClient client(int maxRetries) {
    return OfflineClient.createClient(
        wm, (DeepLClientOptions) new DeepLClientOptions().setMaxRetries(maxRetries));
  }

  private void failThenSucceed(String scenario, String path, int status, String body) {
    wm.stubFor(
        get(path)
            .inScenario(scenario)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("recovered")
            .willReturn(aResponse().withStatus(status)));
    wm.stubFor(
        get(path).inScenario(scenario).whenScenarioStateIs("recovered").willReturn(okJson(body)));
  }

  @Test
  void retriesAfterTooManyRequests() throws Exception {
    failThenSucceed("429", "/v2/usage", 429, USAGE);
    client(5).getUsage();
    wm.verify(2, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void retriesAfterAServerError() throws Exception {
    failThenSucceed("503", "/v2/usage", 503, USAGE);
    client(5).getUsage();
    wm.verify(2, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void doesNotRetryAfterAClientError() {
    wm.stubFor(get("/v2/usage").willReturn(aResponse().withStatus(400)));
    assertThrows(DeepLException.class, () -> client(5).getUsage());
    wm.verify(1, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void throwsTooManyRequestsWithoutRetryingWhenMaxRetriesIsZero() {
    wm.stubFor(get("/v2/usage").willReturn(aResponse().withStatus(429)));
    assertThrows(TooManyRequestsException.class, () -> client(0).getUsage());
    wm.verify(1, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void stopsRetryingAfterMaxRetries() {
    wm.stubFor(get("/v2/usage").willReturn(aResponse().withStatus(429)));
    assertThrows(TooManyRequestsException.class, () -> client(1).getUsage());
    wm.verify(2, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void throwsConnectionExceptionWhenTheServerDoesNotAnswerInTime() {
    wm.stubFor(get("/v2/usage").willReturn(okJson(USAGE).withFixedDelay(2500)));
    DeepLClient timeoutClient =
        OfflineClient.createClient(
            wm,
            (DeepLClientOptions)
                new DeepLClientOptions().setMaxRetries(0).setTimeout(Duration.ofMillis(200)));
    assertThrows(ConnectionException.class, timeoutClient::getUsage);
  }

  @Test
  void retriesAfterATimeout() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .inScenario("slow")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("fast")
            .willReturn(okJson(USAGE).withFixedDelay(2500)));
    wm.stubFor(
        get("/v2/usage").inScenario("slow").whenScenarioStateIs("fast").willReturn(okJson(USAGE)));
    DeepLClient retryingClient =
        OfflineClient.createClient(
            wm,
            (DeepLClientOptions)
                new DeepLClientOptions().setMaxRetries(1).setTimeout(Duration.ofMillis(200)));
    retryingClient.getUsage();
    wm.verify(2, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void throwsConnectionExceptionWhenTheConnectionIsReset() {
    wm.stubFor(get("/v2/usage").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    assertThrows(ConnectionException.class, () -> client(0).getUsage());
  }

  // A PATCH request, because HttpURLConnection silently resends a GET once after a reset.
  @Test
  void retriesAConnectionReset() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .inScenario("reset")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("recovered")
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .inScenario("reset")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(GLOSSARY)));
    client(1).updateMultilingualGlossaryName("g1", "n");
    wm.verify(2, patchRequestedFor(urlEqualTo("/v3/glossaries/g1")));
  }

  // WireMock reads the whole request before failing, so a socket that resets every connection
  // stands in for a server that drops the connection while the request body is still being sent.
  @Test
  void sendsAPatchRequestOnlyOnceWhenMaxRetriesIsZero() throws Exception {
    AtomicInteger connections = new AtomicInteger();
    try (ServerSocket server = new ServerSocket(0)) {
      Thread acceptor =
          new Thread(
              () -> {
                try {
                  while (true) {
                    Socket socket = server.accept();
                    connections.incrementAndGet();
                    socket.setSoLinger(true, 0);
                    socket.close();
                  }
                } catch (Exception closed) {
                  // the server socket was closed at the end of the test
                }
              });
      acceptor.start();
      DeepLClientOptions options =
          new DeepLClientOptions().setServerUrl("http://127.0.0.1:" + server.getLocalPort());
      DeepLClient resetClient = new DeepLClient("k", options.setMaxRetries(0));
      StringBuilder longName = new StringBuilder();
      for (int i = 0; i < 1 << 23; i++) {
        longName.append('n');
      }
      ConnectionException thrown =
          assertThrows(
              ConnectionException.class,
              () -> resetClient.updateMultilingualGlossaryName("g1", longName.toString()));
      assertTrue(thrown.getCause() instanceof SocketException, thrown.toString());
    }
    assertEquals(1, connections.get());
  }

  // A request to any host but the local server goes to the refusing proxy that the offline
  // suite configures through the JVM settings. The .invalid host never resolves, so a request
  // that ignored the proxy would fail with another message.
  private static DeepLClient externalClient() {
    DeepLClientOptions options = new DeepLClientOptions();
    options.setServerUrl("https://example.invalid").setMaxRetries(0);
    return new DeepLClient("k", options);
  }

  @Test
  void sendsRequestsThroughTheProxyOfTheJvmSettings() {
    ConnectionException thrown =
        assertThrows(ConnectionException.class, () -> externalClient().getUsage());
    assertTrue(thrown.getMessage().contains("Connection refused"), thrown.getMessage());
    assertTrue(thrown.getShouldRetry(), "a refused connection is retried");
    assertNull(thrown.getHttpStatusCode());
  }

  @Test
  void sendsPatchRequestsThroughTheProxyOfTheJvmSettings() {
    ConnectionException thrown =
        assertThrows(
            ConnectionException.class,
            () -> externalClient().updateMultilingualGlossaryName("g1", "n"));
    assertTrue(thrown.getMessage().contains("Connection refused"), thrown.getMessage());
    assertTrue(thrown.getShouldRetry(), "a refused connection is retried");
  }

  @Test
  void retriesAfterAFailedDnsLookup() {
    DeepLClientOptions options = new DeepLClientOptions();
    options.setServerUrl("https://example.invalid").setProxy(Proxy.NO_PROXY).setMaxRetries(0);
    ConnectionException thrown =
        assertThrows(ConnectionException.class, () -> new DeepLClient("k", options).getUsage());
    assertTrue(thrown.getCause() instanceof UnknownHostException, thrown.toString());
    assertTrue(thrown.getShouldRetry(), "a failed DNS lookup is retried");
  }

  @Test
  void retriesATimeoutWhileReadingTheResponseBody() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .inScenario("slow body")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("fast")
            .willReturn(okJson(USAGE).withChunkedDribbleDelay(2, 4000)));
    wm.stubFor(
        get("/v2/usage")
            .inScenario("slow body")
            .whenScenarioStateIs("fast")
            .willReturn(okJson(USAGE)));
    DeepLClient retryingClient =
        OfflineClient.createClient(
            wm,
            (DeepLClientOptions)
                new DeepLClientOptions().setMaxRetries(1).setTimeout(Duration.ofMillis(300)));
    retryingClient.getUsage();
    wm.verify(2, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void throwsConnectionExceptionWhenTheResponseBodyIsCutOff() {
    wm.stubFor(get("/v2/usage").willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(ConnectionException.class, () -> client(0).getUsage());
  }

  @Test
  void throwsConnectionExceptionWhenThePatchResponseBodyIsCutOff() {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(
        ConnectionException.class, () -> client(0).updateMultilingualGlossaryName("g1", "n"));
  }

  @Test
  void retriesPatchRequestsAfterTooManyRequests() throws Exception {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .inScenario("patch")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("recovered")
            .willReturn(aResponse().withStatus(429)));
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .inScenario("patch")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(GLOSSARY)));
    client(5).updateMultilingualGlossaryName("g1", "n");
    wm.verify(2, patchRequestedFor(urlEqualTo("/v3/glossaries/g1")));
  }

  @Test
  void stopsRetryingPatchRequestsAfterMaxRetries() {
    wm.stubFor(patch(urlEqualTo("/v3/glossaries/g1")).willReturn(aResponse().withStatus(503)));
    DeepLException thrown =
        assertThrows(
            DeepLException.class, () -> client(1).updateMultilingualGlossaryName("g1", "n"));
    assertEquals(true, thrown.getMessage().startsWith("Service unavailable"));
    wm.verify(2, patchRequestedFor(urlEqualTo("/v3/glossaries/g1")));
  }

  @Test
  void throwsConnectionExceptionWhenAPatchRequestDoesNotAnswerInTime() {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1")).willReturn(okJson(GLOSSARY).withFixedDelay(2500)));
    DeepLClient timeoutClient =
        OfflineClient.createClient(
            wm,
            (DeepLClientOptions)
                new DeepLClientOptions().setMaxRetries(0).setTimeout(Duration.ofMillis(200)));
    assertThrows(
        ConnectionException.class, () -> timeoutClient.updateMultilingualGlossaryName("g1", "n"));
  }
}
