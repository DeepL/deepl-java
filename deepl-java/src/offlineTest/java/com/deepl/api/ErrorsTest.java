// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@SuppressWarnings("deprecation")
class ErrorsTest {
  @RegisterExtension static OfflineServer wm = new OfflineServer();

  private final Translator translator =
      OfflineClient.createTranslator(wm, new TranslatorOptions().setMaxRetries(0));

  private void respond(int status, String body) {
    wm.stubFor(get("/v2/usage").willReturn(aResponse().withStatus(status).withBody(body)));
  }

  @ParameterizedTest
  @CsvSource({
    "400,com.deepl.api.DeepLException,Bad request",
    "401,com.deepl.api.AuthorizationException,Authorization failure",
    "403,com.deepl.api.AuthorizationException,Authorization failure",
    "404,com.deepl.api.NotFoundException,Not found",
    "429,com.deepl.api.TooManyRequestsException,Too many requests",
    "456,com.deepl.api.QuotaExceededException,Quota for this billing period has been exceeded",
    "500,com.deepl.api.DeepLException,Unknown error",
    "503,com.deepl.api.DeepLException,Service unavailable",
  })
  void throwsTheExceptionOfTheStatusCode(
      int status, Class<? extends DeepLException> expected, String messagePrefix) {
    respond(status, "");
    DeepLException thrown = assertThrows(expected, translator::getUsage);
    assertEquals(expected, thrown.getClass());
    assertEquals(true, thrown.getMessage().startsWith(messagePrefix), thrown.getMessage());
  }

  @Test
  void throwsAuthorizationExceptionForHttp401OfTheAdminClient() {
    wm.stubFor(get("/v2/admin/developer-keys").willReturn(aResponse().withStatus(401)));
    DeepLAdminClient admin =
        OfflineClient.createAdminClient(wm, new DeepLAdminClientOptions().setMaxRetries(0));
    assertThrows(AuthorizationException.class, admin::getDeveloperKeys);
  }

  @Test
  void throwsWithTheMessageAndDetailOfAJsonErrorBody() {
    respond(400, "{\"message\":\"Value for 'target_lang' not supported.\",\"detail\":\"Try de\"}");
    DeepLException thrown = assertThrows(DeepLException.class, translator::getUsage);
    assertEquals(
        "Bad request, error message: message: Value for 'target_lang' not supported., detail: Try de",
        thrown.getMessage());
  }

  @Test
  void throwsWithTheRawBodyWhenTheErrorBodyIsNotJson() {
    respond(500, "<html>Bad gateway</html>");
    DeepLException thrown = assertThrows(DeepLException.class, translator::getUsage);
    assertEquals("Unknown error, response: <html>Bad gateway</html>", thrown.getMessage());
  }

  @Test
  void throwsWithoutSuffixWhenTheErrorBodyIsEmpty() {
    respond(456, "");
    QuotaExceededException thrown =
        assertThrows(QuotaExceededException.class, translator::getUsage);
    assertEquals("Quota for this billing period has been exceeded", thrown.getMessage());
  }

  @Test
  void throwsGlossaryNotFoundOnlyForGlossaryRequests() {
    wm.stubFor(get("/v2/glossaries/g1").willReturn(aResponse().withStatus(404)));
    assertThrows(GlossaryNotFoundException.class, () -> translator.getGlossary("g1"));
  }
}
