// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
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

  private interface ClientCall {
    void run(DeepLClient client) throws Exception;
  }

  private static final MultilingualGlossaryDictionaryEntries DICTIONARY =
      new MultilingualGlossaryDictionaryEntries(
          "en", "de", new GlossaryEntries(Collections.singletonMap("a", "b")));
  private static final ClientCall CREATE_GLOSSARY =
      c -> c.createMultilingualGlossary("n", Collections.singletonList(DICTIONARY));
  private static final ClientCall CREATE_EXPORT = c -> c.createTranslationMemoryExport("tm1");

  /** Fails the TLS step of every connection with the given error, after the TCP connect. */
  private static SSLSocketFactory failingTls(IOException failure) {
    return new SSLSocketFactory() {
      @Override
      public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
          throws IOException {
        socket.close();
        throw failure;
      }

      @Override
      public Socket createSocket(String host, int port) throws IOException {
        throw failure;
      }

      @Override
      public Socket createSocket(String host, int port, InetAddress local, int localPort)
          throws IOException {
        throw failure;
      }

      @Override
      public Socket createSocket(InetAddress host, int port) throws IOException {
        throw failure;
      }

      @Override
      public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort)
          throws IOException {
        throw failure;
      }

      @Override
      public String[] getDefaultCipherSuites() {
        return new String[0];
      }

      @Override
      public String[] getSupportedCipherSuites() {
        return new String[0];
      }
    };
  }

  /** A TLS context with the given factory: Apache HttpClient reads the JVM default context. */
  private static SSLContext contextOf(SSLSocketFactory factory) {
    return new SSLContext(
        new SSLContextSpi() {
          @Override
          protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom random) {}

          @Override
          protected SSLSocketFactory engineGetSocketFactory() {
            return factory;
          }

          @Override
          protected SSLServerSocketFactory engineGetServerSocketFactory() {
            throw new UnsupportedOperationException();
          }

          @Override
          protected SSLEngine engineCreateSSLEngine() {
            throw new UnsupportedOperationException();
          }

          @Override
          protected SSLEngine engineCreateSSLEngine(String host, int port) {
            throw new UnsupportedOperationException();
          }

          @Override
          protected SSLSessionContext engineGetServerSessionContext() {
            throw new UnsupportedOperationException();
          }

          @Override
          protected SSLSessionContext engineGetClientSessionContext() {
            throw new UnsupportedOperationException();
          }
        },
        null,
        "TLS") {};
  }

  /**
   * Runs the call with maxRetries 1 against a local listener that accepts every TCP connection
   * while the TLS step fails with the given error; counts the accepted connections.
   */
  private static ConnectionException failWhileConnecting(
      IOException failure, int expectedConnections, AtomicInteger connections, ClientCall call)
      throws Exception {
    SSLSocketFactory previousFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
    SSLContext previousContext = SSLContext.getDefault();
    SSLSocketFactory failing = failingTls(failure);
    try (ServerSocket server = new ServerSocket(0)) {
      Thread acceptor =
          new Thread(
              () -> {
                try {
                  while (true) {
                    server.accept().close();
                    connections.incrementAndGet();
                  }
                } catch (IOException closed) {
                  // the server socket was closed at the end of the test
                }
              });
      acceptor.start();
      HttpsURLConnection.setDefaultSSLSocketFactory(failing);
      SSLContext.setDefault(contextOf(failing));
      DeepLClientOptions options = new DeepLClientOptions();
      options
          .setServerUrl("https://127.0.0.1:" + server.getLocalPort())
          .setProxy(Proxy.NO_PROXY)
          .setMaxRetries(1);
      DeepLClient failingClient = new DeepLClient("k", options);
      ConnectionException thrown =
          assertThrows(ConnectionException.class, () -> call.run(failingClient));
      // The listener may count the last connection after the client gave up.
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (connections.get() < expectedConnections && System.nanoTime() < deadline) {
        Thread.sleep(10);
      }
      return thrown;
    } finally {
      HttpsURLConnection.setDefaultSSLSocketFactory(previousFactory);
      SSLContext.setDefault(previousContext);
    }
  }

  /**
   * Runs the call through a local HTTP proxy that refuses every CONNECT with the given status and
   * counts its connections. Apache returns the proxy's answer as the response; the client turns it
   * into a ConnectionException.
   */
  private static DeepLException failThroughRefusingProxy(
      ClientCall call, int status, AtomicInteger connections) throws Exception {
    try (ServerSocket proxy = new ServerSocket(0)) {
      Thread responder =
          new Thread(
              () -> {
                try {
                  while (true) {
                    try (Socket socket = proxy.accept()) {
                      connections.incrementAndGet();
                      BufferedReader request =
                          new BufferedReader(
                              new InputStreamReader(
                                  socket.getInputStream(), StandardCharsets.US_ASCII));
                      String line;
                      do {
                        line = request.readLine();
                      } while (line != null && !line.isEmpty());
                      socket
                          .getOutputStream()
                          .write(
                              ("HTTP/1.1 " + status + " Refused\r\nContent-Length: 0\r\n\r\n")
                                  .getBytes(StandardCharsets.US_ASCII));
                    }
                  }
                } catch (IOException closed) {
                  // the server socket was closed at the end of the test
                }
              });
      responder.start();
      DeepLClientOptions options = new DeepLClientOptions();
      options
          .setServerUrl("https://example.invalid")
          .setProxy(
              new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxy.getLocalPort())))
          .setMaxRetries(1);
      DeepLClient proxied = new DeepLClient("k", options);
      return assertThrows(DeepLException.class, () -> call.run(proxied));
    }
  }

  // An error that no exception-class check knows, raised after the TCP connection was made and
  // before any request byte is written: the request was not sent.
  @Test
  void retriesAFailureWhileConnectingForCreatesAndOtherRequests() throws Exception {
    for (ClientCall call : Arrays.asList(CREATE_GLOSSARY, CREATE_EXPORT, DeepLClient::getUsage)) {
      SocketException unreachable = new SocketException("Network is unreachable");
      AtomicInteger connections = new AtomicInteger();
      ConnectionException thrown = failWhileConnecting(unreachable, 2, connections, call);
      assertSame(unreachable, thrown.getCause());
      assertTrue(thrown.getShouldRetry());
      assertEquals(2, connections.get(), "a failure while connecting is retried");
    }
  }

  @Test
  void doesNotRetryACreateAfterATlsOrProxyFailureWhileConnecting() throws Exception {
    for (ClientCall call : Arrays.asList(CREATE_GLOSSARY, CREATE_EXPORT)) {
      SSLHandshakeException handshake = new SSLHandshakeException("handshake failed");
      ConnectionException thrown = failWhileConnecting(handshake, 1, new AtomicInteger(), call);
      assertSame(handshake, thrown.getCause());
      assertFalse(thrown.getShouldRetry(), "a TLS failure is not retried");

      DeepLException refused = failThroughRefusingProxy(call, 403, new AtomicInteger());
      if (call == CREATE_EXPORT) {
        assertTrue(refused instanceof ConnectionException, refused.toString());
      }
      assertFalse(refused.getShouldRetry(), "a refused proxy tunnel is not retried");
    }
  }

  @Test
  void doesNotRetryAProxyErrorAnswerToConnect() throws Exception {
    ClientCall patch = c -> c.updateMultilingualGlossaryName("g1", "n");
    for (int status : new int[] {403, 429, 503}) {
      for (ClientCall call : Arrays.asList(CREATE_EXPORT, patch)) {
        AtomicInteger connections = new AtomicInteger();
        DeepLException thrown = failThroughRefusingProxy(call, status, connections);
        assertEquals(ConnectionException.class, thrown.getClass(), thrown.toString());
        assertFalse(thrown.getShouldRetry(), "a proxy error answer is not retried");
        assertEquals(1, connections.get(), "status " + status);
      }
    }
  }

  @Test
  void sendsPatchRequestsDirectlyWithNoProxy() {
    DeepLClientOptions options = new DeepLClientOptions();
    options.setServerUrl("https://example.invalid").setProxy(Proxy.NO_PROXY).setMaxRetries(0);
    ConnectionException thrown =
        assertThrows(
            ConnectionException.class,
            () -> new DeepLClient("k", options).updateMultilingualGlossaryName("g1", "n"));
    assertTrue(thrown.getCause() instanceof UnknownHostException, thrown.toString());
  }

  @Test
  void sendsPatchRequestsThroughASocksProxy() throws Exception {
    AtomicInteger firstByte = new AtomicInteger(-1);
    try (ServerSocket server = new ServerSocket(0)) {
      Thread acceptor =
          new Thread(
              () -> {
                try (Socket socket = server.accept()) {
                  firstByte.set(socket.getInputStream().read());
                } catch (Exception closed) {
                  // the server socket was closed at the end of the test
                }
              });
      acceptor.start();
      DeepLClientOptions options = new DeepLClientOptions();
      options
          .setServerUrl("http://127.0.0.1:1")
          .setProxy(
              new Proxy(
                  Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", server.getLocalPort())))
          .setMaxRetries(0);
      DeepLClient socksClient = new DeepLClient("k", options);
      assertThrows(
          ConnectionException.class, () -> socksClient.updateMultilingualGlossaryName("g1", "n"));
      acceptor.join(5000);
    }
    assertEquals(5, firstByte.get());
  }

  @Test
  void sendsTheHostNameToTheSocksProxyInPatchRequests() throws Exception {
    AtomicInteger addressType = new AtomicInteger(-1);
    AtomicReference<String> hostName = new AtomicReference<>();
    try (ServerSocket server = new ServerSocket(0)) {
      Thread acceptor =
          new Thread(
              () -> {
                try (Socket socket = server.accept()) {
                  DataInputStream in = new DataInputStream(socket.getInputStream());
                  in.readUnsignedByte(); // version
                  in.skipBytes(in.readUnsignedByte()); // offered authentication methods
                  socket.getOutputStream().write(new byte[] {5, 0});
                  in.skipBytes(3); // version, CONNECT, reserved
                  addressType.set(in.readUnsignedByte());
                  byte[] host = new byte[in.readUnsignedByte()];
                  in.readFully(host);
                  hostName.set(new String(host, StandardCharsets.US_ASCII));
                } catch (Exception closed) {
                  // the server socket was closed at the end of the test
                }
              });
      acceptor.start();
      DeepLClientOptions options = new DeepLClientOptions();
      options
          .setServerUrl("http://deepl-test.invalid")
          .setProxy(
              new Proxy(
                  Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", server.getLocalPort())))
          .setMaxRetries(0);
      DeepLClient socksClient = new DeepLClient("k", options);
      assertThrows(
          ConnectionException.class, () -> socksClient.updateMultilingualGlossaryName("g1", "n"));
      acceptor.join(5000);
    }
    assertEquals(3, addressType.get(), "address type: domain name");
    assertEquals("deepl-test.invalid", hostName.get());
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
    ConnectionException thrown =
        assertThrows(ConnectionException.class, () -> client(0).getUsage());
    assertTrue(thrown.getShouldRetry());
  }

  @Test
  void retriesAResponseBodyThatIsCutOff() throws Exception {
    wm.stubFor(
        get("/v2/usage")
            .inScenario("cut off")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("recovered")
            .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    wm.stubFor(
        get("/v2/usage")
            .inScenario("cut off")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(USAGE)));
    client(1).getUsage();
    wm.verify(2, getRequestedFor(urlEqualTo("/v2/usage")));
  }

  @Test
  void throwsConnectionExceptionWhenThePatchResponseBodyIsCutOff() {
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    ConnectionException thrown =
        assertThrows(
            ConnectionException.class, () -> client(0).updateMultilingualGlossaryName("g1", "n"));
    assertTrue(thrown.getShouldRetry());
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
  void rethrowsTheInterruptionUnwrappedWhenUpdatingAGlossaryName() {
    wm.stubFor(patch(urlEqualTo("/v3/glossaries/g1")).willReturn(aResponse().withStatus(503)));
    Thread.currentThread().interrupt();
    try {
      assertThrows(
          InterruptedException.class, () -> client(1).updateMultilingualGlossaryName("g1", "n"));
    } finally {
      Thread.interrupted();
    }
    wm.verify(1, patchRequestedFor(urlEqualTo("/v3/glossaries/g1")));
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

  @Test
  void keepsTheLibraryHostAndBodyHeadersInPatchRequests() throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put("host", "example.invalid");
    headers.put("CONTENT-LENGTH", "999");
    headers.put("Transfer-Encoding", "chunked");
    DeepLClient client =
        OfflineClient.createClient(
            wm, (DeepLClientOptions) new DeepLClientOptions().setHeaders(headers).setMaxRetries(0));
    wm.stubFor(
        patch(urlEqualTo("/v3/glossaries/g1"))
            .withHeader("Host", equalTo("localhost:" + wm.getPort()))
            .willReturn(okJson(GLOSSARY)));

    MultilingualGlossaryInfo result = client.updateMultilingualGlossaryName("g1", "n");

    assertEquals("g1", result.getGlossaryId());
    wm.verify(
        1, patchRequestedFor(urlEqualTo("/v3/glossaries/g1")).withoutHeader("Transfer-Encoding"));
  }
}
