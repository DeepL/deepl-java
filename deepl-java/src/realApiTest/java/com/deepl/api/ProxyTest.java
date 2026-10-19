// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

class ProxyTest {
  @Test
  void sendsRequestsThroughTheConfiguredProxy() throws Exception {
    List<String> connectTargets = Collections.synchronizedList(new ArrayList<>());
    HttpProxyServer proxyServer =
        DefaultHttpProxyServer.bootstrap()
            .withAddress(new InetSocketAddress("127.0.0.1", 0))
            .withFiltersSource(
                new HttpFiltersSourceAdapter() {
                  @Override
                  public HttpFilters filterRequest(
                      HttpRequest request, ChannelHandlerContext context) {
                    if (request.method() == HttpMethod.CONNECT) {
                      connectTargets.add(request.uri());
                    }
                    return new HttpFiltersAdapter(request);
                  }
                })
            .start();
    try {
      DeepLClientOptions options = new DeepLClientOptions();
      options.setProxy(new Proxy(Proxy.Type.HTTP, proxyServer.getListenAddress()));

      Usage usage = RealApiClient.createClient(options).getUsage();

      assertTrue(usage.toString().contains("Usage this billing period"));
      URL serverUrl = new URL(RealApiClient.serverUrl());
      int port = serverUrl.getPort() == -1 ? 443 : serverUrl.getPort();
      assertEquals(Collections.singletonList(serverUrl.getHost() + ":" + port), connectTargets);
    } finally {
      proxyServer.stop();
    }
  }
}
