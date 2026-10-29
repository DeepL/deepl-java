// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Status of a translation memory import or export job. */
public class TranslationMemoryJob {
  private final String jobId;
  private final String product;
  private final Operation operation;
  private final List<TranslationMemoryJobResult> results;
  private final Date creationTime;
  private final Date updatedTime;
  private final @Nullable String translationMemoryId;
  private final @Nullable String displayName;
  private final @Nullable String sourceContentType;
  private final @Nullable Long sourceContentLength;

  /** Operation a translation memory job performs. */
  public enum Operation {
    /** The job imports a TMX file into a new translation memory. */
    @SerializedName("import")
    Import,
    /** The job exports an existing translation memory to a TMX file. */
    @SerializedName("export")
    Export,
  }

  /**
   * Initializes a new {@link TranslationMemoryJob} describing an import or export job.
   *
   * @param jobId Unique ID assigned to the job.
   * @param product Product the job belongs to, always "translation_memory".
   * @param operation Operation the job performs.
   * @param results Results of the job; the API returns exactly one.
   * @param creationTime Timestamp when the job was created.
   * @param updatedTime Timestamp when the job was last updated.
   * @param translationMemoryId ID of the translation memory an export job reads from.
   * @param displayName Display name an import job assigns to the new translation memory.
   * @param sourceContentType MIME type declared for the file of an import job.
   * @param sourceContentLength Size in bytes declared for the file of an import job.
   */
  public TranslationMemoryJob(
      String jobId,
      String product,
      Operation operation,
      List<TranslationMemoryJobResult> results,
      Date creationTime,
      Date updatedTime,
      @Nullable String translationMemoryId,
      @Nullable String displayName,
      @Nullable String sourceContentType,
      @Nullable Long sourceContentLength) {
    this.jobId = jobId;
    this.product = product;
    this.operation = operation;
    this.results = results;
    this.creationTime = creationTime;
    this.updatedTime = updatedTime;
    this.translationMemoryId = translationMemoryId;
    this.displayName = displayName;
    this.sourceContentType = sourceContentType;
    this.sourceContentLength = sourceContentLength;
  }

  /** @return Unique ID assigned to the job. */
  public String getJobId() {
    return jobId;
  }

  /** @return Product the job belongs to, always "translation_memory". */
  public String getProduct() {
    return product;
  }

  /** @return Operation the job performs. */
  public Operation getOperation() {
    return operation;
  }

  /** @return Results of the job; the API returns exactly one. */
  public List<TranslationMemoryJobResult> getResults() {
    return results;
  }

  /** @return The single result of the job, or <code>null</code> if the API returned none. */
  public @Nullable TranslationMemoryJobResult getResult() {
    return getResults().isEmpty() ? null : getResults().get(0);
  }

  /** @return Status of the result of the job, or <code>null</code> if there is no result. */
  public @Nullable TranslationMemoryJobResult.Status getStatus() {
    TranslationMemoryJobResult result = getResult();
    return result == null ? null : result.getStatus();
  }

  /**
   * @return <code>true</code> if the job has finished, successfully or not, otherwise <code>false
   *     </code>.
   */
  public boolean done() {
    TranslationMemoryJobResult result = getResult();
    return result != null && result.done();
  }

  /** @return <code>false</code> if the job failed or expired, otherwise <code>true</code>. */
  public boolean ok() {
    TranslationMemoryJobResult result = getResult();
    return result == null || result.ok();
  }

  /** @return Timestamp when the job was created. */
  public Date getCreationTime() {
    return creationTime;
  }

  /** @return Timestamp when the job was last updated. */
  public Date getUpdatedTime() {
    return updatedTime;
  }

  /**
   * @return ID of the translation memory an export job reads from, or <code>null</code> for an
   *     import job.
   */
  public @Nullable String getTranslationMemoryId() {
    return translationMemoryId;
  }

  /**
   * @return Display name an import job assigns to the new translation memory, or <code>null</code>
   *     for an export job.
   */
  public @Nullable String getDisplayName() {
    return displayName;
  }

  /**
   * @return MIME type declared for the file of an import job, or <code>null</code> for an export
   *     job.
   */
  public @Nullable String getSourceContentType() {
    return sourceContentType;
  }

  /**
   * @return Size in bytes declared for the file of an import job, or <code>null</code> for an
   *     export job.
   */
  public @Nullable Long getSourceContentLength() {
    return sourceContentLength;
  }
}
