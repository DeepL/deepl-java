// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.http.HttpResponse;
import com.deepl.api.parsing.Parser;
import com.deepl.api.utils.KeyValuePair;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
   * calendar days and both the start and the end day are included. The API requires the end date to
   * be after the start date and at most 366 days after it.
   *
   * <p>Dates can also be passed as <code>YYYY-MM-DD</code> strings, or as {@link Date} or {@link
   * Instant} values, of which the UTC day is sent: 2026-10-20T00:00+02:00 is the UTC day
   * 2026-10-19.
   *
   * @param startDate Start date of the report, a UTC day.
   * @param endDate End date of the report, a UTC day.
   * @param groupBy How the usage is grouped, or <code>null</code> for totals only.
   * @return {@link AdminUsageReport} containing the usage statistics.
   * @throws IllegalArgumentException If startDate or endDate is null.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public AdminUsageReport getUsageAnalytics(
      LocalDate startDate, LocalDate endDate, @Nullable AdminUsageGroupBy groupBy)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(startDate, endDate, groupBy != null ? groupBy.getValue() : null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate,
   * AdminUsageGroupBy)} but takes the grouping as a string, sent as given, for values not yet in
   * {@link AdminUsageGroupBy}.
   *
   * @see DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate, AdminUsageGroupBy)
   */
  public AdminUsageReport getUsageAnalytics(
      LocalDate startDate, LocalDate endDate, @Nullable String groupBy)
      throws DeepLException, InterruptedException {
    List<KeyValuePair<String, String>> queryParams = createDateParams(startDate, endDate);
    if (groupBy != null) {
      queryParams.add(new KeyValuePair<>("group_by", groupBy));
    }
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(
            "/v2/admin/analytics" + ApiClientHelper.createQueryString(queryParams));
    checkResponse(response);
    return jsonParser.parseAdminUsageReport(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate,
   * AdminUsageGroupBy)} but without grouping.
   */
  public AdminUsageReport getUsageAnalytics(LocalDate startDate, LocalDate endDate)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(startDate, endDate, (String) null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate,
   * AdminUsageGroupBy)} but takes the dates as ISO 8601 strings in the format <code>YYYY-MM-DD
   * </code>, which are UTC calendar days.
   *
   * @throws IllegalArgumentException If a date is null or not a valid <code>YYYY-MM-DD</code> date.
   */
  public AdminUsageReport getUsageAnalytics(
      String startDate, String endDate, @Nullable AdminUsageGroupBy groupBy)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(parseDate(startDate), parseDate(endDate), groupBy);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate, String)}
   * but takes the dates as <code>YYYY-MM-DD</code> strings.
   *
   * @throws IllegalArgumentException If a date is null or not a valid <code>YYYY-MM-DD</code> date.
   */
  public AdminUsageReport getUsageAnalytics(
      String startDate, String endDate, @Nullable String groupBy)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(parseDate(startDate), parseDate(endDate), groupBy);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(String, String,
   * AdminUsageGroupBy)} but without grouping.
   */
  public AdminUsageReport getUsageAnalytics(String startDate, String endDate)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(startDate, endDate, (String) null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate,
   * AdminUsageGroupBy)} but takes the dates as {@link Date} values, of which the UTC day is sent.
   */
  public AdminUsageReport getUsageAnalytics(
      Date startDate, Date endDate, @Nullable AdminUsageGroupBy groupBy)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(utcDay(startDate), utcDay(endDate), groupBy);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate, String)}
   * but takes the dates as {@link Date} values, of which the UTC day is sent.
   */
  public AdminUsageReport getUsageAnalytics(Date startDate, Date endDate, @Nullable String groupBy)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(utcDay(startDate), utcDay(endDate), groupBy);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(Date, Date, AdminUsageGroupBy)}
   * but without grouping.
   */
  public AdminUsageReport getUsageAnalytics(Date startDate, Date endDate)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(startDate, endDate, (String) null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate,
   * AdminUsageGroupBy)} but takes the dates as {@link Instant} values, of which the UTC day is
   * sent.
   */
  public AdminUsageReport getUsageAnalytics(
      Instant startDate, Instant endDate, @Nullable AdminUsageGroupBy groupBy)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(utcDay(startDate), utcDay(endDate), groupBy);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(LocalDate, LocalDate, String)}
   * but takes the dates as {@link Instant} values, of which the UTC day is sent.
   */
  public AdminUsageReport getUsageAnalytics(
      Instant startDate, Instant endDate, @Nullable String groupBy)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(utcDay(startDate), utcDay(endDate), groupBy);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getUsageAnalytics(Instant, Instant,
   * AdminUsageGroupBy)} but without grouping.
   */
  public AdminUsageReport getUsageAnalytics(Instant startDate, Instant endDate)
      throws DeepLException, InterruptedException {
    return getUsageAnalytics(startDate, endDate, (String) null);
  }

  /**
   * Retrieves usage statistics per custom tag within the given date range. Dates are UTC calendar
   * days and both the start and the end day are included. The API requires the end date to be after
   * the start date and at most 366 days after it. Results are paginated; pass the {@link
   * CustomTagUsageReport#getNextPage()} value of a report as <code>page</code> to retrieve the
   * following page.
   *
   * <p>Dates can also be passed as <code>YYYY-MM-DD</code> strings, or as {@link Date} or {@link
   * Instant} values, of which the UTC day is sent: 2026-10-20T00:00+02:00 is the UTC day
   * 2026-10-19.
   *
   * @param startDate Start date of the report, a UTC day.
   * @param endDate End date of the report, a UTC day.
   * @param aggregateBy Aggregate the usage over the whole period or by day, or <code>null</code>
   *     for the API default (whole period).
   * @param page Page number, as returned by {@link CustomTagUsageReport#getNextPage()} of the
   *     previous page, or <code>null</code> for the first page.
   * @return {@link CustomTagUsageReport} containing the usage statistics.
   * @throws IllegalArgumentException If startDate or endDate is null.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      LocalDate startDate,
      LocalDate endDate,
      @Nullable CustomTagUsageAggregateBy aggregateBy,
      @Nullable Integer page)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(
        startDate, endDate, aggregateBy != null ? aggregateBy.getValue() : null, page);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * CustomTagUsageAggregateBy, Integer)} but takes the aggregation as a string, sent as given, for
   * values not yet in {@link CustomTagUsageAggregateBy}.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      LocalDate startDate, LocalDate endDate, @Nullable String aggregateBy, @Nullable Integer page)
      throws DeepLException, InterruptedException {
    List<KeyValuePair<String, String>> queryParams = createDateParams(startDate, endDate);
    if (aggregateBy != null) {
      queryParams.add(new KeyValuePair<>("aggregate_by", aggregateBy));
    }
    if (page != null) {
      queryParams.add(new KeyValuePair<>("page", page.toString()));
    }
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(
            "/v2/admin/analytics/custom-tags" + ApiClientHelper.createQueryString(queryParams));
    checkResponse(response);
    return jsonParser.parseCustomTagUsageReport(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * CustomTagUsageAggregateBy, Integer)} but retrieves the first page aggregated over the whole
   * period.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(LocalDate startDate, LocalDate endDate)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(startDate, endDate, (String) null, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * CustomTagUsageAggregateBy, Integer)} but takes the dates as ISO 8601 strings in the format
   * <code>YYYY-MM-DD</code>, which are UTC calendar days.
   *
   * @throws IllegalArgumentException If a date is null or not a valid <code>YYYY-MM-DD</code> date.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      String startDate,
      String endDate,
      @Nullable CustomTagUsageAggregateBy aggregateBy,
      @Nullable Integer page)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(parseDate(startDate), parseDate(endDate), aggregateBy, page);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * String, Integer)} but takes the dates as <code>YYYY-MM-DD</code> strings.
   *
   * @throws IllegalArgumentException If a date is null or not a valid <code>YYYY-MM-DD</code> date.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      String startDate, String endDate, @Nullable String aggregateBy, @Nullable Integer page)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(parseDate(startDate), parseDate(endDate), aggregateBy, page);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(String, String,
   * CustomTagUsageAggregateBy, Integer)} but retrieves the first page aggregated over the whole
   * period.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(String startDate, String endDate)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(startDate, endDate, (String) null, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * CustomTagUsageAggregateBy, Integer)} but takes the dates as {@link Date} values, of which the
   * UTC day is sent.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      Date startDate,
      Date endDate,
      @Nullable CustomTagUsageAggregateBy aggregateBy,
      @Nullable Integer page)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(utcDay(startDate), utcDay(endDate), aggregateBy, page);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * String, Integer)} but takes the dates as {@link Date} values, of which the UTC day is sent.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      Date startDate, Date endDate, @Nullable String aggregateBy, @Nullable Integer page)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(utcDay(startDate), utcDay(endDate), aggregateBy, page);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(Date, Date,
   * CustomTagUsageAggregateBy, Integer)} but retrieves the first page aggregated over the whole
   * period.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(Date startDate, Date endDate)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(startDate, endDate, (String) null, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * CustomTagUsageAggregateBy, Integer)} but takes the dates as {@link Instant} values, of which
   * the UTC day is sent.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      Instant startDate,
      Instant endDate,
      @Nullable CustomTagUsageAggregateBy aggregateBy,
      @Nullable Integer page)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(utcDay(startDate), utcDay(endDate), aggregateBy, page);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(LocalDate, LocalDate,
   * String, Integer)} but takes the dates as {@link Instant} values, of which the UTC day is sent.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(
      Instant startDate, Instant endDate, @Nullable String aggregateBy, @Nullable Integer page)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(utcDay(startDate), utcDay(endDate), aggregateBy, page);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#getCustomTagUsageAnalytics(Instant, Instant,
   * CustomTagUsageAggregateBy, Integer)} but retrieves the first page aggregated over the whole
   * period.
   */
  public CustomTagUsageReport getCustomTagUsageAnalytics(Instant startDate, Instant endDate)
      throws DeepLException, InterruptedException {
    return getCustomTagUsageAnalytics(startDate, endDate, (String) null, null);
  }

  /**
   * Creates a developer key in the organization. The request is retried only when the API cannot
   * have processed it (HTTP 429, a DNS resolution failure, a refused connection or a connect
   * timeout), so that a lost response cannot lead to several keys being created. For the same
   * reason the request is sent in streaming mode, so it does not follow HTTP redirects: a redirect
   * fails with a {@link ConnectionException}.
   *
   * @param options Options for the new key, such as its label, or <code>null</code> to let the API
   *     use the label "DeepL API Key".
   * @return {@link DeveloperKey} describing the created key.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey createDeveloperKey(@Nullable CreateDeveloperKeyOptions options)
      throws DeepLException, InterruptedException {
    JsonObject requestData = new JsonObject();
    if (options != null && options.getLabel() != null) {
      requestData.addProperty("label", options.getLabel());
    }
    addExtraBodyParameters(requestData, options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(
            "/v2/admin/developer-keys", requestData.toString());
    checkResponse(response);
    return jsonParser.parseDeveloperKey(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLAdminClient#createDeveloperKey(CreateDeveloperKeyOptions)}
   * but uses the default label.
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
  public List<DeveloperKey> listDeveloperKeys() throws DeepLException, InterruptedException {
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff("/v2/admin/developer-keys");
    checkResponse(response);
    return jsonParser.parseDeveloperKeyList(response.getBody());
  }

  /**
   * Deactivates a developer key. Deactivation is permanent.
   *
   * @param keyId ID of the key to deactivate.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link DeveloperKey} describing the deactivated key.
   * @throws IllegalArgumentException If keyId is null or empty.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey deactivateDeveloperKey(
      String keyId, @Nullable DeactivateDeveloperKeyOptions options)
      throws DeepLException, InterruptedException {
    return putDeveloperKey("/v2/admin/developer-keys/deactivate", createKeyRequest(keyId), options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#deactivateDeveloperKey(String,
   * DeactivateDeveloperKeyOptions)} but accepts a {@link DeveloperKey}.
   */
  public DeveloperKey deactivateDeveloperKey(
      DeveloperKey key, @Nullable DeactivateDeveloperKeyOptions options)
      throws DeepLException, InterruptedException {
    return deactivateDeveloperKey(keyIdOf(key), options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#deactivateDeveloperKey(String,
   * DeactivateDeveloperKeyOptions)} but without options.
   */
  public DeveloperKey deactivateDeveloperKey(String keyId)
      throws DeepLException, InterruptedException {
    return deactivateDeveloperKey(keyId, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#deactivateDeveloperKey(DeveloperKey,
   * DeactivateDeveloperKeyOptions)} but without options.
   */
  public DeveloperKey deactivateDeveloperKey(DeveloperKey key)
      throws DeepLException, InterruptedException {
    return deactivateDeveloperKey(key, null);
  }

  /**
   * Changes the label of a developer key.
   *
   * @param keyId ID of the key to rename.
   * @param label New label for the key.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link DeveloperKey} describing the renamed key.
   * @throws IllegalArgumentException If keyId is null or empty.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey renameDeveloperKey(
      String keyId, String label, @Nullable RenameDeveloperKeyOptions options)
      throws DeepLException, InterruptedException {
    JsonObject requestData = createKeyRequest(keyId);
    requestData.addProperty("label", label);
    return putDeveloperKey("/v2/admin/developer-keys/label", requestData, options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#renameDeveloperKey(String, String,
   * RenameDeveloperKeyOptions)} but accepts a {@link DeveloperKey}.
   */
  public DeveloperKey renameDeveloperKey(
      DeveloperKey key, String label, @Nullable RenameDeveloperKeyOptions options)
      throws DeepLException, InterruptedException {
    return renameDeveloperKey(keyIdOf(key), label, options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#renameDeveloperKey(String, String,
   * RenameDeveloperKeyOptions)} but without options.
   */
  public DeveloperKey renameDeveloperKey(String keyId, String label)
      throws DeepLException, InterruptedException {
    return renameDeveloperKey(keyId, label, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#renameDeveloperKey(DeveloperKey, String,
   * RenameDeveloperKeyOptions)} but without options.
   */
  public DeveloperKey renameDeveloperKey(DeveloperKey key, String label)
      throws DeepLException, InterruptedException {
    return renameDeveloperKey(key, label, null);
  }

  /**
   * Changes the monthly usage limits of a developer key. Only the limits set in <code>limits</code>
   * are changed; the other limits keep their current value. A <code>null</code> limit means
   * unlimited and 0 blocks that usage.
   *
   * @param keyId ID of the key to modify.
   * @param limits Usage limits to change.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link DeveloperKey} describing the modified key.
   * @throws IllegalArgumentException If keyId is null or empty.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DeveloperKey setDeveloperKeyUsageLimits(
      String keyId,
      DeveloperKeyUsageLimitsUpdate limits,
      @Nullable SetDeveloperKeyUsageLimitsOptions options)
      throws DeepLException, InterruptedException {
    JsonObject requestData = createKeyRequest(keyId);
    if (limits != null) {
      limits.addTo(requestData);
    }
    return putDeveloperKey("/v2/admin/developer-keys/limits", requestData, options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#setDeveloperKeyUsageLimits(String,
   * DeveloperKeyUsageLimitsUpdate, SetDeveloperKeyUsageLimitsOptions)} but accepts a {@link
   * DeveloperKey}.
   */
  public DeveloperKey setDeveloperKeyUsageLimits(
      DeveloperKey key,
      DeveloperKeyUsageLimitsUpdate limits,
      @Nullable SetDeveloperKeyUsageLimitsOptions options)
      throws DeepLException, InterruptedException {
    return setDeveloperKeyUsageLimits(keyIdOf(key), limits, options);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#setDeveloperKeyUsageLimits(String,
   * DeveloperKeyUsageLimitsUpdate, SetDeveloperKeyUsageLimitsOptions)} but without options.
   */
  public DeveloperKey setDeveloperKeyUsageLimits(String keyId, DeveloperKeyUsageLimitsUpdate limits)
      throws DeepLException, InterruptedException {
    return setDeveloperKeyUsageLimits(keyId, limits, null);
  }

  /**
   * Functions the same as {@link DeepLAdminClient#setDeveloperKeyUsageLimits(DeveloperKey,
   * DeveloperKeyUsageLimitsUpdate, SetDeveloperKeyUsageLimitsOptions)} but without options.
   */
  public DeveloperKey setDeveloperKeyUsageLimits(
      DeveloperKey key, DeveloperKeyUsageLimitsUpdate limits)
      throws DeepLException, InterruptedException {
    return setDeveloperKeyUsageLimits(key, limits, null);
  }

  private DeveloperKey putDeveloperKey(
      String relativeUrl, JsonObject requestData, @Nullable BaseRequestOptions options)
      throws DeepLException, InterruptedException {
    addExtraBodyParameters(requestData, options);
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

  /** Adds the extra body parameters last, replacing fields with the same key. */
  private static void addExtraBodyParameters(
      JsonObject requestData, @Nullable BaseRequestOptions options) {
    if (options != null && options.getExtraBodyParameters() != null) {
      for (Map.Entry<String, String> entry : options.getExtraBodyParameters().entrySet()) {
        requestData.addProperty(entry.getKey(), entry.getValue());
      }
    }
  }

  private static @Nullable LocalDate utcDay(@Nullable Date date) {
    return date != null ? utcDay(date.toInstant()) : null;
  }

  private static @Nullable LocalDate utcDay(@Nullable Instant instant) {
    return instant != null ? instant.atZone(ZoneOffset.UTC).toLocalDate() : null;
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
