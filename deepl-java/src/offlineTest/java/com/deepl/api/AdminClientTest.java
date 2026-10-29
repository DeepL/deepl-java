// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AdminClientTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private static final String KEY_ID =
      "ca7d5694-96eb-4263-a9a4-7f7e4211529e:20c2abcf-4c3c-4cd6-8ae8-8bd2a7d4da38";
  private static final String DEVELOPER_KEY =
      "{\"key_id\":\""
          + KEY_ID
          + "\",\"label\":\"developer key prod\",\"creation_time\":\"2025-07-08T08:15:29.362Z\","
          + "\"deactivated_time\":null,\"is_deactivated\":false,"
          + "\"usage_limits\":{\"characters\":5000,\"speech_to_text_milliseconds\":null,"
          + "\"speech_to_speech_milliseconds\":60000}}";

  private final DeepLAdminClient client = OfflineClient.createAdminClient(wm);

  @Test
  void rejectsAnEmptyAdminKey() {
    assertThrows(IllegalArgumentException.class, () -> new DeepLAdminClient(""));
    assertThrows(IllegalArgumentException.class, () -> new DeepLAdminClient(" "));
    assertThrows(
        IllegalArgumentException.class, () -> new DeepLAdminClient(OfflineClient.ADMIN_KEY, null));
  }

  @Test
  void rejectsMissingDatesWithoutSendingThem() {
    LocalDate date = LocalDate.of(2025, 10, 1);
    assertThrows(IllegalArgumentException.class, () -> client.getUsageAnalytics(null, date));
    assertThrows(
        IllegalArgumentException.class, () -> client.getCustomTagUsageAnalytics(date, null));
  }

  @Test
  void leavesInvalidDateRangesToTheApi() {
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2025-10-01&end_date=2025-10-01")
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withBody("{\"message\":\"start_date must be before end_date\"}")));
    LocalDate date = LocalDate.of(2025, 10, 1);
    DeepLException thrown =
        assertThrows(DeepLException.class, () -> client.getUsageAnalytics(date, date));
    assertTrue(thrown.getMessage().contains("must be before"), thrown.getMessage());
  }

  @Test
  void parsesMalformedValuesTolerantly() throws Exception {
    wm.stubFor(
        get("/v2/admin/developer-keys")
            .willReturn(
                okJson(
                    "[1,null,{},{\"key_id\":\"\"},{\"key_id\":\"k\",\"label\":\"l\","
                        + "\"creation_time\":\"not a timestamp\",\"is_deactivated\":true,"
                        + "\"usage_limits\":\"x\"}]")));
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2025-09-29&end_date=2025-10-01&group_by=key")
            .willReturn(
                okJson(
                    "{\"usage_report\":{\"total_usage\":{\"total_characters\":\"many\","
                        + "\"speech_to_text_minutes\":{},\"speech_to_speech_minutes\":2.5},"
                        + "\"start_date\":\"garbage\","
                        + "\"end_date\":\"2025-10-01T00:00:00\",\"key_usages\":[7,{},"
                        + "{\"api_key\":\"a\",\"usage\":{\"total_characters\":5}}]}}")));
    wm.stubFor(
        get("/v2/admin/analytics/custom-tags?start_date=2025-09-29&end_date=2025-10-01")
            .willReturn(
                okJson(
                    "{\"custom_tag_usage_report\":{\"next_page\":null,\"usage\":[\"x\","
                        + "{\"custom_tag\":\"t\",\"usage_date\":\"bad\","
                        + "\"breakdown\":{\"total_characters\":4}}]}}")));

    List<DeveloperKey> keys = client.listDeveloperKeys();
    assertEquals(1, keys.size());
    assertEquals("k", keys.get(0).getKeyId());
    assertNull(keys.get(0).getCreationTime());
    assertTrue(keys.get(0).isDeactivated());
    assertNull(keys.get(0).getUsageLimits());

    LocalDate start = LocalDate.of(2025, 9, 29);
    LocalDate end = LocalDate.of(2025, 10, 1);
    AdminUsageReport report = client.getUsageAnalytics(start, end, AdminUsageGroupBy.Key);
    assertEquals(0, report.getTotalUsage().getTotalCharacters());
    assertEquals(0, report.getTotalUsage().getSpeechToTextMinutes());
    assertEquals(2.5, report.getTotalUsage().getSpeechToSpeechMinutes());
    assertEquals(0, report.getKeyUsages().get(1).getUsage().getSpeechToSpeechMinutes());
    assertNull(report.getStartDate());
    assertEquals(utcMidnight("2025-10-01"), report.getEndDate());
    assertEquals(2, report.getKeyUsages().size());
    assertEquals(5, report.getKeyUsages().get(1).getUsage().getTotalCharacters());

    CustomTagUsageReport tagReport = client.getCustomTagUsageAnalytics(start, end);
    assertNull(tagReport.getNextPage());
    assertEquals(1, tagReport.getUsage().size());
    assertNull(tagReport.getUsage().get(0).getUsageDate());
    assertEquals(4, tagReport.getUsage().get(0).getBreakdown().getTotalCharacters());
  }

  @Test
  void throwsDeepLExceptionIfAKeyResponseHasNoKeyId() {
    wm.stubFor(post("/v2/admin/developer-keys").willReturn(okJson("{\"label\":\"l\"}")));
    assertThrows(DeepLException.class, client::createDeveloperKey);
    wm.stubFor(post("/v2/admin/developer-keys").willReturn(okJson("{\"key_id\":\"\"}")));
    assertThrows(DeepLException.class, client::createDeveloperKey);
    wm.stubFor(post("/v2/admin/developer-keys").willReturn(okJson("[]")));
    assertThrows(DeepLException.class, client::createDeveloperKey);
    wm.stubFor(post("/v2/admin/developer-keys").willReturn(aResponse().withStatus(200)));
    assertThrows(DeepLException.class, client::createDeveloperKey);
  }

  @Test
  void throwsDeepLExceptionIfTheListOrAnalyticsAreOfTheWrongShape() {
    wm.stubFor(get("/v2/admin/developer-keys").willReturn(okJson("{\"keys\":[]}")));
    assertThrows(DeepLException.class, client::listDeveloperKeys);
    wm.stubFor(get(urlPathEqualTo("/v2/admin/analytics")).willReturn(okJson("[]")));
    assertThrows(
        DeepLException.class,
        () -> client.getUsageAnalytics(LocalDate.of(2025, 9, 29), LocalDate.of(2025, 10, 1)));
  }

  @Test
  void readsIsDeactivatedOnlyFromJsonTrue() throws Exception {
    wm.stubFor(
        get("/v2/admin/developer-keys")
            .willReturn(
                okJson(
                    "["
                        + DEVELOPER_KEY.replace(
                            "\"is_deactivated\":false", "\"is_deactivated\":\"true\"")
                        + ","
                        + DEVELOPER_KEY.replace(
                            "\"is_deactivated\":false", "\"is_deactivated\":null")
                        + ","
                        + DEVELOPER_KEY.replace("\"is_deactivated\":false", "\"is_deactivated\":1")
                        + "]")));
    for (DeveloperKey key : client.listDeveloperKeys()) {
      assertFalse(key.isDeactivated());
    }
  }

  @Test
  void dropsANextPageOutsideTheIntRange() throws Exception {
    wm.stubFor(
        get(urlPathEqualTo("/v2/admin/analytics/custom-tags"))
            .willReturn(okJson("{\"custom_tag_usage_report\":{\"next_page\":4294967297}}")));
    CustomTagUsageReport report =
        client.getCustomTagUsageAnalytics(LocalDate.of(2025, 9, 29), LocalDate.of(2025, 10, 1));
    assertNull(report.getNextPage());
  }

  @Test
  void passesLabelAndLimitValuesThroughToTheApi() throws Exception {
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .withRequestBody(equalToJson("{\"label\":\"\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    client.createDeveloperKey(new CreateDeveloperKeyOptions().setLabel(""));
    wm.stubFor(
        put("/v2/admin/developer-keys/limits")
            .withRequestBody(equalToJson("{\"key_id\":\"" + KEY_ID + "\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    client.setDeveloperKeyUsageLimits(KEY_ID, new DeveloperKeyUsageLimitsUpdate());
  }

  @Test
  void throwsDeepLExceptionIfTheResponseIsNotJson() {
    wm.stubFor(get("/v2/admin/developer-keys").willReturn(okJson("{not json")));
    assertThrows(DeepLException.class, client::listDeveloperKeys);
  }

  @Test
  void acceptsALongDateRangeAndLeavesItToTheApi() throws Exception {
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2024-01-01&end_date=2026-01-01")
            .willReturn(okJson("{\"usage_report\":{}}")));
    client.getUsageAnalytics(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1));
  }

  @Test
  void acceptsIsoStringDatesForBothAnalyticsMethods() throws Exception {
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2026-10-19&end_date=2026-10-20&group_by=key")
            .willReturn(okJson("{\"usage_report\":{}}")));
    wm.stubFor(
        get("/v2/admin/analytics/custom-tags?start_date=2026-10-19&end_date=2026-10-20")
            .willReturn(okJson("{\"custom_tag_usage_report\":{}}")));
    client.getUsageAnalytics("2026-10-19", "2026-10-20", AdminUsageGroupBy.Key);
    client.getCustomTagUsageAnalytics("2026-10-19", "2026-10-20");
  }

  @Test
  void sendsTheUtcDayOfAnInstantConvertedByTheCaller() throws Exception {
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2026-10-19&end_date=2026-10-21")
            .willReturn(okJson("{\"usage_report\":{}}")));
    LocalDate start = Instant.parse("2026-10-19T22:00:00Z").atZone(ZoneOffset.UTC).toLocalDate();
    client.getUsageAnalytics(start, LocalDate.of(2026, 10, 21));
  }

  @Test
  void rejectsInvalidStringDatesWithoutSendingThem() {
    for (String invalid : new String[] {"2026-10-1", "2026-02-30", "20261019", "", "x"}) {
      assertThrows(
          IllegalArgumentException.class, () -> client.getUsageAnalytics(invalid, "2026-10-20"));
      assertThrows(
          IllegalArgumentException.class,
          () -> client.getCustomTagUsageAnalytics("2026-10-19", invalid));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> client.getUsageAnalytics((String) null, "2026-10-20"));
    assertEquals(0, wm.getAllServeEvents().size());
  }

  @Test
  void rejectsAnEmptyKeyIdWithoutSendingIt() {
    assertThrows(IllegalArgumentException.class, () -> client.deactivateDeveloperKey(""));
    assertThrows(
        IllegalArgumentException.class, () -> client.deactivateDeveloperKey((DeveloperKey) null));
    assertThrows(IllegalArgumentException.class, () -> client.renameDeveloperKey("", "label"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            client.setDeveloperKeyUsageLimits(
                "", new DeveloperKeyUsageLimitsUpdate().setCharacters(1L)));
  }

  @Test
  void usesTheProServerByDefault() throws Exception {
    Field wrapperField = DeepLAdminClient.class.getDeclaredField("httpClientWrapper");
    wrapperField.setAccessible(true);
    Field serverUrlField = HttpClientWrapper.class.getDeclaredField("serverUrl");
    serverUrlField.setAccessible(true);
    Object wrapper = wrapperField.get(new DeepLAdminClient(OfflineClient.ADMIN_KEY));
    assertEquals("https://api.deepl.com", serverUrlField.get(wrapper));
  }

  @Test
  void sendsTheAdminKeyAndUserAgent() throws Exception {
    wm.stubFor(
        get("/v2/admin/developer-keys")
            .withHeader("Authorization", equalTo("DeepL-Auth-Key " + OfflineClient.ADMIN_KEY))
            .withHeader("User-Agent", matching("deepl-java/.*"))
            .willReturn(okJson("[]")));
    assertTrue(client.listDeveloperKeys().isEmpty());
  }

  @Test
  void getsUsageAnalyticsAndParsesTheDates() throws Exception {
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2025-09-29&end_date=2025-10-01&group_by=key_and_day")
            .willReturn(
                okJson(
                    "{\"usage_report\":{\"total_usage\":{\"total_characters\":9619},"
                        + "\"group_by\":\"key_and_day\",\"start_date\":\"2025-09-29T00:00:00\","
                        + "\"end_date\":\"2025-10-01T00:00:00\",\"key_and_day_usages\":[{"
                        + "\"api_key\":\"dc88****3a2c\",\"api_key_label\":\"Staging API Key\","
                        + "\"usage_date\":\"2025-09-29T00:00:00Z\","
                        + "\"usage\":{\"total_characters\":315}}]}}")));
    AdminUsageReport report =
        client.getUsageAnalytics(
            LocalDate.of(2025, 9, 29), LocalDate.of(2025, 10, 1), AdminUsageGroupBy.KeyAndDay);

    assertEquals(utcMidnight("2025-09-29"), report.getStartDate());
    assertEquals(utcMidnight("2025-10-01"), report.getEndDate());
    assertTrue(report.getKeyUsages().isEmpty());
    assertEquals(utcMidnight("2025-09-29"), report.getKeyAndDayUsages().get(0).getUsageDate());
  }

  @Test
  void getsCustomTagUsageAnalytics() throws Exception {
    wm.stubFor(
        get("/v2/admin/analytics/custom-tags"
                + "?start_date=2026-05-03&end_date=2026-05-15&aggregate_by=day&page=2")
            .willReturn(
                okJson(
                    "{\"custom_tag_usage_report\":{\"aggregate_by\":\"day\","
                        + "\"start_date\":\"2026-05-03T00:00:00\","
                        + "\"end_date\":\"2026-05-15T00:00:00\",\"next_page\":3,\"usage\":[{"
                        + "\"custom_tag\":\"t\",\"usage_date\":\"2026-05-04T00:00:00Z\","
                        + "\"breakdown\":{\"total_characters\":380}}]}}")));
    CustomTagUsageReport report =
        client.getCustomTagUsageAnalytics(
            LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 15), CustomTagUsageAggregateBy.Day, 2);

    assertEquals(3, report.getNextPage());
    assertEquals(utcMidnight("2026-05-03"), report.getStartDate());
    assertEquals(utcMidnight("2026-05-15"), report.getEndDate());
    assertEquals(utcMidnight("2026-05-04"), report.getUsage().get(0).getUsageDate());
  }

  @Test
  void createsADeveloperKey() throws Exception {
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .withRequestBody(equalToJson("{\"label\":\"developer key prod\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    DeveloperKey key =
        client.createDeveloperKey(new CreateDeveloperKeyOptions().setLabel("developer key prod"));

    assertEquals("developer key prod", key.getLabel());

    assertEquals(Date.from(Instant.parse("2025-07-08T08:15:29.362Z")), key.getCreationTime());
    assertNull(key.getDeactivatedTime());
    assertFalse(key.isDeactivated());
    assertEquals(5000L, key.getUsageLimits().getCharacters());
    assertNull(key.getUsageLimits().getSpeechToTextMilliseconds());
    assertEquals(60000L, key.getUsageLimits().getSpeechToSpeechMilliseconds());
  }

  @Test
  void parsesADeveloperKeyTimestampWithoutATimeZoneAsUtc() throws Exception {
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .willReturn(
                okJson(DEVELOPER_KEY.replace("2025-07-08T08:15:29.362Z", "2025-07-08T08:15:29"))));
    DeveloperKey key = client.createDeveloperKey();

    assertEquals(Date.from(Instant.parse("2025-07-08T08:15:29Z")), key.getCreationTime());
  }

  @Test
  void retriesKeyCreationAfterTooManyRequests() throws Exception {
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .inScenario("429")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("recovered")
            .willReturn(aResponse().withStatus(429)));
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .inScenario("429")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(DEVELOPER_KEY)));
    client.createDeveloperKey();
    wm.verify(2, postRequestedFor(urlEqualTo("/v2/admin/developer-keys")));
  }

  @Test
  void doesNotRetryKeyCreationAfterAServerError() {
    wm.stubFor(post("/v2/admin/developer-keys").willReturn(aResponse().withStatus(503)));
    assertThrows(DeepLException.class, client::createDeveloperKey);
    wm.verify(1, postRequestedFor(urlEqualTo("/v2/admin/developer-keys")));
  }

  @Test
  void retriesKeyCreationAfterARefusedConnection() throws Exception {
    int closedPort;
    try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    DeepLAdminClient refusedClient =
        new DeepLAdminClient(
            "k",
            new DeepLAdminClientOptions()
                .setServerUrl("http://127.0.0.1:" + closedPort)
                .setMaxRetries(1));
    long start = System.nanoTime();
    ConnectionException thrown =
        assertThrows(ConnectionException.class, refusedClient::createDeveloperKey);
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
    assertTrue(thrown.getCause() instanceof java.net.ConnectException, thrown.toString());
    // The first backoff is 1 s minus at most 23 % jitter: only a retry takes this long.
    assertTrue(elapsedMillis >= 700, "no retry after " + elapsedMillis + " ms");
  }

  @Test
  void doesNotRetryKeyCreationAfterATimeoutOrConnectionReset() {
    DeepLAdminClient timeoutClient =
        OfflineClient.createAdminClient(
            wm, new DeepLAdminClientOptions().setTimeout(Duration.ofMillis(200)));
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .withRequestBody(equalToJson("{\"label\":\"slow\"}"))
            .willReturn(okJson(DEVELOPER_KEY).withFixedDelay(2500)));
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .withRequestBody(equalToJson("{\"label\":\"reset\"}"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    assertThrows(
        ConnectionException.class,
        () -> timeoutClient.createDeveloperKey(new CreateDeveloperKeyOptions().setLabel("slow")));
    assertThrows(
        ConnectionException.class,
        () -> timeoutClient.createDeveloperKey(new CreateDeveloperKeyOptions().setLabel("reset")));
    wm.verify(2, postRequestedFor(urlEqualTo("/v2/admin/developer-keys")));
  }

  @Test
  void retriesOtherRequestsAfterAServerError() throws Exception {
    wm.stubFor(
        put("/v2/admin/developer-keys/deactivate")
            .inScenario("503")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("recovered")
            .willReturn(aResponse().withStatus(503)));
    wm.stubFor(
        put("/v2/admin/developer-keys/deactivate")
            .inScenario("503")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(DEVELOPER_KEY)));
    client.deactivateDeveloperKey(KEY_ID);
    wm.verify(2, putRequestedFor(urlEqualTo("/v2/admin/developer-keys/deactivate")));
  }

  @Test
  void parsesDeactivatedKeys() throws Exception {
    String deactivated =
        DEVELOPER_KEY
            .replace("\"is_deactivated\":false", "\"is_deactivated\":true")
            .replace("\"deactivated_time\":null", "\"deactivated_time\":\"2025-07-09T08:15:29\"");
    wm.stubFor(get("/v2/admin/developer-keys").willReturn(okJson("[" + deactivated + "]")));
    List<DeveloperKey> keys = client.listDeveloperKeys();

    assertTrue(keys.get(0).isDeactivated());
    assertEquals(
        Date.from(Instant.parse("2025-07-09T08:15:29Z")), keys.get(0).getDeactivatedTime());
  }

  @Test
  void deactivatesADeveloperKey() throws Exception {
    wm.stubFor(
        put("/v2/admin/developer-keys/deactivate")
            .withRequestBody(equalToJson("{\"key_id\":\"" + KEY_ID + "\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    client.deactivateDeveloperKey(new DeveloperKey(KEY_ID, "label", null, null, false, null));
  }

  @Test
  void renamesADeveloperKey() throws Exception {
    wm.stubFor(
        put("/v2/admin/developer-keys/label")
            .withRequestBody(equalToJson("{\"key_id\":\"" + KEY_ID + "\",\"label\":\"renamed\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    client.renameDeveloperKey(KEY_ID, "renamed");
  }

  @Test
  void sendsOnlyTheUsageLimitsThatWereSet() throws Exception {
    wm.stubFor(
        put("/v2/admin/developer-keys/limits")
            .withRequestBody(
                equalToJson(
                    "{\"key_id\":\""
                        + KEY_ID
                        + "\",\"characters\":0,\"speech_to_speech_milliseconds\":null}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    client.setDeveloperKeyUsageLimits(
        KEY_ID,
        new DeveloperKeyUsageLimitsUpdate().setCharacters(0L).setSpeechToSpeechMilliseconds(null));
  }

  @Test
  void throwsAuthorizationExceptionOnForbidden() {
    wm.stubFor(
        get("/v2/admin/developer-keys")
            .willReturn(aResponse().withStatus(403).withBody("{\"message\":\"Forbidden\"}")));
    AuthorizationException thrown =
        assertThrows(AuthorizationException.class, client::listDeveloperKeys);
    assertTrue(thrown.getMessage().contains("adminKey"), thrown.getMessage());
  }

  @Test
  void sendsExtraBodyParametersLastOnEveryKeyRequest() throws Exception {
    wm.stubFor(
        post("/v2/admin/developer-keys")
            .withRequestBody(equalToJson("{\"label\":\"extra\",\"beta\":\"on\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    wm.stubFor(
        put("/v2/admin/developer-keys/deactivate")
            .withRequestBody(equalToJson("{\"key_id\":\"" + KEY_ID + "\",\"beta\":\"on\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    wm.stubFor(
        put("/v2/admin/developer-keys/label")
            .withRequestBody(equalToJson("{\"key_id\":\"" + KEY_ID + "\",\"label\":\"extra\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    wm.stubFor(
        put("/v2/admin/developer-keys/limits")
            .withRequestBody(
                equalToJson("{\"key_id\":\"" + KEY_ID + "\",\"characters\":\"7\",\"beta\":\"on\"}"))
            .willReturn(okJson(DEVELOPER_KEY)));
    Map<String, String> beta = Collections.singletonMap("beta", "on");

    client.createDeveloperKey(
        new CreateDeveloperKeyOptions()
            .setLabel("replaced")
            .setExtraBodyParameters(
                new HashMap<String, String>() {
                  {
                    put("label", "extra");
                    put("beta", "on");
                  }
                }));
    client.deactivateDeveloperKey(
        KEY_ID, new DeactivateDeveloperKeyOptions().setExtraBodyParameters(beta));
    client.renameDeveloperKey(
        KEY_ID,
        "replaced",
        new RenameDeveloperKeyOptions()
            .setExtraBodyParameters(Collections.singletonMap("label", "extra")));
    client.setDeveloperKeyUsageLimits(
        new DeveloperKey(KEY_ID, null, null, null, false, null),
        new DeveloperKeyUsageLimitsUpdate().setCharacters(1L),
        new SetDeveloperKeyUsageLimitsOptions()
            .setExtraBodyParameters(
                new HashMap<String, String>() {
                  {
                    put("characters", "7");
                    put("beta", "on");
                  }
                }));
    assertEquals(4, wm.getAllServeEvents().size());
  }

  @Test
  void sendsStringGroupByAndAggregateByAsGiven() throws Exception {
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2026-10-19&end_date=2026-10-21&group_by=key_and_week")
            .willReturn(okJson("{\"usage_report\":{}}")));
    wm.stubFor(
        get("/v2/admin/analytics/custom-tags"
                + "?start_date=2026-10-19&end_date=2026-10-21&aggregate_by=week&page=4")
            .willReturn(okJson("{\"custom_tag_usage_report\":{}}")));
    client.getUsageAnalytics("2026-10-19", "2026-10-21", "key_and_week");
    client.getUsageAnalytics(
        LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21), "key_and_week");
    client.getCustomTagUsageAnalytics("2026-10-19", "2026-10-21", "week", 4);
    client.getCustomTagUsageAnalytics(
        LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21), "week", 4);
    assertEquals(4, wm.getAllServeEvents().size());
  }

  @Test
  void sendsTheUtcDayOfDateAndInstantArguments() throws Exception {
    wm.stubFor(
        get("/v2/admin/analytics?start_date=2026-10-19&end_date=2026-10-21&group_by=key")
            .willReturn(okJson("{\"usage_report\":{}}")));
    wm.stubFor(
        get("/v2/admin/analytics/custom-tags?start_date=2026-10-19&end_date=2026-10-21&page=2")
            .willReturn(okJson("{\"custom_tag_usage_report\":{}}")));
    // 2026-10-20T00:30+02:00 is still 2026-10-19 in UTC.
    Instant start = OffsetDateTime.parse("2026-10-20T00:30:00+02:00").toInstant();
    Instant end = Instant.parse("2026-10-21T23:59:59Z");

    client.getUsageAnalytics(start, end, AdminUsageGroupBy.Key);
    client.getUsageAnalytics(Date.from(start), Date.from(end), "key");
    client.getCustomTagUsageAnalytics(start, end, (String) null, 2);
    client.getCustomTagUsageAnalytics(
        Date.from(start), Date.from(end), (CustomTagUsageAggregateBy) null, 2);
    assertEquals(4, wm.getAllServeEvents().size());
    assertThrows(
        IllegalArgumentException.class, () -> client.getUsageAnalytics((Date) null, new Date()));
  }

  @Test
  void returnsNullForAbsentLabelsAndUsageLimits() throws Exception {
    wm.stubFor(
        get("/v2/admin/developer-keys")
            .willReturn(okJson("[{\"key_id\":\"k\",\"is_deactivated\":false}]")));
    wm.stubFor(
        get(urlPathEqualTo("/v2/admin/analytics"))
            .willReturn(
                okJson(
                    "{\"usage_report\":{\"key_usages\":[{\"api_key\":\"a\","
                        + "\"usage\":{\"total_characters\":1}}]}}")));

    DeveloperKey key = client.listDeveloperKeys().get(0);
    assertNull(key.getLabel());
    assertNull(key.getUsageLimits());
    KeyUsage usage =
        client
            .getUsageAnalytics(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21))
            .getKeyUsages()
            .get(0);
    assertNull(usage.getApiKeyLabel());
  }

  @Test
  void parsesReportDatesAsUtcMidnight() throws Exception {
    wm.stubFor(
        get(urlPathEqualTo("/v2/admin/analytics"))
            .willReturn(
                okJson(
                    "{\"usage_report\":{\"start_date\":\"2025-09-29T00:00:00+02:00\","
                        + "\"end_date\":\"2025-10-01\"}}")));
    AdminUsageReport report =
        client.getUsageAnalytics(LocalDate.of(2025, 9, 29), LocalDate.of(2025, 10, 1));
    assertEquals(utcMidnight("2025-09-28"), report.getStartDate());
    assertEquals(utcMidnight("2025-10-01"), report.getEndDate());
  }

  private static Date utcMidnight(String day) {
    return Date.from(Instant.parse(day + "T00:00:00Z"));
  }
}
