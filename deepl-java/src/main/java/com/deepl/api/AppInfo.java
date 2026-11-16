// Copyright 2023 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Objects;

public class AppInfo {
  private String appName;
  private String appVersion;

  /**
   * Initializes a new {@link AppInfo} object.
   *
   * @param appName Name of the program/plugin that uses this Client Library.
   * @param appVersion Version of the program/plugin that uses this Client Library.
   * @throws NullPointerException If appName or appVersion is <code>null</code>.
   */
  public AppInfo(String appName, String appVersion) {
    this.appName = Objects.requireNonNull(appName, "appName");
    this.appVersion = Objects.requireNonNull(appVersion, "appVersion");
  }

  public String getAppName() {
    return appName;
  }

  public String getAppVersion() {
    return appVersion;
  }
}
