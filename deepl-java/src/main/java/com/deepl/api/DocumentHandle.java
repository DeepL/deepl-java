// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.google.gson.annotations.SerializedName;

/**
 * Handle to an in-progress document translation.
 *
 * @see Translator#translateDocumentGetStatus(DocumentHandle)
 */
public class DocumentHandle {
  @SerializedName(value = "document_id")
  private final String documentId;

  @SerializedName(value = "document_key")
  private final String documentKey;

  public DocumentHandle(String documentId, String documentKey) {
    this.documentId = documentId;
    this.documentKey = documentKey;
  }

  /** Get the ID of associated document request. */
  public String getDocumentId() {
    return documentId;
  }

  /** Get the key of associated document request. */
  public String getDocumentKey() {
    return documentKey;
  }
}
