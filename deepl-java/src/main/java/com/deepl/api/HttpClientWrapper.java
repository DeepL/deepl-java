// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.http.*;
import com.deepl.api.utils.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPatch;
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
    return sendRequestWithBackoff(GET, relativeUrl, null).toStringResponse();
  }

  public HttpResponse sendDeleteRequestWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendRequestWithBackoff(DELETE, relativeUrl, content).toStringResponse();
  }

  public HttpResponse sendDeleteRequestWithBackoff(String relativeUrl)
      throws InterruptedException, DeepLException {
    return sendDeleteRequestWithBackoff(relativeUrl, null);
  }

  public HttpResponse sendRequestWithBackoff(String relativeUrl)
      throws InterruptedException, DeepLException {
    return sendRequestWithBackoff(POST, relativeUrl, null).toStringResponse();
  }

  public HttpResponse sendRequestWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendRequestWithBackoff(POST, relativeUrl, content).toStringResponse();
  }

  public HttpResponse sendPutRequestWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendRequestWithBackoff(PUT, relativeUrl, content).toStringResponse();
  }

  public HttpResponse sendJsonRequestWithBackoff(String relativeUrl, String jsonBody)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildJsonContent(jsonBody);
    return sendRequestWithBackoff(POST, relativeUrl, content).toStringResponse();
  }

  /**
   * Sends a JSON POST request that must not be repeated if the server may have processed it, such
   * as creating a resource. It is retried only after HTTP 429 or a DNS resolution failure, because
   * in both cases the request was not processed.
   */
  public HttpResponse sendJsonRequestRetryingOnlyUnprocessed(String relativeUrl, String jsonBody)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildJsonContent(jsonBody);
    return sendRequestToUrlWithBackoff(POST, serverUrl + relativeUrl, content, this.headers, true)
        .toStringResponse();
  }

  public HttpResponse sendJsonRequestWithBackoff(String method, String relativeUrl, String jsonBody)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildJsonContent(jsonBody);
    return sendRequestWithBackoff(method, relativeUrl, content).toStringResponse();
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
    HttpClientBuilder builder = HttpClients.custom().useSystemProperties();
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

      // Get the response stream
      InputStream responseStream =
          (response.getStatusLine().getStatusCode() >= 200
                  && response.getStatusLine().getStatusCode() < 400)
              ? response.getEntity().getContent()
              : new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity()));

      return new HttpResponseStream(response.getStatusLine().getStatusCode(), responseStream)
          .toStringResponse();
    } catch (SocketTimeoutException e) {
      throw new ConnectionException(e.getMessage(), true, e);
    } catch (RuntimeException | IOException e) {
      throw new ConnectionException(e.getMessage(), false, e);
    }
  }

  public HttpResponseStream downloadWithBackoff(
      String relativeUrl, @Nullable Iterable<KeyValuePair<String, String>> params)
      throws InterruptedException, DeepLException {
    HttpContent content = HttpContent.buildFormURLEncodedContent(params);
    return sendRequestWithBackoff(POST, relativeUrl, content);
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
    return sendRequestWithBackoff(POST, relativeUrl, content).toStringResponse();
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
    return sendRequestToUrlWithBackoff(PUT, url, content, Collections.emptyMap())
        .toStringResponse();
  }

  /**
   * Downloads content from an absolute URL outside of the DeepL API, for example a pre-signed
   * storage URL returned by a completed translation memory export job. The DeepL headers, in
   * particular the Authorization header, are deliberately not sent to third-party storage.
   */
  public HttpResponseStream downloadAssetWithBackoff(String url)
      throws InterruptedException, DeepLException {
    return sendRequestToUrlWithBackoff(GET, url, null, Collections.emptyMap());
  }

  // Sends a request with exponential backoff
  private HttpResponseStream sendRequestWithBackoff(
      String method, String relativeUrl, HttpContent content)
      throws InterruptedException, DeepLException {
    return sendRequestToUrlWithBackoff(method, serverUrl + relativeUrl, content, this.headers);
  }

  // Sends a request to an absolute URL with exponential backoff
  private HttpResponseStream sendRequestToUrlWithBackoff(
      String method, String url, @Nullable HttpContent content, Map<String, String> requestHeaders)
      throws InterruptedException, DeepLException {
    return sendRequestToUrlWithBackoff(method, url, content, requestHeaders, false);
  }

  private HttpResponseStream sendRequestToUrlWithBackoff(
      String method,
      String url,
      @Nullable HttpContent content,
      Map<String, String> requestHeaders,
      boolean retryOnlyUnprocessed)
      throws InterruptedException, DeepLException {
    BackoffTimer backoffTimer = new BackoffTimer(this.minTimeout);
    while (true) {
      try {
        HttpResponseStream response =
            sendRequest(
                method,
                url,
                backoffTimer.getTimeoutMillis(),
                content,
                requestHeaders,
                retryOnlyUnprocessed);
        if (backoffTimer.getNumRetries() >= this.maxRetries) {
          return response;
        } else if (response.getCode() != 429
            && (retryOnlyUnprocessed || response.getCode() < 500)) {
          return response;
        }
        response.close();
      } catch (ConnectionException exception) {
        // A failed DNS lookup means the request was never sent, so a retry cannot duplicate it.
        boolean shouldRetry =
            retryOnlyUnprocessed
                ? exception.getCause() instanceof UnknownHostException
                : exception.getShouldRetry();
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
      boolean retryOnlyUnprocessed)
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

        try (OutputStream output = connection.getOutputStream()) {
          output.write(content.getContent());
        }
      }

      int responseCode = connection.getResponseCode();
      InputStream responseStream =
          (responseCode >= 200 && responseCode < 400)
              ? connection.getInputStream()
              : connection.getErrorStream();
      return new HttpResponseStream(responseCode, responseStream);
    } catch (SocketTimeoutException e) {
      throw new ConnectionException(e.getMessage(), true, e);
    } catch (RuntimeException | IOException e) {
      throw new ConnectionException(e.getMessage(), false, e);
    }
  }
}
