// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.util.*;
import org.jetbrains.annotations.*;

/** Information about a developer API key, as returned by the Admin API. */
public class DeveloperKey {
  @SerializedName(value = "key_id")
  private final String keyId;

  @SerializedName(value = "label")
  private final String label;

  @SerializedName(value = "creation_time")
  private final @Nullable Date creationTime;

  @SerializedName(value = "deactivated_time")
  private final @Nullable Date deactivatedTime;

  @SerializedName(value = "is_deactivated")
  private final boolean isDeactivated;

  @SerializedName(value = "usage_limits")
  private final @Nullable DeveloperKeyUsageLimits usageLimits;

  /**
   * Initializes a new {@link DeveloperKey} containing information about a developer key.
   *
   * @param keyId Unique ID of the key.
   * @param label Label assigned to the key.
   * @param creationTime Timestamp when the key was created.
   * @param deactivatedTime Timestamp when the key was deactivated, or <code>null</code> if active.
   * @param isDeactivated True if the key is deactivated.
   * @param usageLimits Usage limits configured for the key.
   */
  public DeveloperKey(
      String keyId,
      String label,
      @Nullable Date creationTime,
      @Nullable Date deactivatedTime,
      boolean isDeactivated,
      @Nullable DeveloperKeyUsageLimits usageLimits) {
    this.keyId = keyId;
    this.label = label;
    this.creationTime = creationTime;
    this.deactivatedTime = deactivatedTime;
    this.isDeactivated = isDeactivated;
    this.usageLimits = usageLimits;
  }

  /** @return Unique ID of the key. */
  public String getKeyId() {
    return keyId;
  }

  /** @return Label assigned to the key. */
  public String getLabel() {
    return label;
  }

  /** @return Timestamp when the key was created. */
  public @Nullable Date getCreationTime() {
    return creationTime;
  }

  /** @return Timestamp when the key was deactivated, or <code>null</code> if it is active. */
  public @Nullable Date getDeactivatedTime() {
    return deactivatedTime;
  }

  /** @return True if the key is deactivated. */
  public boolean isDeactivated() {
    return isDeactivated;
  }

  /** @return Usage limits configured for the key; a <code>null</code> limit means unlimited. */
  public DeveloperKeyUsageLimits getUsageLimits() {
    return usageLimits != null ? usageLimits : new DeveloperKeyUsageLimits(null, null, null);
  }

  @Override
  public String toString() {
    return "DeveloperKey \"" + label + "\" (" + keyId + ")";
  }
}
