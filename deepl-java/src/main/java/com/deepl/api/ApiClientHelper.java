// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.http.HttpResponse;
import com.deepl.api.parsing.Parser;
import com.deepl.api.utils.KeyValuePair;
import com.google.gson.JsonSyntaxException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * HTTP helpers shared by the DeepL API clients.
 *
 * <p>This class is internal; you should not use this class directly.
 */
final class ApiClientHelper {
  private ApiClientHelper() {}

  /**
   * Creates the HTTP client wrapper with the Authorization and User-Agent headers. Custom headers
   * may replace the User-Agent (in any casing) but never the Authorization header.
   */
  static HttpClientWrapper createHttpClientWrapper(
      String sanitizedAuthKey, String serverUrl, TranslatorOptions options) {
    return createHttpClientWrapper(
        sanitizedAuthKey,
        serverUrl,
        options.getHeaders(),
        options.getSendPlatformInfo(),
        options.getAppInfo(),
        options.getTimeout(),
        options.getProxy(),
        options.getMaxRetries());
  }

  static HttpClientWrapper createHttpClientWrapper(
      String sanitizedAuthKey,
      String serverUrl,
      @Nullable Map<String, String> customHeaders,
      boolean sendPlatformInfo,
      @Nullable AppInfo appInfo,
      Duration timeout,
      @Nullable Proxy proxy,
      int maxRetries) {
    Map<String, String> headers = new HashMap<>();
    boolean hasCustomUserAgent = false;
    if (customHeaders != null) {
      for (Map.Entry<String, String> header : customHeaders.entrySet()) {
        if (!"Authorization".equalsIgnoreCase(header.getKey())) {
          headers.put(header.getKey(), header.getValue());
          hasCustomUserAgent |= "User-Agent".equalsIgnoreCase(header.getKey());
        }
      }
    }
    headers.put("Authorization", "DeepL-Auth-Key " + sanitizedAuthKey);
    if (!hasCustomUserAgent) {
      headers.put("User-Agent", Translator.constructUserAgentString(sendPlatformInfo, appInfo));
    }

    return new HttpClientWrapper(serverUrl, headers, timeout, proxy, maxRetries);
  }

  /**
   * Checks the response HTTP status is OK, otherwise throws corresponding exception.
   *
   * @param jsonParser Parser used to extract the error message from the response body.
   * @param response Response received from DeepL API.
   * @param inDocumentDownload True if document download function is used, otherwise false.
   * @param usingGlossary True if a glossary function is used, otherwise false.
   * @throws DeepLException Throws {@link DeepLException} or a derived exception depending on the
   *     type of error.
   */
  static void checkResponse(
      Parser jsonParser, HttpResponse response, boolean inDocumentDownload, boolean usingGlossary)
      throws DeepLException {
    checkResponse(jsonParser, response, inDocumentDownload, usingGlossary, "auth_key");
  }

  /**
   * Same as {@link #checkResponse(Parser, HttpResponse, boolean, boolean)}, naming the key to check
   * in the authorization failure message.
   */
  static void checkResponse(
      Parser jsonParser,
      HttpResponse response,
      boolean inDocumentDownload,
      boolean usingGlossary,
      String keyName)
      throws DeepLException {
    if (response.getCode() >= 200 && response.getCode() < 300) {
      return;
    }

    String messageSuffix = "";
    String body = response.getBody();
    if (body != null && !body.isEmpty()) {
      try {
        String errorMessage = jsonParser.parseErrorMessage(body);
        messageSuffix =
            errorMessage.isEmpty() ? ", response: " + body : ", error message: " + errorMessage;
      } catch (JsonSyntaxException ignored) {
        messageSuffix = ", response: " + body;
      }
    }

    throw createException(
            response.getCode(), messageSuffix, inDocumentDownload, usingGlossary, keyName)
        .setHttpStatusCode(response.getCode());
  }

  @SuppressWarnings("deprecation")
  private static DeepLException createException(
      int code,
      String messageSuffix,
      boolean inDocumentDownload,
      boolean usingGlossary,
      String keyName) {
    switch (code) {
      case HttpURLConnection.HTTP_BAD_REQUEST:
        return new DeepLException("Bad request" + messageSuffix);
      case HttpURLConnection.HTTP_UNAUTHORIZED:
      case HttpURLConnection.HTTP_FORBIDDEN:
        return new AuthorizationException(
            "Authorization failure, check " + keyName + messageSuffix);
      case HttpURLConnection.HTTP_NOT_FOUND:
        if (usingGlossary) {
          return new GlossaryNotFoundException("Glossary not found" + messageSuffix);
        } else {
          return new NotFoundException("Not found" + messageSuffix);
        }
      case 429:
        return new TooManyRequestsException(
            "Too many requests, DeepL servers are currently experiencing high load"
                + messageSuffix);
      case 456:
        return new QuotaExceededException(
            "Quota for this billing period has been exceeded" + messageSuffix);
      case HttpURLConnection.HTTP_UNAVAILABLE:
        if (inDocumentDownload) {
          return new DocumentNotReadyException("Document not ready" + messageSuffix);
        } else {
          return new DeepLException("Service unavailable" + messageSuffix);
        }
      default:
        return new DeepLException("Unknown error" + messageSuffix);
    }
  }

  /** Creates the query string for the given parameters, including the leading "?" if non-empty. */
  static String createQueryString(List<KeyValuePair<String, String>> queryParams) {
    if (queryParams.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder("?");
    for (int i = 0; i < queryParams.size(); i++) {
      if (i > 0) {
        sb.append("&");
      }
      KeyValuePair<String, String> param = queryParams.get(i);
      try {
        sb.append(encodeQueryComponent(param.getKey()))
            .append("=")
            .append(encodeQueryComponent(param.getValue()));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("UTF-8 encoding not supported", e);
      }
    }
    return sb.toString();
  }

  // URLEncoder encodes a space as "+", which is correct for a form body but not for a URI query
  // string. Any literal "+" is already escaped as %2B by URLEncoder, so every remaining "+" is a
  // space.
  static String encodeQueryComponent(String value) throws UnsupportedEncodingException {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
  }
}
