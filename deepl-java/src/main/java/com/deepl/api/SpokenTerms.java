// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Information about a Spoken Terms collection: its ID, name, creation time and the term lists it
 * contains. Does not contain the terms themselves, see {@link
 * DeepLClient#getSpokenTermsEntries(String, String)}.
 */
public class SpokenTerms {
  @SerializedName(value = "spoken_terms_id")
  private final String spokenTermsId;

  @SerializedName(value = "name")
  private final String name;

  @SerializedName(value = "creation_time")
  private final Date creationTime;

  @SerializedName(value = "term_lists")
  private final @Nullable List<SpokenTermsList> termLists;

  /**
   * Initializes a new {@link SpokenTerms} object.
   *
   * @param spokenTermsId ID of the Spoken Terms collection.
   * @param name Name of the Spoken Terms collection.
   * @param creationTime Time the Spoken Terms collection was created.
   * @param termLists Term lists contained in the Spoken Terms collection.
   */
  public SpokenTerms(
      String spokenTermsId, String name, Date creationTime, List<SpokenTermsList> termLists) {
    this.spokenTermsId = spokenTermsId;
    this.name = name;
    this.creationTime = creationTime;
    this.termLists = termLists;
  }

  /** @return ID of the Spoken Terms collection. */
  public String getSpokenTermsId() {
    return spokenTermsId;
  }

  /** @return Name of the Spoken Terms collection. */
  public String getName() {
    return name;
  }

  /** @return Time the Spoken Terms collection was created. */
  public Date getCreationTime() {
    return creationTime;
  }

  /** @return Term lists contained in the Spoken Terms collection, one per language. */
  public List<SpokenTermsList> getTermLists() {
    return termLists != null ? termLists : Collections.emptyList();
  }

  @Override
  public String toString() {
    return "SpokenTerms{spokenTermsId='"
        + spokenTermsId
        + "', name='"
        + name
        + "', creationTime="
        + creationTime
        + ", termLists="
        + getTermLists()
        + '}';
  }
}
