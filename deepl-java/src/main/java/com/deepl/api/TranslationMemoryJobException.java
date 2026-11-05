// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when an error occurs in {@link DeepLClient#importTranslationMemoryFromFilepath}
 * or {@link DeepLClient#exportTranslationMemoryToFilepath}. If the error occurs after the import or
 * export job was created, the ID of that job is included, to allow the job to be inspected or
 * waited for later. The underlying error is the cause.
 */
public class TranslationMemoryJobException extends DeepLException {

  private final @Nullable String jobId;
  private final @Nullable TranslationMemoryJob job;

  public TranslationMemoryJobException(
      String message,
      @Nullable Throwable cause,
      @Nullable String jobId,
      @Nullable TranslationMemoryJob job) {
    super(message, cause);
    this.jobId = jobId;
    this.job = job;
  }

  /**
   * Get the ID of the import or export job, or <code>null</code> if the error occurred before the
   * job was created.
   */
  public @Nullable String getJobId() {
    return jobId;
  }

  /**
   * Get the last fetched status of the import or export job, for example a failed or expired job,
   * or <code>null</code> if the error occurred before the job status was first fetched.
   */
  public @Nullable TranslationMemoryJob getJob() {
    return job;
  }

  /** Returns the HTTP status code of the underlying error, if it came from an HTTP response. */
  @Override
  public @Nullable Integer getHttpStatusCode() {
    Throwable cause = getCause();
    return cause instanceof DeepLException
        ? ((DeepLException) cause).getHttpStatusCode()
        : super.getHttpStatusCode();
  }

  /** Returns whether a retry of the underlying failed request could succeed. */
  @Override
  public boolean getShouldRetry() {
    Throwable cause = getCause();
    return cause instanceof DeepLException
        ? ((DeepLException) cause).getShouldRetry()
        : super.getShouldRetry();
  }
}
