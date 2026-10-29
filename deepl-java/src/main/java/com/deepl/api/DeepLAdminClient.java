// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.http.HttpResponse;
import com.deepl.api.parsing.Parser;
import com.deepl.api.utils.KeyValuePair;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Client for the DeepL Admin API, used to manage the developer keys of your organization and to
 * retrieve usage analytics. Initialize an instance of this class using an Admin API key as found in
 * your <a href="https://www.deepl.com/your-account/keys?keyType=admin">DeepL account</a>.
 *
 * <p>Admin keys cannot be used for translation; developer keys cannot access the Admin API.
 */
public class DeepLAdminClient {
  /** Base URL for the DeepL Admin API, which is only available on the DeepL API Pro server. */
  private static final String DEEPL_SERVER_URL = "https://api.deepl.com";

  private final Parser jsonParser = new Parser();
  private final HttpClientWrapper httpClientWrapper;

  /**
   * Initializes a new DeepLAdminClient object using your Admin API key.
   *
   * <p>Note: This function does not establish a connection to the DeepL API.
   *
   * @param adminKey Admin API key as found in your DeepL account.
   * @param options Additional options controlling client behaviour.
   * @throws IllegalArgumentException If adminKey is null or blank, or if options is null.
   */
  public DeepLAdminClient(String adminKey, DeepLAdminClientOptions options)
      throws IllegalArgumentException {
    if (options == null) {
      throw new IllegalArgumentException("options cannot be null");
    }
    if (adminKey == null || adminKey.trim().isEmpty()) {
      throw new IllegalArgumentException("adminKey cannot be null or empty");
    }
    String sanitizedAdminKey = adminKey.trim();
    String serverUrl = options.getServerUrl() != null ? options.getServerUrl() : DEEPL_SERVER_URL;
    this.httpClientWrapper =
        ApiClientHelper.createHttpClientWrapper(
            sanitizedAdminKey,
            serverUrl,
            options.getHeaders(),
            options.getSendPlatformInfo(),
            options.getAppInfo(),
            options.getTimeout(),
            options.getProxy(),
            options.getMaxRetries());
  }

  /**
   * Initializes a new DeepLAdminClient object using your Admin API key and default options.
   *
   * @see DeepLAdminClient#DeepLAdminClient(String, DeepLAdminClientOptions)
   */
  public DeepLAdminClient(String adminKey) throws IllegalArgumentException {
    this(adminKey, new DeepLAdminClientOptions());
  }

  /**
   * Retrieves usage statistics for the organization within the given date range. Dates are UTC
   * calendar days and both the start and the end day are included. A {@link LocalDate} is used as
   * is. To get a date from a moment in time, convert it in UTC, for example with <code>
   * instant.atZone(ZoneOffset.UTC).toLocalDate()</code>: 2026-10-20T00:00+02:00 is the UTC day
   * 2026-10-19. The API requires the end date to be after the start date and at most 366 days after
   * it.
   *
   * @param startDate Start date of the report, a UTC day.
   * @param endDate End date of the report, a UTC day.
   * @param options Options controlling the grouping, or <code>null</code> for totals only.
   * @return {@link AdminUsageReport} containing the usage statistics.
   * @throws IllegalArgumentException If startDate or endDate is null.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public AdminUsageReport getUsageAnalytics(
      LocalDate startDate, LocalDate endDate, @Nullable AdminUsageAnalyticsOptions options)
      throws DeepLException, InterruptedException {
    List<KeyValuePair<String, String>> queryParams = createDateParams(startDate, endDate);
    if (options != null && options.getGroupBy() != null) {
      queryParams.add(new KeyValuePair<>("group_by", options.getGroupBy().getValue()));
    }
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(
            "/v2/admin/analytics" + ApiClientHelper.createQueryString(queryParams));
    checkResponse(response);
    return jsonParser.parseAdminUsageReport(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate,
   * AdminUsageAnalyticsOptions)} but without grouping.
   *
   * @see DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate, AdminUsageAnalyticsOptions)
   */
  public AdminUsageReport getUsageAnalytics(LocalDate startDate, LocalDate endDate)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(startDate, endDate, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate,
   * AdminUsageAnalyticsOptions)} but takes the dates as ISO 8601 strings in the format <code>
   * YYYY-MM-DD</code>, which are UTC calendar days.
   *
   * @throws IllegalArgumentException If a date is null or not a valid <code>YYYY-MM-DD</code> date.
   * @see DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate, AdminUsageAnalyticsOptions)
   */
  public AdminUsageReport getUsageAnalytics(
      String startDate, String endDate, @Nullable AdminUsageAnalyticsOptions options)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(parseDate(startDate), parseDate(endDate), options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(String, String,
   * AdminUsageAnalyticsOptions)} but without grouping.
   *
   * @see DeepLAdminClient#getUsageAnalytics(String, String, AdminUsageAnalyticsOptions)
   */
  public AdminUsageReport getUsageAnalytics(String startDate, String endDate)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(startDate, endDate, null);
  }

  /**
   * Retrieves usage statistics per custom tag within the given date range. Dates are UTC calendar
   * days and both the start and the end day are included. A {@link LocalDate} is used as is. To get
   * a date from a moment in time, convert it in UTC, for example with <code>
   * instant.atZone(ZoneOffset.UTC).toLocalDate()</code>: 2026-10-20T00:00+02:00 is the UTC day
   * 2026-10-19. The API requires the end date to be after the start date and at most 366 days after
   * it. Results are paginated; set the {@link CustomTagUsageReport#getNextPage()} value of a report
   * as the page option to retrieve the following page.
   *
   * @param startDate Start date of the report, a UTC day.
   * @param endDate End date of the report, a UTC day.
   * @param options Options controlling the aggregation and the page, or <code>null</code> for the
   *     first page aggregated over the whole period.
   * @return {@link CustomTagUsageReport} containing the usage statistics.
   * @throws IllegalArgumentException If startDate or endDate is null.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      LocalDate startDate, LocalDate endDate, @Nullable CustomTagUsageAnalyticsOptions options)
      throws DeepLException, InterruptedException {
    List<KeyValuePair<String, String>> queryParams = createDateParams(startDate, endDate);
    if (options != null && options.getAggregateBy() != null) {
      queryParams.add(new KeyValuePair<>("aggregate_by", options.getAggregateBy().getValue()));
    }
    if (options != null && options.getPage() != null) {
      queryParams.add(new KeyValuePair<>("page", options.getPage().toString()));
    }
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(
            "/v2/admin/analytics/custom-tags" + ApiClientHelper.createQueryString(queryParams));
    checkResponse(response);
    return jsonParser.parseCustomTagUsageReport(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * CustomTagUsageAnalyticsOptions)} but retrieves the first page aggregated over the whole period.
   *
   * @see DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   *     CustomTagUsageAnalyticsOptions)
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(LocalDate startDate, LocalDate endDate)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(startDate, endDate, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * CustomTagUsageAnalyticsOptions)} but takes the dates as ISO 8601 strings in the format <code>
   * YYYY-MM-DD</code>, which are UTC calendar days.
   *
   * @throws IllegalArgumentException If a date is null or not a valid <code>YYYY-MM-DD</code> date.
   * @see DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   *     CustomTagUsageAnalyticsOptions)
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      String startDate, String endDate, @Nullable CustomTagUsageAnalyticsOptions options)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(parseDate(startDate), parseDate(endDate), options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(String, String,
   * CustomTagUsageAnalyticsOptions)} but retrieves the first page aggregated over the whole period.
   *
   * @see DeepLAdminClient#getCustomTagUsageAnalytics(String, String,
   *     CustomTagUsageAnalyticsOptions)
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(String startDate, String endDate)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(startDate, endDate, null);
  }

  /**
   * Creates a developer key in the organization. The request is retried only when the API cannot
   * have processed it (HTTP 429, a DNS resolution failure, a refused connection or a connect
   * timeout), so that a lost response cannot lead to several keys being created. For the same
   * reason the request is sent in streaming mode, so it does not follow HTTP redirects: a redirect
   * fails with a {@link ConnectionException}.
   *
   * @param label Label for the new key, or <code>null</code> to let the API use "DeepL API Key".
   * @return {@link DeveloperKey} describing the created key.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey createDeveloperKey(@Nullable String label)
      throws DeepLException, InterruptedException {
    JsonObject requestData = new JsonObject();
    if (label != null) {
      requestData.addProperty("label", label);
    }
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(
            "/v2/admin/developer-keys", requestData.toString());
    checkResponse(response);
    return jsonParser.parseDeveloperKey(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLAdminClient#createDeveloperKey(String)} but uses the default
   * label.
   *
   * @see DeepLAdminClient#createDeveloperKey(String)
   */
  public DeveloperKey createDeveloperKey() throws DeepLException, InterruptedException {
    return createDeveloperKey(null);
  }

  /**
   * Retrieves all developer keys in the organization, including deactivated ones.
   *
   * @return List of {@link DeveloperKey} objects.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<DeveloperKey> getDeveloperKeys() throws DeepLException, InterruptedException {
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff("/v2/admin/developer-keys");
    checkResponse(response);
    return jsonParser.parseDeveloperKeyList(response.getBody());
  }

  /**
   * Deactivates a developer key. Deactivation is permanent.
   *
   * @param keyId ID of the key to deactivate.
   * @return {@link DeveloperKey} describing the deactivated key.
   * @throws IllegalArgumentException If keyId is null or empty.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey deactivateDeveloperKey(String keyId)
      throws DeepLException, InterruptedException {
    return putDeveloperKey("/v2/admin/developer-keys/deactivate", createKeyRequest(keyId));
  }

  /**
   * Functions the same as {@link DeepLAdminClient#deactivateDeveloperKey(String)} but accepts a
   * {@link DeveloperKey}.
   *
   * @see DeepLAdminClient#deactivateDeveloperKey(String)
   */
  public DeveloperKey deactivateDeveloperKey(DeveloperKey key)
      throws DeepLException, InterruptedException {
    return deactivateDeveloperKey(keyIdOf(key));
  }

  /**
   * Changes the label of a developer key.
   *
   * @param keyId ID of the key to rename.
   * @param label New label for the key.
   * @return {@link DeveloperKey} describing the renamed key.
   * @throws IllegalArgumentException If keyId is null or empty.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey renameDeveloperKey(String keyId, String label)
      throws DeepLException, InterruptedException {
    JsonObject requestData = createKeyRequest(keyId);
    requestData.addProperty("label", label);
    return putDeveloperKey("/v2/admin/developer-keys/label", requestData);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#renameDeveloperKey(String, String)} but accepts a
   * {@link DeveloperKey}.
   *
   * @see DeepLAdminClient#renameDeveloperKey(String, String)
   */
  public DeveloperKey renameDeveloperKey(DeveloperKey key, String label)
      throws DeepLException, InterruptedException {
    return renameDeveloperKey(keyIdOf(key), label);
  }

  /**
   * Changes the monthly usage limits of a developer key. Only the limits set in <code>limits</code>
   * are changed; the other limits keep their current value. A <code>null</code> limit means
   * unlimited and 0 blocks that usage.
   *
   * @param keyId ID of the key to modify.
   * @param limits Usage limits to change.
   * @return {@link DeveloperKey} describing the modified key.
   * @throws IllegalArgumentException If keyId is null or empty.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey setDeveloperKeyUsageLimits(String keyId, DeveloperKeyUsageLimitsUpdate limits)
      throws DeepLException, InterruptedException {
    JsonObject requestData = createKeyRequest(keyId);
    if (limits != null) {
      limits.addTo(requestData);
    }
    return putDeveloperKey("/v2/admin/developer-keys/limits", requestData);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#setDeveloperKeyUsageLimits(String,
   * DeveloperKeyUsageLimitsUpdate)} but accepts a {@link DeveloperKey}.
   *
   * @see DeepLAdminClient#setDeveloperKeyUsageLimits(String, DeveloperKeyUsageLimitsUpdate)
   */
  public DeveloperKey setDeveloperKeyUsageLimits(
      DeveloperKey key, DeveloperKeyUsageLimitsUpdate limits)
      throws DeepLException, InterruptedException {
    return setDeveloperKeyUsageLimits(keyIdOf(key), limits);
  }

  private DeveloperKey putDeveloperKey(String relativeUrl, JsonObject requestData)
      throws DeepLException, InterruptedException {
    // JsonObject.toString() keeps explicit nulls, unlike Gson.toJson() with default settings.
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("PUT", relativeUrl, requestData.toString());
    checkResponse(response);
    return jsonParser.parseDeveloperKey(response.getBody());
  }

  private void checkResponse(HttpResponse response) throws DeepLException {
    ApiClientHelper.checkResponse(jsonParser, response, false, false, "adminKey");
  }

  private static JsonObject createKeyRequest(String keyId) {
    if (keyId == null || keyId.isEmpty()) {
      throw new IllegalArgumentException("keyId must not be empty");
    }
    JsonObject requestData = new JsonObject();
    requestData.addProperty("key_id", keyId);
    return requestData;
  }

  private static @Nullable String keyIdOf(DeveloperKey key) {
    return key != null ? key.getKeyId() : null;
  }

  private static LocalDate parseDate(String date) {
    if (date == null) {
      throw new IllegalArgumentException("startDate and endDate must not be null");
    }
    try {
      return LocalDate.parse(date);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("date must be in the format YYYY-MM-DD: " + date);
    }
  }

  private static List<KeyValuePair<String, String>> createDateParams(
      LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("startDate and endDate must not be null");
    }
    List<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    queryParams.add(new KeyValuePair<>("start_date", startDate.toString()));
    queryParams.add(new KeyValuePair<>("end_date", endDate.toString()));
    return queryParams;
  }
}
