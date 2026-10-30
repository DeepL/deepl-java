// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import org.jetbrains.annotations.Nullable;

/** The outcome of a translation memory import or export job. */
public class TranslationMemoryJobResult {
  private final @Nullable Status status;
  private final @Nullable String statusString;
  private final @Nullable String requiredAction;
  private final @Nullable String downloadUrl;
  private final @Nullable Date expiresAt;
  private final @Nullable String errorMessage;
  private final @Nullable String translationMemoryId;
  private final @Nullable Integer skippedSegmentCount;

  /** Status of a translation memory import or export job. */
  public enum Status {
    /** The job is waiting for the caller, for example to upload the TMX file of an import. */
    @SerializedName("awaiting_input")
    AwaitingInput,
    /** The job is being processed. */
    @SerializedName("processing")
    Processing,
    /** The job completed successfully. */
    @SerializedName("completed")
    Completed,
    /** The exported file of a completed export job has been downloaded. */
    @SerializedName("downloaded")
    Downloaded,
    /** An error occurred while processing the job. */
    @SerializedName("failed")
    Failed,
    /** The job expired before it finished. */
    @SerializedName("expired")
    Expired,
  }

  /**
   * Initializes a new {@link TranslationMemoryJobResult} describing the outcome of a job.
   *
   * @param status Status of the job.
   * @param requiredAction Action the caller must take, set while the job is waiting on the caller.
   * @param downloadUrl Download URL of the exported TMX file, set once an export completed.
   * @param expiresAt Timestamp after which the download URL is no longer valid.
   * @param errorMessage Error description, set when the job failed.
   * @param translationMemoryId ID of the translation memory created by a completed import.
   * @param skippedSegmentCount Number of segments an import skipped.
   */
  public TranslationMemoryJobResult(
      @Nullable Status status,
      @Nullable String requiredAction,
      @Nullable String downloadUrl,
      @Nullable Date expiresAt,
      @Nullable String errorMessage,
      @Nullable String translationMemoryId,
      @Nullable Integer skippedSegmentCount) {
    this(
        status,
        statusString(status),
        requiredAction,
        downloadUrl,
        expiresAt,
        errorMessage,
        translationMemoryId,
        skippedSegmentCount);
  }

  /**
   * Initializes a new {@link TranslationMemoryJobResult} describing the outcome of a job, keeping
   * the status as the API sent it.
   *
   * @param status Status of the job, or <code>null</code> if the API sent a status not in {@link
   *     Status}.
   * @param statusString Status of the job as the API sent it.
   * @param requiredAction Action the caller must take, set while the job is waiting on the caller.
   * @param downloadUrl Download URL of the exported TMX file, set once an export completed.
   * @param expiresAt Timestamp after which the download URL is no longer valid.
   * @param errorMessage Error description, set when the job failed.
   * @param translationMemoryId ID of the translation memory created by a completed import.
   * @param skippedSegmentCount Number of segments an import skipped.
   */
  public TranslationMemoryJobResult(
      @Nullable Status status,
      @Nullable String statusString,
      @Nullable String requiredAction,
      @Nullable String downloadUrl,
      @Nullable Date expiresAt,
      @Nullable String errorMessage,
      @Nullable String translationMemoryId,
      @Nullable Integer skippedSegmentCount) {
    this.status = status;
    this.statusString = statusString;
    this.requiredAction = requiredAction;
    this.downloadUrl = downloadUrl;
    this.expiresAt = expiresAt;
    this.errorMessage = errorMessage;
    this.translationMemoryId = translationMemoryId;
    this.skippedSegmentCount = skippedSegmentCount;
  }

  /**
   * @return Status of the job, or <code>null</code> if not provided by the API or not in {@link
   *     Status}; {@link #getStatusString()} returns it as sent.
   */
  public @Nullable Status getStatus() {
    return status;
  }

  /**
   * @return Status of the job as the API sent it, for example <code>"processing"</code>, including
   *     values not in {@link Status}; <code>null</code> if the response had no status.
   */
  public @Nullable String getStatusString() {
    return statusString;
  }

  private static @Nullable String statusString(@Nullable Status status) {
    if (status == null) {
      return null;
    }
    try {
      return Status.class.getField(status.name()).getAnnotation(SerializedName.class).value();
    } catch (NoSuchFieldException exception) {
      return null;
    }
  }

  /**
   * @return <code>true</code> if the job has finished, successfully or not, otherwise <code>false
   *     </code>.
   */
  public boolean done() {
    return status == Status.Completed
        || status == Status.Downloaded
        || status == Status.Failed
        || status == Status.Expired;
  }

  /** @return <code>false</code> if the job failed or expired, otherwise <code>true</code>. */
  public boolean ok() {
    return status != Status.Failed && status != Status.Expired;
  }

  /**
   * @return Action the caller must take, or <code>null</code> if the job is not waiting on the
   *     caller.
   */
  public @Nullable String getRequiredAction() {
    return requiredAction;
  }

  /**
   * @return Download URL of the exported TMX file, or <code>null</code> if the export has not
   *     completed.
   */
  public @Nullable String getDownloadUrl() {
    return downloadUrl;
  }

  /**
   * @return Timestamp after which the download URL is no longer valid, or <code>null</code> if not
   *     provided by the API.
   */
  public @Nullable Date getExpiresAt() {
    return expiresAt;
  }

  /** @return Error description if the job failed, otherwise <code>null</code>. */
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  /**
   * @return ID of the translation memory created by a completed import, otherwise <code>null</code>
   *     .
   */
  public @Nullable String getTranslationMemoryId() {
    return translationMemoryId;
  }

  /**
   * @return Number of segments an import skipped, or <code>null</code> if not provided by the API.
   */
  public @Nullable Integer getSkippedSegmentCount() {
    return skippedSegmentCount;
  }
}
