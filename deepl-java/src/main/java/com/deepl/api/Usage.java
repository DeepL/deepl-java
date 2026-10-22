// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * Information about DeepL account usage for the current billing period, for example the number of
 * characters translated.
 *
 * <p>Depending on the account type, some usage types will be omitted. See the <a
 * href="https://www.deepl.com/docs-api/">API documentation</a> for more information.
 */
public class Usage {
  private final @Nullable Detail character;
  private final @Nullable Detail document;
  private final @Nullable Detail teamDocument;
  private final @Nullable Detail apiKeyCharacter;
  private final @Nullable Detail speechToTextMinutes;
  private final @Nullable Detail speechToSpeechMinutes;
  private final @Nullable List<Product> products;
  private final @Nullable Date startTime;
  private final @Nullable Date endTime;

  /** The character usage if included for the account type, or <code>null</code>. */
  public @Nullable Detail getCharacter() {
    return character;
  }

  /** The document usage if included for the account type, or <code>null</code>. */
  public @Nullable Detail getDocument() {
    return document;
  }

  /** The team document usage if included for the account type, or <code>null</code>. */
  public @Nullable Detail getTeamDocument() {
    return teamDocument;
  }

  /**
   * The character usage of the API key used for this request, as opposed to the account-wide usage
   * in {@link #getCharacter()}, or <code>null</code> if not included (only API Pro accounts).
   */
  public @Nullable Detail getApiKeyCharacter() {
    return apiKeyCharacter;
  }

  /**
   * The speech-to-text usage in minutes if included for the account type (only API Pro accounts),
   * or <code>null</code>.
   */
  public @Nullable Detail getSpeechToTextMinutes() {
    return speechToTextMinutes;
  }

  /**
   * The speech-to-speech usage in minutes if included for the account type (only API Pro accounts),
   * or <code>null</code>.
   */
  public @Nullable Detail getSpeechToSpeechMinutes() {
    return speechToSpeechMinutes;
  }

  /**
   * The usage per product if included for the account type (only API Pro accounts), or <code>null
   * </code>.
   */
  public @Nullable List<Product> getProducts() {
    return products;
  }

  /**
   * The start of the current billing period if included for the account type (only API Pro
   * accounts), or <code>null</code>.
   */
  public @Nullable Date getStartTime() {
    return startTime;
  }

  /**
   * The end of the current billing period if included for the account type (only API Pro accounts),
   * or <code>null</code>.
   */
  public @Nullable Date getEndTime() {
    return endTime;
  }

  /** Stores the amount used and maximum amount for one usage type. */
  public static class Detail {
    private final long count;
    private final long limit;

    public Detail(long count, long limit) {
      this.count = count;
      this.limit = limit;
    }

    /** @return The currently used number of items for this usage type. */
    public long getCount() {
      return count;
    }

    /** @return The maximum permitted number of items for this usage type. */
    public long getLimit() {
      return limit;
    }

    /**
     * @return <code>true</code> if the amount used meets or exceeds the limit, otherwise <code>
     *     false</code>.
     */
    public boolean limitReached() {
      return getCount() >= getLimit();
    }

    @Override
    public String toString() {
      return getCount() + " of " + getLimit();
    }
  }

  /** Usage of one product, for example <code>translate</code> or <code>write</code>. */
  public static class Product {
    private final @Nullable String productType;
    private final @Nullable String billingUnit;
    private final long apiKeyUnitCount;
    private final long accountUnitCount;

    public Product(
        @Nullable String productType,
        @Nullable String billingUnit,
        long apiKeyUnitCount,
        long accountUnitCount) {
      this.productType = productType;
      this.billingUnit = billingUnit;
      this.apiKeyUnitCount = apiKeyUnitCount;
      this.accountUnitCount = accountUnitCount;
    }

    /**
     * @return The product type, for example <code>"translate"</code>, <code>"write"</code>, <code>
     *     "speechToText"</code> or <code>"speechToSpeech"</code>. New values may be added.
     */
    public @Nullable String getProductType() {
      return productType;
    }

    /**
     * @return The unit this product is billed in, for example <code>"characters"</code> or <code>
     *     "minutes"</code>.
     */
    public @Nullable String getBillingUnit() {
      return billingUnit;
    }

    /** @return Units used for this product by this API key in the current billing period. */
    public long getApiKeyUnitCount() {
      return apiKeyUnitCount;
    }

    /** @return Units used for this product by the whole account in the current billing period. */
    public long getAccountUnitCount() {
      return accountUnitCount;
    }

    @Override
    public String toString() {
      return getProductType()
          + ": "
          + getApiKeyUnitCount()
          + " (API key) / "
          + getAccountUnitCount()
          + " (account) "
          + getBillingUnit();
    }
  }

  public Usage(
      @Nullable Detail character, @Nullable Detail document, @Nullable Detail teamDocument) {
    this(character, document, teamDocument, null, null, null, null, null, null);
  }

  public Usage(
      @Nullable Detail character,
      @Nullable Detail document,
      @Nullable Detail teamDocument,
      @Nullable Detail apiKeyCharacter,
      @Nullable Detail speechToTextMinutes,
      @Nullable Detail speechToSpeechMinutes,
      @Nullable List<Product> products,
      @Nullable Date startTime,
      @Nullable Date endTime) {
    this.character = character;
    this.document = document;
    this.teamDocument = teamDocument;
    this.apiKeyCharacter = apiKeyCharacter;
    this.speechToTextMinutes = speechToTextMinutes;
    this.speechToSpeechMinutes = speechToSpeechMinutes;
    this.products = products;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /**
   * @return <code>true</code> if any of the usage types included for the account type have been
   *     reached, otherwise <code>false</code>.
   */
  public boolean anyLimitReached() {
    return (getCharacter() != null && getCharacter().limitReached())
        || (getDocument() != null && getDocument().limitReached())
        || (getTeamDocument() != null && getTeamDocument().limitReached());
  }

  /**
   * Returns a string representing the usage. This function is for diagnostic purposes only; the
   * content of the returned string is exempt from backwards compatibility.
   *
   * @return A string containing the usage for this billing period.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Usage this billing period:");

    BiConsumer<String, Detail> addLabelledDetail =
        (label, detail) -> {
          if (detail != null) {
            sb.append("\n").append(label).append(": ").append(detail);
          }
        };

    addLabelledDetail.accept("Characters", getCharacter());
    addLabelledDetail.accept("Documents", getDocument());
    addLabelledDetail.accept("Team documents", getTeamDocument());
    return sb.toString();
  }
}
