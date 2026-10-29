// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.util.Map;

/**
 * Options to control text rephrasing behaviour. These options may be provided to {@link
 * DeepLClient#rephraseText} overloads.
 *
 * <p>All properties have corresponding setters in fluent-style, so the following is possible:
 * <code>
 *      TextRephraseOptions options = new TextRephraseOptions()
 *          .setWritingStyle(WritingStyle.Business);
 * </code>
 */
public class TextRephraseOptions extends BaseRequestOptions {
  private String writingStyle;
  private String tone;

  /**
   * Sets a style the improved text should be in. Note that only style OR tone can be set.
   *
   * @see WritingStyle
   */
  public TextRephraseOptions setWritingStyle(String style) {
    this.writingStyle = style;
    return this;
  }

  /**
   * Sets a style the improved text should be in. Note that only style OR tone can be set.
   *
   * @param style The writing style, sent as its {@link WritingStyle#getValue()}.
   */
  public TextRephraseOptions setWritingStyle(WritingStyle style) {
    this.writingStyle = style != null ? style.getValue() : null;
    return this;
  }

  /**
   * Sets a tone the improved text should be in. Note that only style OR tone can be set.
   *
   * @see WritingTone
   */
  public TextRephraseOptions setTone(String tone) {
    this.tone = tone;
    return this;
  }

  /**
   * Sets a tone the improved text should be in. Note that only style OR tone can be set.
   *
   * @param tone The writing tone, sent as its {@link WritingTone#getValue()}.
   */
  public TextRephraseOptions setTone(WritingTone tone) {
    this.tone = tone != null ? tone.getValue() : null;
    return this;
  }

  /**
   * Sets additional parameters to pass in the body of the HTTP request. They are added last and
   * replace built-in parameters with the same name.
   *
   * @param extraBodyParameters Map of additional parameters to include in the request.
   * @return This options object for method chaining.
   */
  @Override
  public TextRephraseOptions setExtraBodyParameters(Map<String, String> extraBodyParameters) {
    super.setExtraBodyParameters(extraBodyParameters);
    return this;
  }

  /** Gets the current style setting. */
  public String getWritingStyle() {
    return writingStyle;
  }

  /** Gets the current tone setting. */
  public String getTone() {
    return tone;
  }
}
