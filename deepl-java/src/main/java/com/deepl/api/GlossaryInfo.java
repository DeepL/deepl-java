// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.*;
import java.util.*;
import org.jetbrains.annotations.*;

/** Information about a glossary, excluding the entry list. */
public class GlossaryInfo implements IGlossary {

  @SerializedName(value = "glossary_id")
  private final String glossaryId;

  @SerializedName(value = "name")
  private final String name;

  @SerializedName(value = "ready")
  private final boolean ready;

  @SerializedName(value = "source_lang")
  private final String sourceLang;

  @SerializedName(value = "target_lang")
  private final String targetLang;

  @SerializedName(value = "creation_time")
  private final Date creationTime;

  @SerializedName(value = "entry_count")
  private final long entryCount;

  /**
   * Initializes a new {@link GlossaryInfo} containing information about a glossary.
   *
   * @param glossaryId ID of the associated glossary.
   * @param name Name of the glossary chosen during creation.
   * @param ready {@code true} if the glossary may be used for translations, otherwise {@code
   *     false}.
   * @param sourceLang Language code of the source terms in the glossary.
   * @param targetLang Language code of the target terms in the glossary.
   * @param creationTime Time when the glossary was created.
   * @param entryCount The number of source-target entry pairs in the glossary.
   */
  public GlossaryInfo(
      String glossaryId,
      String name,
      boolean ready,
      String sourceLang,
      String targetLang,
      Date creationTime,
      long entryCount) {
    this.glossaryId = glossaryId;
    this.name = name;
    this.ready = ready;
    this.sourceLang = sourceLang;
    this.targetLang = targetLang;
    this.creationTime = creationTime;
    this.entryCount = entryCount;
  }

  /** @return Unique ID assigned to the glossary. */
  public String getGlossaryId() {
    return glossaryId;
  }

  /** @return User-defined name assigned to the glossary. */
  public String getName() {
    return name;
  }

  /** @return True if the glossary may be used for translations, otherwise false. */
  public boolean isReady() {
    return ready;
  }

  /** @return Source language code of the glossary. */
  public String getSourceLang() {
    return sourceLang;
  }

  /** @return Target language code of the glossary. */
  public String getTargetLang() {
    return targetLang;
  }

  /** @return Timestamp when the glossary was created. */
  public Date getCreationTime() {
    return creationTime;
  }

  /** @return The number of entries contained in the glossary. */
  public long getEntryCount() {
    return entryCount;
  }
}
