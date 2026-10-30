// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.http.*;
import com.deepl.api.utils.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.*;

/**
 * Helper class providing functions to make HTTP requests and retry with exponential-backoff.
 *
 * <p>This class is internal; you should not use this class directly.
 */
class HttpClientWrapper {
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String GET = "GET";
  private static final String POST = "POST";
  private static final String DELETE = "DELETE";
  private static final String PUT = "PUT";
  private final String serverUrl;
  private final Map<String, String> headers;
  private final Duration minTimeout;
  private final @Nullable Proxy proxy;
  private final int maxRetries;

  public HttpClientWrapper(
      String serverUrl,
      Map<String, String> headers,
      Duration minTimeout,
      @Nullable Proxy proxy,
      int maxRetries) {
    this.serverUrl = serverUrl;
    this.headers = headers;
    this.minTimeout = minTimeout;
    this.proxy = proxy;
    this.maxRetries = maxRetries;
  }

  public HttpResponse sendGetRequestWithBackoff(String relativeUrl)
      throws InterruptedException, DeepLException {
    return sendRequestWithBackoff(GET, relativeUrl, null);
  }

  public HttpResponse sendDeleteRequestWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendRequestWithBackoff(DELETE, relativeUrl, content);
  }

  public HttpResponse sendDeleteRequestWithBackoff(String relativeUrl)
      throws InterruptedException, DeepLException {
    return sendDeleteRequestWithBackoff(relativeUrl, null);
  }

  public HttpResponse sendRequestWithBackoff(String relativeUrl)
      throws InterruptedException, DeepLException {
    return sendRequestWithBackoff(POST, relativeUrl, null);
  }

  public HttpResponse sendRequestWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendRequestWithBackoff(POST, relativeUrl, content);
  }

  public HttpResponse sendPutRequestWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendRequestWithBackoff(PUT, relativeUrl, content);
  }

  public HttpResponse sendJsonRequestWithBackoff(String relativeUrl, String jsonBody)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildJsonContent(jsonBody);
    return sendRequestWithBackoff(POST, relativeUrl, content);
  }

  /**
   * Sends a JSON POST request that must not be repeated if the server may have processed it, such
   * as creating a resource. It is retried only when the request was not sent: after HTTP 429, a DNS
   * resolution failure, a refused connection or a connect timeout.
   */
  public HttpResponse sendJsonRequestRetryingOnlyUnprocessed(String relativeUrl, String jsonBody)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildJsonContent(jsonBody);
    return sendRequestToUrlWithBackoff(
            POST, serverUrl + relativeUrl, content, this.headers, true, true)
        .toStringResponse();
  }

  public HttpResponse sendJsonRequestWithBackoff(String method, String relativeUrl, String jsonBody)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildJsonContent(jsonBody);
    return sendRequestWithBackoff(method, relativeUrl, content);
  }

  public HttpResponse sendJsonPatchRequestWithBackoff(String relativeUrl, String jsonBody)
      throws InterruptedException, DeepLException {
    return sendPatchRequestWithBackoff(relativeUrl, HttpContent.buildJsonContent(jsonBody));
  }

  public HttpResponse sendPatchRequestWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendPatchRequestWithBackoff(relativeUrl, content);
  }

  private HttpResponse sendPatchRequestWithBackoff(String relativeUrl, HttpContent content)
      throws InterruptedException, DeepLException {
    BackoffTimer backoffTimer = new BackoffTimer(this.minTimeout);
    while (true) {
      try {
        HttpResponse response = sendPatchRequest(relativeUrl, content, backoffTimer);
        if (backoffTimer.getNumRetries() >= this.maxRetries) {
          return response;
        } else if (response.getCode() != 429 && response.getCode() < 500) {
          return response;
        }
      } catch (ConnectionException exception) {
        if (!exception.getShouldRetry() || backoffTimer.getNumRetries() >= this.maxRetries) {
          throw exception;
        }
      }
      backoffTimer.sleepUntilRetry();
    }
  }

  private HttpResponse sendPatchRequest(
      String relativeUrl, HttpContent content, BackoffTimer backoffTimer) throws DeepLException {
    // Like HttpURLConnection, honor the JVM's proxy settings unless a proxy was configured.
    // Automatic retries are disabled so that only the backoff loop retries, up to maxRetries.
    HttpClientBuilder builder =
        HttpClients.custom().useSystemProperties().disableAutomaticRetries();
    if (proxy != null) {
      InetSocketAddress addr = (InetSocketAddress) proxy.address();
      builder.setProxy(new HttpHost(addr.getHostName(), addr.getPort()));
    }
    try (CloseableHttpClient httpClient = builder.build()) {
      HttpPatch request = new HttpPatch(serverUrl + relativeUrl);

      // Set timeouts
      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectTimeout((int) backoffTimer.getTimeoutMillis())
              .setSocketTimeout((int) backoffTimer.getTimeoutMillis())
              .build();
      request.setConfig(requestConfig);

      // Set headers
      for (Map.Entry<String, String> entry : this.headers.entrySet()) {
        request.setHeader(entry.getKey(), entry.getValue());
      }

      request.setHeader("Content-Type", content.getContentType());
      request.setEntity(new ByteArrayEntity(content.getContent()));

      // Execute the request
      org.apache.http.HttpResponse response = httpClient.execute(request);

      // The body is read here so that a failure while reading it is retried like any other.
      byte[] body =
          response.getEntity() == null
              ? new byte[0]
              : EntityUtils.toByteArray(response.getEntity());
      return new HttpResponse(
          response.getStatusLine().getStatusCode(), new String(body, StandardCharsets.UTF_8));
    } catch (RuntimeException | IOException e) {
      throw toConnectionException(e);
    }
  }

  /** Checks a download response before its body is copied, throwing for an error status. */
  interface ResponseCheck {
    void check(HttpResponseStream response) throws DeepLException;
  }

  /**
   * Downloads the response body to the output stream. A transient failure while reading the body is
   * retried like a failed request as long as nothing has been written to the output stream yet.
   */
  public void downloadWithBackoff(
      String relativeUrl,
      @Nullable Iterable<KeyValuePair<String, String>> params,
      ResponseCheck check,
      OutputStream outputStream)
      throws InterruptedException, DeepLException, IOException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    downloadToStreamWithBackoff(
        POST, serverUrl + relativeUrl, content, this.headers, check, outputStream);
  }

  public HttpResponse uploadWithBackoff(
      String relativeUrl,
      @Nullable Iterable<KeyValuePair<String, String>> params,
      String fileName,
      InputStream inputStream)
      throws InterruptedException, DeepLException {
    ArrayList<KeyValuePair<String, Object>> fields = new ArrayList<>();
    fields.add(new KeyValuePair<>("file", new NamedStream(fileName, inputStream)));
    if (params != null) {
      params.forEach(
          (KeyValuePair<String, String> entry) -> {
            fields.add(new KeyValuePair<>(entry.getKey(), entry.getValue()));
          });
    }
    HttpContent content;
    try {
      content = HttpContent.buildMultipartFormDataContent(fields);
    } catch (Exception e) {
      throw new DeepLException("Failed building request", e);
    }
    return sendRequestWithBackoff(POST, relativeUrl, content);
  }

  /**
   * Uploads content to an absolute URL outside of the DeepL API, for example a pre-signed storage
   * URL returned by a translation memory import job. The DeepL headers, in particular the
   * Authorization header, are deliberately not sent to third-party storage.
   */
  public HttpResponse sendAssetPutRequestWithBackoff(
      String url, byte[] fileContent, String contentType)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildRawContent(contentType, fileContent);
    return sendRequestToUrlWithBackoff(PUT, url, content, Collections.emptyMap(), false, true)
        .toStringResponse();
  }

  /**
   * Downloads content from an absolute URL outside of the DeepL API, for example a pre-signed
   * storage URL returned by a completed translation memory export job. The DeepL headers, in
   * particular the Authorization header, are deliberately not sent to third-party storage.
   */
  public void downloadAssetWithBackoff(String url, ResponseCheck check, OutputStream outputStream)
      throws InterruptedException, DeepLException, IOException {
    downloadToStreamWithBackoff(GET, url, null, Collections.emptyMap(), check, outputStream);
  }

  // Retrying after the first byte was written would duplicate it in the output stream, which
  // cannot be rewound; buffering the body instead would hold whole documents in memory.
  private void downloadToStreamWithBackoff(
      String method,
      String url,
      @Nullable HttpContent content,
      Map<String, String> requestHeaders,
      ResponseCheck check,
      OutputStream outputStream)
      throws InterruptedException, DeepLException, IOException {
    BackoffTimer backoffTimer = new BackoffTimer(this.minTimeout);
    while (true) {
      WriteTrackingOutputStream output = new WriteTrackingOutputStream(outputStream);
      try (HttpResponseStream response =
          sendRequestToUrlWithBackoff(
              method, url, content, requestHeaders, false, false, backoffTimer)) {
        check.check(response);
        if (response.getBody() != null) {
          copyBody(response.getBody(), output);
        }
        return;
      } catch (ConnectionException exception) {
        if (!exception.getShouldRetry()
            || output.written
            || backoffTimer.getNumRetries() >= this.maxRetries) {
          throw exception;
        }
      }
      backoffTimer.sleepUntilRetry();
    }
  }

  /** Forwards writes to the wrapped stream, recording whether anything was written. */
  private static class WriteTrackingOutputStream extends FilterOutputStream {
    private boolean written;

    WriteTrackingOutputStream(OutputStream out) {
      super(out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (len > 0) {
        written = true;
      }
      out.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
      written = true;
      out.write(b);
    }
  }

  // Sends a request with exponential backoff and reads the response body
  private HttpResponse sendRequestWithBackoff(
      String method, String relativeUrl, @Nullable HttpContent content)
      throws InterruptedException, DeepLException {
    return sendRequestToUrlWithBackoff(
            method, serverUrl + relativeUrl, content, this.headers, false, true)
        .toStringResponse();
  }

  // With readBody false, the response body is returned as a stream and read by the caller.
  private HttpResponseStream sendRequestToUrlWithBackoff(
      String method,
      String url,
      @Nullable HttpContent content,
      Map<String, String> requestHeaders,
      boolean retryOnlyUnprocessed,
      boolean readBody)
      throws InterruptedException, DeepLException {
    return sendRequestToUrlWithBackoff(
        method,
        url,
        content,
        requestHeaders,
        retryOnlyUnprocessed,
        readBody,
        new BackoffTimer(this.minTimeout));
  }

  private HttpResponseStream sendRequestToUrlWithBackoff(
      String method,
      String url,
      @Nullable HttpContent content,
      Map<String, String> requestHeaders,
      boolean retryOnlyUnprocessed,
      boolean readBody,
      BackoffTimer backoffTimer)
      throws InterruptedException, DeepLException {
    while (true) {
      try {
        HttpResponseStream response =
            sendRequest(
                method,
                url,
                backoffTimer.getTimeoutMillis(),
                content,
                requestHeaders,
                retryOnlyUnprocessed,
                readBody);
        if (backoffTimer.getNumRetries() >= this.maxRetries) {
          return response;
        } else if (response.getCode() != 429
            && (retryOnlyUnprocessed || response.getCode() < 500)) {
          return response;
        }
        response.close();
      } catch (ConnectionException exception) {
        boolean shouldRetry =
            retryOnlyUnprocessed ? exception.getRequestNotSent() : exception.getShouldRetry();
        if (!shouldRetry || backoffTimer.getNumRetries() >= this.maxRetries) {
          throw exception;
        }
      }
      backoffTimer.sleepUntilRetry();
    }
  }

  private HttpResponseStream sendRequest(
      String method,
      String urlString,
      long timeoutMs,
      @Nullable HttpContent content,
      Map<String, String> requestHeaders,
      boolean retryOnlyUnprocessed,
      boolean readBody)
      throws ConnectionException {
    try {
      URL url = new URL(urlString);
      HttpURLConnection connection =
          (HttpURLConnection) (proxy != null ? url.openConnection(proxy) : url.openConnection());

      connection.setRequestMethod(method);
      connection.setConnectTimeout((int) timeoutMs);
      connection.setReadTimeout((int) timeoutMs);
      connection.setUseCaches(false);

      for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
        connection.setRequestProperty(entry.getKey(), entry.getValue());
      }

      if (content != null) {
        connection.setDoOutput(true);
        connection.setRequestProperty(CONTENT_TYPE, content.getContentType());
        if (retryOnlyUnprocessed) {
          // Stops HttpURLConnection from silently resending the POST (sun.net.http.retryPost)
          // after a connection failure the server may have processed. Trade-off: redirects (and,
          // over plain HTTP, 407 challenges) cannot be replayed from a streamed body.
          connection.setFixedLengthStreamingMode(content.getContent().length);
        }
      }

      // Connect separately: a DNS, refused or timed-out connect means nothing was sent yet.
      try {
        connection.connect();
      } catch (IOException e) {
        ConnectionException failure = toConnectionException(e);
        throw new ConnectionException(
            failure.getMessage(), failure.getShouldRetry(), e, isUnsentFailure(e));
      }

      if (content != null) {
        try (OutputStream output = connection.getOutputStream()) {
          output.write(content.getContent());
        }
      }

      int responseCode = connection.getResponseCode();
      InputStream responseStream =
          (responseCode >= 200 && responseCode < 400)
              ? connection.getInputStream()
              : connection.getErrorStream();
      if (readBody && responseStream != null) {
        // Read inside the attempt so that a failure while reading the body is retried.
        try (InputStream stream = responseStream) {
          ByteArrayOutputStream body = new ByteArrayOutputStream();
          StreamUtil.transferTo(stream, body);
          responseStream = new ByteArrayInputStream(body.toByteArray());
        }
      }
      return new HttpResponseStream(responseCode, responseStream);
    } catch (RuntimeException | IOException e) {
      throw toConnectionException(e);
    }
  }

  /**
   * Copies a streamed response body to the output stream. A failure while reading the body is a
   * {@link ConnectionException}; a failure while writing the output stays an {@link IOException}.
   */
  private static void copyBody(InputStream body, OutputStream outputStream)
      throws ConnectionException, IOException {
    final byte[] buffer = new byte[8192];
    while (true) {
      int read;
      try {
        read = body.read(buffer);
      } catch (RuntimeException | IOException e) {
        throw toConnectionException(e);
      }
      if (read < 0) {
        return;
      }
      outputStream.write(buffer, 0, read);
    }
  }

  // Connect, DNS, timeout, connection reset and broken pipe failures are transient: the request is
  // retried.
  private static ConnectionException toConnectionException(Exception e) {
    boolean shouldRetry = isUnsentFailure(e) || isConnectionResetOrBrokenPipe(e);
    return new ConnectionException(e.getMessage(), shouldRetry, e);
  }

  // Failures of the connect step after which the server cannot have received the request.
  private static boolean isUnsentFailure(Exception e) {
    return e instanceof SocketTimeoutException
        || e instanceof ConnectException
        || e instanceof NoRouteToHostException
        || e instanceof UnknownHostException
        || e instanceof ConnectTimeoutException;
  }

  // Java has no exception types for these, only SocketException messages such as
  // "Connection reset", "Connection reset by peer" and "Broken pipe (Write failed)".
  private static boolean isConnectionResetOrBrokenPipe(Exception e) {
    if (!(e instanceof SocketException) || e.getMessage() == null) {
      return false;
    }
    String message = e.getMessage().toLowerCase(Locale.ROOT);
    return message.contains("connection reset") || message.contains("broken pipe");
  }
}
