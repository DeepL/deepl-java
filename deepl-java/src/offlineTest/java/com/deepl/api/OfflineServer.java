// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A local WireMock server that stands in for the DeepL API. A request that matches no stub, and a
 * stub that no request matched, both fail the test.
 */
class OfflineServer extends WireMockExtension {
  OfflineServer() {
    super(WireMockExtension.extensionOptions().failOnUnmatchedRequests(true));
  }

  @Override
  protected void onAfterEach(WireMockRuntimeInfo info) {
    Set<UUID> matched = new HashSet<>();
    for (ServeEvent event : getAllServeEvents()) {
      if (event.getWasMatched()) {
        matched.add(event.getStubMapping().getId());
      }
    }
    List<String> unsent =
        getStubMappings().stream()
            .filter(stub -> !matched.contains(stub.getId()))
            .map(OfflineServer::describe)
            .collect(Collectors.toList());
    if (!unsent.isEmpty()) {
      throw new AssertionError("Declared requests were not sent: " + unsent);
    }
  }

  private static String describe(StubMapping stub) {
    return stub.getRequest().toString().replaceAll("\\s+", " ");
  }
}
